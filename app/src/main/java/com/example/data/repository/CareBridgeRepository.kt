package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.BeneficiaryCase
import com.example.data.model.CaseCategory
import com.example.data.model.CaseStatus
import com.example.data.model.CoverAssetKey
import com.example.data.model.ImpactUpdate
import com.example.data.model.MilestoneType
import com.example.data.model.OreNote
import com.example.data.model.OreScopeType
import com.example.data.model.PledgeStatus
import com.example.data.model.SupportType
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserPledge
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CareBridgeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing CareBridge data." }
        return uid
    }

    fun observePublicCases(): Flow<List<BeneficiaryCase>> = flow {
        val uid = requireUserId()
        val path = "beneficiaryCases"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BeneficiaryCase::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<BeneficiaryCase> { it.urgency == UrgencyLevel.URGENT.raw }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCaseUpdates(caseId: String): Flow<List<ImpactUpdate>> = flow {
        val uid = requireUserId()
        require(caseId.isNotBlank()) { "Case ID must not be blank" }
        val path = "beneficiaryCases/$caseId/updates"
        emitAll(
            db.collection("beneficiaryCases").document(caseId)
                .collection("updates")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ImpactUpdate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserPledges(userId: String): Flow<List<UserPledge>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user pledge observation." }
        val path = "users/$userId/pledges"
        emitAll(
            db.collection("users").document(userId).collection("pledges")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserPledge::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOreNotes(userId: String): Flow<List<OreNote>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user Ore notes observation." }
        val path = "users/$userId/oreNotes"
        emitAll(
            db.collection("users").document(userId).collection("oreNotes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OreNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedStarterCasesIfEmpty(): Result<Unit> {
        val path = "beneficiaryCases"
        return try {
            val uid = requireUserId()
            val existing = db.collection(path)
                .whereEqualTo("visibility", "public")
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.success(Unit)
            }

            val batch = db.batch()
            val starterCases = listOf(
                Triple(
                    "starter_amina_medical",
                    mapOf(
                        "creatorId" to uid,
                        "beneficiaryName" to "Amina K.",
                        "region" to "Nakuru, Kenya",
                        "title" to "Post-Surgery Recovery & Pediatric Care",
                        "story" to "After an emergency gallbladder operation, Amina cannot return to her laundry work for six weeks. Your sponsorship covers follow-up clinic visits, pain relief medication, and nourishing meals for her two young children while she heals.",
                        "allocationPlan" to "65% clinic dressings & antibiotics, 35% family groceries during recovery",
                        "category" to CaseCategory.MEDICAL.raw,
                        "supportType" to SupportType.SPONSORSHIP.raw,
                        "goalAmountCents" to 42000L,
                        "raisedAmountCents" to 0L,
                        "supporterCount" to 0L,
                        "repaymentMonths" to 0L,
                        "urgency" to UrgencyLevel.URGENT.raw,
                        "status" to CaseStatus.ACTIVE.raw,
                        "visibility" to "public",
                        "coverAssetKey" to CoverAssetKey.MEDICAL.raw,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    mapOf(
                        "headline" to "Discharged home safely",
                        "message" to "Amina left the district clinic this morning and needs two follow-up wound checks over the next 21 days.",
                        "milestoneType" to MilestoneType.MEDICAL_UPDATE.raw
                    )
                ),
                Triple(
                    "starter_mateo_tailoring",
                    mapOf(
                        "creatorId" to uid,
                        "beneficiaryName" to "Mateo R.",
                        "region" to "Oaxaca, Mexico",
                        "title" to "Pedal Sewing Machine for Home Tailoring",
                        "story" to "Mateo lost his workshop tools in seasonal flooding. A zero-interest microloan helps him purchase a durable pedal sewing machine and linen fabric rolls so he can mend school uniforms and rebuild steady weekly income.",
                        "allocationPlan" to "75% refurbished sewing machine, 25% thread, shears & starter fabric",
                        "category" to CaseCategory.LIVELIHOOD.raw,
                        "supportType" to SupportType.MICROLOAN.raw,
                        "goalAmountCents" to 36000L,
                        "raisedAmountCents" to 0L,
                        "supporterCount" to 0L,
                        "repaymentMonths" to 9L,
                        "urgency" to UrgencyLevel.STANDARD.raw,
                        "status" to CaseStatus.ACTIVE.raw,
                        "visibility" to "public",
                        "coverAssetKey" to CoverAssetKey.LIVELIHOOD.raw,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    mapOf(
                        "headline" to "Local supplier quote confirmed",
                        "message" to "A local repair co-op has reserved a serviced pedal machine with a 1-year warranty.",
                        "milestoneType" to MilestoneType.PROGRESS.raw
                    )
                ),
                Triple(
                    "starter_nadia_insulin",
                    mapOf(
                        "creatorId" to uid,
                        "beneficiaryName" to "Nadia H.",
                        "region" to "Amman, Jordan",
                        "title" to "Monthly Insulin & Solar Cooler Pack",
                        "story" to "Frequent neighborhood power outages spoil Nadia's insulin supply. Combining direct medical sponsorship with a modest livelihood loan lets her store insulin safely and sell homemade spice blends to neighbors.",
                        "allocationPlan" to "55% 4-month insulin & test strips, 45% compact solar medicine cooler",
                        "category" to CaseCategory.MEDICAL.raw,
                        "supportType" to SupportType.BOTH.raw,
                        "goalAmountCents" to 28000L,
                        "raisedAmountCents" to 0L,
                        "supporterCount" to 0L,
                        "repaymentMonths" to 6L,
                        "urgency" to UrgencyLevel.URGENT.raw,
                        "status" to CaseStatus.ACTIVE.raw,
                        "visibility" to "public",
                        "coverAssetKey" to CoverAssetKey.MEDICAL.raw,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    null
                ),
                Triple(
                    "starter_priya_books",
                    mapOf(
                        "creatorId" to uid,
                        "beneficiaryName" to "Priya & Dev",
                        "region" to "Jaipur, India",
                        "title" to "Nursing Tuition & Study Bridge Fund",
                        "story" to "After her father's chronic illness halted household wages, Priya paused her final nursing semester. Support covers exam fees, clinical scrubs, and daily bus transit so she can graduate and support her family.",
                        "allocationPlan" to "70% semester exam & clinical fee, 30% textbooks & bus pass",
                        "category" to CaseCategory.EDUCATION.raw,
                        "supportType" to SupportType.BOTH.raw,
                        "goalAmountCents" to 50000L,
                        "raisedAmountCents" to 0L,
                        "supporterCount" to 0L,
                        "repaymentMonths" to 12L,
                        "urgency" to UrgencyLevel.STANDARD.raw,
                        "status" to CaseStatus.ACTIVE.raw,
                        "visibility" to "public",
                        "coverAssetKey" to CoverAssetKey.HERO.raw,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    null
                )
            )

            starterCases.forEach { (caseId, payload, initialUpdate) ->
                val caseRef = db.collection(path).document(caseId)
                batch.set(caseRef, payload)
                if (initialUpdate != null) {
                    val updRef = caseRef.collection("updates").document("${caseId}_init")
                    batch.set(
                        updRef,
                        mapOf(
                            "caseId" to caseId,
                            "authorId" to uid,
                            "headline" to initialUpdate.getValue("headline"),
                            "message" to initialUpdate.getValue("message"),
                            "milestoneType" to initialUpdate.getValue("milestoneType"),
                            "visibility" to "public",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitPledge(
        beneficiaryCase: BeneficiaryCase,
        supportType: SupportType,
        amountCents: Long,
        repaymentMonths: Long,
        note: String?
    ): Result<Unit> {
        var logPath = "beneficiaryCases/${beneficiaryCase.id}"
        return try {
            val uid = requireUserId()
            require(beneficiaryCase.id.isNotBlank()) { "Beneficiary case ID is required." }
            require(beneficiaryCase.status == CaseStatus.ACTIVE.raw) { "This case is already fully funded." }
            require(supportType == SupportType.SPONSORSHIP || supportType == SupportType.MICROLOAN) {
                "Choose Sponsorship or 0% Microloan."
            }
            require(amountCents in 500L..10_000_000L) {
                "Contribution must be between $5 and $100,000."
            }
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanNote != null) {
                require(cleanNote.length <= 300) { "Encouragement note must be 300 characters or fewer." }
            }
            val months = if (supportType == SupportType.MICROLOAN) repaymentMonths.coerceIn(3L, 36L) else 0L

            val caseRef = db.collection("beneficiaryCases").document(beneficiaryCase.id)
            val pledgeRef = db.collection("users").document(uid).collection("pledges").document()
            logPath = pledgeRef.path

            db.runTransaction { transaction ->
                val freshSnap = transaction.get(caseRef)
                check(freshSnap.exists()) { "Beneficiary case no longer exists." }
                val currentStatus = freshSnap.getString("status") ?: CaseStatus.ACTIVE.raw
                check(currentStatus == CaseStatus.ACTIVE.raw) { "This case has already reached its goal." }

                val currentRaised = freshSnap.getLong("raisedAmountCents") ?: 0L
                val currentGoal = freshSnap.getLong("goalAmountCents") ?: beneficiaryCase.goalAmountCents
                val currentSupporters = freshSnap.getLong("supporterCount") ?: 0L

                val newRaised = currentRaised + amountCents
                val newSupporters = currentSupporters + 1L
                val newStatus = if (newRaised >= currentGoal) CaseStatus.FUNDED.raw else CaseStatus.ACTIVE.raw

                val pledgePayload = mapOf(
                    "userId" to uid,
                    "caseId" to beneficiaryCase.id,
                    "caseTitle" to beneficiaryCase.title.take(100),
                    "beneficiaryName" to beneficiaryCase.beneficiaryName.take(60),
                    "supportType" to supportType.raw,
                    "amountCents" to amountCents,
                    "repaidCents" to 0L,
                    "repaymentMonths" to months,
                    "note" to cleanNote,
                    "status" to PledgeStatus.ACTIVE.raw,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                transaction.set(pledgeRef, pledgePayload)
                transaction.update(
                    caseRef,
                    mapOf(
                        "raisedAmountCents" to newRaised,
                        "supporterCount" to newSupporters,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, logPath)
            Result.failure(e)
        }
    }

    suspend fun createCase(
        beneficiaryName: String,
        region: String,
        title: String,
        story: String,
        allocationPlan: String,
        category: CaseCategory,
        supportType: SupportType,
        goalAmountCents: Long,
        repaymentMonths: Long,
        urgency: UrgencyLevel,
        coverAssetKey: CoverAssetKey
    ): Result<String> {
        var docPath = "beneficiaryCases"
        return try {
            val uid = requireUserId()
            val cleanName = beneficiaryName.trim()
            val cleanRegion = region.trim()
            val cleanTitle = title.trim()
            val cleanStory = story.trim()
            val cleanPlan = allocationPlan.trim()

            require(cleanName.length in 2..60) { "Beneficiary alias must be 2 to 60 characters." }
            require(cleanRegion.length in 2..80) { "Region must be 2 to 80 characters." }
            require(cleanTitle.length in 4..100) { "Title must be 4 to 100 characters." }
            require(cleanStory.length in 10..1500) { "Story must be 10 to 1,500 characters." }
            require(cleanPlan.length in 4..500) { "Allocation plan must be 4 to 500 characters." }
            require(goalAmountCents in 500L..50_000_000L) { "Goal must be between $5 and $500,000." }

            val months = if (supportType == SupportType.SPONSORSHIP) 0L else repaymentMonths.coerceIn(3L, 36L)
            val docRef = db.collection("beneficiaryCases").document()
            docPath = docRef.path

            val payload = mapOf(
                "creatorId" to uid,
                "beneficiaryName" to cleanName,
                "region" to cleanRegion,
                "title" to cleanTitle,
                "story" to cleanStory,
                "allocationPlan" to cleanPlan,
                "category" to category.raw,
                "supportType" to supportType.raw,
                "goalAmountCents" to goalAmountCents,
                "raisedAmountCents" to 0L,
                "supporterCount" to 0L,
                "repaymentMonths" to months,
                "urgency" to urgency.raw,
                "status" to CaseStatus.ACTIVE.raw,
                "visibility" to "public",
                "coverAssetKey" to coverAssetKey.raw,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun postImpactUpdate(
        caseId: String,
        headline: String,
        message: String,
        milestoneType: MilestoneType
    ): Result<Unit> {
        var docPath = "beneficiaryCases/$caseId/updates"
        return try {
            val uid = requireUserId()
            require(caseId.isNotBlank()) { "Case ID is required." }
            val cleanHeadline = headline.trim()
            val cleanMessage = message.trim()
            require(cleanHeadline.length in 3..100) { "Headline must be 3 to 100 characters." }
            require(cleanMessage.length in 5..1000) { "Message must be 5 to 1,000 characters." }

            val updateRef = db.collection("beneficiaryCases").document(caseId)
                .collection("updates").document()
            docPath = updateRef.path

            val payload = mapOf(
                "caseId" to caseId,
                "authorId" to uid,
                "headline" to cleanHeadline,
                "message" to cleanMessage,
                "milestoneType" to milestoneType.raw,
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp()
            )
            updateRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordRepaymentOrFulfill(pledge: UserPledge): Result<Unit> {
        var docPath = "users/${pledge.userId}/pledges/${pledge.id}"
        return try {
            val uid = requireUserId()
            check(uid == pledge.userId) { "Refusing cross-user pledge update." }
            require(pledge.id.isNotBlank()) { "Pledge ID must not be blank." }
            require(pledge.status == PledgeStatus.ACTIVE.raw) { "Pledge is already settled." }

            val docRef = db.collection("users").document(uid)
                .collection("pledges").document(pledge.id)
            docPath = docRef.path

            if (pledge.supportTypeEnum == SupportType.MICROLOAN) {
                val installment = (pledge.amountCents / pledge.repaymentMonths.coerceAtLeast(1L))
                    .coerceAtLeast(100L)
                val nextRepaid = (pledge.repaidCents + installment).coerceAtMost(pledge.amountCents)
                val nextStatus = if (nextRepaid >= pledge.amountCents) {
                    PledgeStatus.REPAID.raw
                } else {
                    PledgeStatus.ACTIVE.raw
                }
                docRef.update(
                    mapOf(
                        "repaidCents" to nextRepaid,
                        "status" to nextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "status" to PledgeStatus.FULFILLED.raw,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveOreNote(
        topic: String,
        prompt: String,
        summary: String,
        scopeType: OreScopeType
    ): Result<Unit> {
        var docPath = "users/unauthenticated/oreNotes"
        return try {
            val uid = requireUserId()
            val cleanTopic = topic.trim().take(100)
            val cleanPrompt = prompt.trim().take(500)
            val cleanSummary = summary.trim().take(4000)
            require(cleanTopic.length >= 2) { "Topic is too short." }
            require(cleanPrompt.length >= 2) { "Prompt is too short." }
            require(cleanSummary.length >= 5) { "Summary is too short." }

            val docRef = db.collection("users").document(uid)
                .collection("oreNotes").document()
            docPath = docRef.path

            docRef.set(
                mapOf(
                    "userId" to uid,
                    "topic" to cleanTopic,
                    "prompt" to cleanPrompt,
                    "summary" to cleanSummary,
                    "scopeType" to scopeType.raw,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteOreNote(noteId: String): Result<Unit> {
        var docPath = "users/unauthenticated/oreNotes/$noteId"
        return try {
            val uid = requireUserId()
            require(noteId.isNotBlank()) { "Note ID is required." }
            val docRef = db.collection("users").document(uid)
                .collection("oreNotes").document(noteId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
