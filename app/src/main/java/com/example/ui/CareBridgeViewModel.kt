package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.data.model.BeneficiaryCase
import com.example.data.model.CaseCategory
import com.example.data.model.CoverAssetKey
import com.example.data.model.ImpactUpdate
import com.example.data.model.MilestoneType
import com.example.data.model.OreNote
import com.example.data.model.OreScopeType
import com.example.data.model.SupportType
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserPledge
import com.example.data.repository.CareBridgeRepository
import com.example.data.repository.OreAssistantRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface OreResponseState {
    data object Idle : OreResponseState
    data class Loading(val topic: String, val scopeType: OreScopeType) : OreResponseState
    data class Streaming(
        val topic: String,
        val prompt: String,
        val text: String,
        val scopeType: OreScopeType
    ) : OreResponseState
    data class Success(
        val topic: String,
        val prompt: String,
        val text: String,
        val scopeType: OreScopeType,
        val saved: Boolean = false
    ) : OreResponseState
    data class Error(val message: String) : OreResponseState
}

data class DiscoverFilterState(
    val searchQuery: String = "",
    val selectedCategory: CaseCategory? = null,
    val onlyMicroloans: Boolean = false,
    val onlyUrgent: Boolean = false
)

class CareBridgeViewModel(
    private val repository: CareBridgeRepository,
    private val oreRepository: OreAssistantRepository,
    val currentUserId: String
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _filterState = MutableStateFlow(DiscoverFilterState())
    val filterState: StateFlow<DiscoverFilterState> = _filterState.asStateFlow()

    private val _selectedCaseId = MutableStateFlow<String?>(null)
    val selectedCaseId: StateFlow<String?> = _selectedCaseId.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _oreState = MutableStateFlow<OreResponseState>(OreResponseState.Idle)
    val oreState: StateFlow<OreResponseState> = _oreState.asStateFlow()

    private val _caseOreState = MutableStateFlow<OreResponseState>(OreResponseState.Idle)
    val caseOreState: StateFlow<OreResponseState> = _caseOreState.asStateFlow()

    private var oreStreamJob: Job? = null
    private var caseOreStreamJob: Job? = null

    val casesState: StateFlow<UiState<List<BeneficiaryCase>>> = repository.observePublicCases()
        .map<List<BeneficiaryCase>, UiState<List<BeneficiaryCase>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing beneficiary cases", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load community cases."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredCasesState: StateFlow<UiState<List<BeneficiaryCase>>> = combine(
        casesState,
        _filterState
    ) { casesUi, filters ->
        when (casesUi) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> casesUi
            is UiState.Success -> {
                val q = filters.searchQuery.trim().lowercase()
                val filtered = casesUi.data.filter { item ->
                    val matchesCategory = filters.selectedCategory == null || item.categoryEnum == filters.selectedCategory
                    val matchesLoan = !filters.onlyMicroloans ||
                        item.supportTypeEnum == SupportType.MICROLOAN ||
                        item.supportTypeEnum == SupportType.BOTH
                    val matchesUrgent = !filters.onlyUrgent || item.urgencyEnum == UrgencyLevel.URGENT
                    val matchesQuery = q.isEmpty() ||
                        item.title.lowercase().contains(q) ||
                        item.beneficiaryName.lowercase().contains(q) ||
                        item.region.lowercase().contains(q) ||
                        item.story.lowercase().contains(q)
                    matchesCategory && matchesLoan && matchesUrgent && matchesQuery
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedCaseUpdatesState: StateFlow<UiState<List<ImpactUpdate>>> = _selectedCaseId
        .flatMapLatest { caseId ->
            if (caseId.isNullOrBlank()) {
                flowOf<UiState<List<ImpactUpdate>>>(UiState.Success(emptyList()))
            } else {
                repository.observeCaseUpdates(caseId)
                    .map<List<ImpactUpdate>, UiState<List<ImpactUpdate>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing updates for $caseId", rootError)
                        emit(UiState.Error(rootError.message ?: "Unable to load impact updates."))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val pledgesState: StateFlow<UiState<List<UserPledge>>> = repository.observeUserPledges(currentUserId)
        .map<List<UserPledge>, UiState<List<UserPledge>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user pledges", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load your impact portfolio."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val oreNotesState: StateFlow<UiState<List<OreNote>>> = repository.observeOreNotes(currentUserId)
        .map<List<OreNote>, UiState<List<OreNote>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Ore notes", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load saved Ore notes."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.seedStarterCasesIfEmpty()
                .onFailure { e -> Log.w(TAG, "Starter case seeding skipped/failed: ${e.message}") }
        }
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun toggleCategory(category: CaseCategory?) {
        val current = _filterState.value.selectedCategory
        _filterState.value = _filterState.value.copy(
            selectedCategory = if (current == category) null else category
        )
    }

    fun toggleOnlyMicroloans() {
        _filterState.value = _filterState.value.copy(onlyMicroloans = !_filterState.value.onlyMicroloans)
    }

    fun toggleOnlyUrgent() {
        _filterState.value = _filterState.value.copy(onlyUrgent = !_filterState.value.onlyUrgent)
    }

    fun clearFilters() {
        _filterState.value = DiscoverFilterState()
    }

    fun selectCase(caseId: String?) {
        if (_selectedCaseId.value != caseId) {
            _caseOreState.value = OreResponseState.Idle
            caseOreStreamJob?.cancel()
        }
        _selectedCaseId.value = caseId
    }

    fun submitPledge(
        beneficiaryCase: BeneficiaryCase,
        supportType: SupportType,
        amountCents: Long,
        repaymentMonths: Long,
        note: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.submitPledge(
                beneficiaryCase = beneficiaryCase,
                supportType = supportType,
                amountCents = amountCents,
                repaymentMonths = repaymentMonths,
                note = note
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit(
                        "Thank you! Your ${supportType.label.lowercase()} for ${beneficiaryCase.beneficiaryName} is recorded."
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _snackbarEvents.tryEmit(error.localizedMessage ?: "Could not complete pledge.")
                }
            )
        }
    }

    fun createCase(
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
        coverAssetKey: CoverAssetKey,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createCase(
                beneficiaryName = beneficiaryName,
                region = region,
                title = title,
                story = story,
                allocationPlan = allocationPlan,
                category = category,
                supportType = supportType,
                goalAmountCents = goalAmountCents,
                repaymentMonths = repaymentMonths,
                urgency = urgency,
                coverAssetKey = coverAssetKey
            )
            _isMutating.value = false
            result.fold(
                onSuccess = { caseId ->
                    _snackbarEvents.tryEmit("Case published to CareBridge community.")
                    onCreated(caseId)
                },
                onFailure = { error ->
                    _snackbarEvents.tryEmit(error.localizedMessage ?: "Failed to publish case.")
                }
            )
        }
    }

    fun postImpactUpdate(
        caseId: String,
        headline: String,
        message: String,
        milestoneType: MilestoneType,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.postImpactUpdate(
                caseId = caseId,
                headline = headline,
                message = message,
                milestoneType = milestoneType
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit("Impact update posted.")
                    onSuccess()
                },
                onFailure = { error ->
                    _snackbarEvents.tryEmit(error.localizedMessage ?: "Failed to post update.")
                }
            )
        }
    }

    fun recordRepaymentOrFulfill(pledge: UserPledge) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.recordRepaymentOrFulfill(pledge)
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    val msg = if (pledge.supportTypeEnum == SupportType.MICROLOAN) {
                        "Repayment installment recorded for ${pledge.beneficiaryName}."
                    } else {
                        "Sponsorship marked as fulfilled."
                    }
                    _snackbarEvents.tryEmit(msg)
                },
                onFailure = { error ->
                    _snackbarEvents.tryEmit(error.localizedMessage ?: "Failed to update pledge status.")
                }
            )
        }
    }

    fun askOreEverythingDigest() {
        val cases = (casesState.value as? UiState.Success)?.data.orEmpty()
        val pledges = (pledgesState.value as? UiState.Success)?.data.orEmpty()
        val contextSnapshot = oreRepository.buildContextSnapshot(cases, pledges)
        val prompt = """
            Give me a complete, warm executive summary of everything happening across my CareBridge workspace right now:
            1) Urgent medical & poverty cases needing immediate support,
            2) Best 0% microloan opportunities, and
            3) My personal sponsorship & microloan portfolio status.
            Recommend 1-2 thoughtful next actions.

            $contextSnapshot
        """.trimIndent()
        runOreStream(
            topic = "Everything Digest",
            displayPrompt = "Summarize everything across community needs and my portfolio",
            fullModelPrompt = prompt,
            scopeType = OreScopeType.DISCOVER
        )
    }

    fun askOrePortfolioDigest() {
        val cases = (casesState.value as? UiState.Success)?.data.orEmpty()
        val pledges = (pledgesState.value as? UiState.Success)?.data.orEmpty()
        val contextSnapshot = oreRepository.buildContextSnapshot(cases, pledges)
        val prompt = """
            Summarize my personal CareBridge impact portfolio: total sponsorships vs. 0% microloans,
            repayment progress, lives impacted, and any active cases I should check in on.

            $contextSnapshot
        """.trimIndent()
        runOreStream(
            topic = "Impact Portfolio Summary",
            displayPrompt = "Summarize my sponsorships, microloans & repayments",
            fullModelPrompt = prompt,
            scopeType = OreScopeType.PORTFOLIO
        )
    }

    fun askOreUrgentMedicalDigest() {
        val cases = (casesState.value as? UiState.Success)?.data.orEmpty()
        val pledges = (pledgesState.value as? UiState.Success)?.data.orEmpty()
        val contextSnapshot = oreRepository.buildContextSnapshot(cases, pledges)
        val prompt = """
            Focus on urgent medical and basic-needs cases in CareBridge. Summarize who needs help most urgently,
            exact funding gaps remaining, and how funds are allocated.

            $contextSnapshot
        """.trimIndent()
        runOreStream(
            topic = "Urgent Medical Brief",
            displayPrompt = "Summarize urgent medical & basic-needs cases",
            fullModelPrompt = prompt,
            scopeType = OreScopeType.DISCOVER
        )
    }

    fun askOreCustomInput(rawUserNoteOrQuestion: String) {
        val trimmed = rawUserNoteOrQuestion.trim()
        if (trimmed.isEmpty()) {
            _oreState.value = OreResponseState.Error("Please share notes, a story, or a question for Ore to summarize.")
            return
        }
        val cases = (casesState.value as? UiState.Success)?.data.orEmpty()
        val pledges = (pledgesState.value as? UiState.Success)?.data.orEmpty()
        val contextSnapshot = oreRepository.buildContextSnapshot(cases, pledges)
        val prompt = """
            The user shared the following note, text, or request for you to take down and summarize clearly.
            Use the CareBridge context below if relevant, and distill everything into a clear, structured summary:

            USER INPUT:
            $trimmed

            CAREBRIDGE CONTEXT:
            $contextSnapshot
        """.trimIndent()
        val shortTopic = trimmed.lineSequence().firstOrNull()?.take(48)?.ifBlank { "Personal Note Summary" }
            ?: "Personal Note Summary"
        runOreStream(
            topic = shortTopic,
            displayPrompt = trimmed.take(480),
            fullModelPrompt = prompt,
            scopeType = OreScopeType.CUSTOM
        )
    }

    fun askOreForCase(beneficiaryCase: BeneficiaryCase, updates: List<ImpactUpdate>) {
        val fullPrompt = oreRepository.buildSingleCasePrompt(beneficiaryCase, updates)
        caseOreStreamJob?.cancel()
        caseOreStreamJob = viewModelScope.launch {
            val topic = "${beneficiaryCase.beneficiaryName}: ${beneficiaryCase.title}".take(96)
            val displayPrompt = "Summarize ${beneficiaryCase.beneficiaryName}'s case and updates"
            _caseOreState.value = OreResponseState.Loading(topic, OreScopeType.CASE)
            val buffer = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    oreRepository.streamOreSummary(fullPrompt).collect { chunk ->
                        buffer.append(chunk)
                        _caseOreState.value = OreResponseState.Streaming(
                            topic = topic,
                            prompt = displayPrompt,
                            text = buffer.toString(),
                            scopeType = OreScopeType.CASE
                        )
                    }
                }
                val finalText = buffer.toString().trim()
                if (finalText.isNotEmpty()) {
                    _caseOreState.value = OreResponseState.Success(
                        topic = topic,
                        prompt = displayPrompt,
                        text = finalText,
                        scopeType = OreScopeType.CASE
                    )
                } else {
                    _caseOreState.value = OreResponseState.Error("Ore returned an empty summary. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Ore case summary timed out", e)
                _caseOreState.value = OreResponseState.Error("Ore timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ore case summary failed", e)
                _caseOreState.value = OreResponseState.Error(e.localizedMessage ?: "Ore could not summarize this case.")
            }
        }
    }

    private fun runOreStream(
        topic: String,
        displayPrompt: String,
        fullModelPrompt: String,
        scopeType: OreScopeType
    ) {
        oreStreamJob?.cancel()
        oreStreamJob = viewModelScope.launch {
            _oreState.value = OreResponseState.Loading(topic, scopeType)
            val buffer = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    oreRepository.streamOreSummary(fullModelPrompt).collect { chunk ->
                        buffer.append(chunk)
                        _oreState.value = OreResponseState.Streaming(
                            topic = topic,
                            prompt = displayPrompt,
                            text = buffer.toString(),
                            scopeType = scopeType
                        )
                    }
                }
                val finalText = buffer.toString().trim()
                if (finalText.isNotEmpty()) {
                    _oreState.value = OreResponseState.Success(
                        topic = topic,
                        prompt = displayPrompt,
                        text = finalText,
                        scopeType = scopeType
                    )
                } else {
                    _oreState.value = OreResponseState.Error("Ore returned an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Ore stream timed out", e)
                _oreState.value = OreResponseState.Error("Ore timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ore stream failed", e)
                _oreState.value = OreResponseState.Error(e.localizedMessage ?: "Ore failed to summarize.")
            }
        }
    }

    fun saveOreSummary(fromCaseView: Boolean) {
        val current = if (fromCaseView) _caseOreState.value else _oreState.value
        val success = current as? OreResponseState.Success ?: return
        if (success.saved) return

        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveOreNote(
                topic = success.topic,
                prompt = success.prompt,
                summary = success.text,
                scopeType = success.scopeType
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    val updated = success.copy(saved = true)
                    if (fromCaseView) _caseOreState.value = updated else _oreState.value = updated
                    _snackbarEvents.tryEmit("Saved to Ore's cloud notes.")
                },
                onFailure = { error ->
                    _snackbarEvents.tryEmit(error.localizedMessage ?: "Could not save Ore note.")
                }
            )
        }
    }

    fun deleteOreNote(noteId: String) {
        viewModelScope.launch {
            val result = repository.deleteOreNote(noteId)
            result.fold(
                onSuccess = { _snackbarEvents.tryEmit("Summary removed.") },
                onFailure = { error -> _snackbarEvents.tryEmit(error.localizedMessage ?: "Could not delete note.") }
            )
        }
    }

    private companion object {
        const val TAG = "CareBridgeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val AI_TIMEOUT_MS = 60_000L
    }
}
