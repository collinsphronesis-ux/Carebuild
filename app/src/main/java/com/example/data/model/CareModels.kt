package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class CaseCategory(val raw: String, val label: String) {
    MEDICAL("medical", "Medical Care"),
    ESSENTIALS("essentials", "Basic Needs"),
    EDUCATION("education", "Education"),
    LIVELIHOOD("livelihood", "Livelihood");

    companion object {
        fun fromRaw(raw: String): CaseCategory =
            entries.firstOrNull { it.raw == raw } ?: MEDICAL
    }
}

enum class SupportType(val raw: String, val label: String) {
    SPONSORSHIP("sponsorship", "Sponsorship"),
    MICROLOAN("microloan", "0% Microloan"),
    BOTH("both", "Sponsor or Lend");

    companion object {
        fun fromRaw(raw: String): SupportType =
            entries.firstOrNull { it.raw == raw } ?: BOTH
    }
}

enum class UrgencyLevel(val raw: String, val label: String) {
    STANDARD("standard", "Standard"),
    URGENT("urgent", "Urgent Need");

    companion object {
        fun fromRaw(raw: String): UrgencyLevel =
            entries.firstOrNull { it.raw == raw } ?: STANDARD
    }
}

enum class CaseStatus(val raw: String, val label: String) {
    ACTIVE("active", "Active"),
    FUNDED("funded", "Fully Funded");

    companion object {
        fun fromRaw(raw: String): CaseStatus =
            entries.firstOrNull { it.raw == raw } ?: ACTIVE
    }
}

enum class CoverAssetKey(val raw: String, val label: String) {
    HERO("hero", "Community Hope"),
    MEDICAL("medical", "Medical Care"),
    LIVELIHOOD("livelihood", "Livelihood Craft");

    companion object {
        fun fromRaw(raw: String): CoverAssetKey =
            entries.firstOrNull { it.raw == raw } ?: HERO
    }
}

enum class MilestoneType(val raw: String, val label: String) {
    PROGRESS("progress", "Progress"),
    MEDICAL_UPDATE("medical_update", "Medical Update"),
    REPAYMENT("repayment", "Repayment Milestone");

    companion object {
        fun fromRaw(raw: String): MilestoneType =
            entries.firstOrNull { it.raw == raw } ?: PROGRESS
    }
}

enum class PledgeStatus(val raw: String, val label: String) {
    ACTIVE("active", "Active"),
    REPAID("repaid", "Repaid"),
    FULFILLED("fulfilled", "Fulfilled");

    companion object {
        fun fromRaw(raw: String): PledgeStatus =
            entries.firstOrNull { it.raw == raw } ?: ACTIVE
    }
}

enum class OreScopeType(val raw: String, val label: String) {
    PORTFOLIO("portfolio", "Portfolio Digest"),
    CASE("case", "Case Brief"),
    DISCOVER("discover", "Community Overview"),
    CUSTOM("custom", "Personal Note");

    companion object {
        fun fromRaw(raw: String): OreScopeType =
            entries.firstOrNull { it.raw == raw } ?: CUSTOM
    }
}

@Keep
@IgnoreExtraProperties
data class BeneficiaryCase(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val beneficiaryName: String = "",
    val region: String = "",
    val title: String = "",
    val story: String = "",
    val allocationPlan: String = "",
    val category: String = CaseCategory.MEDICAL.raw,
    val supportType: String = SupportType.BOTH.raw,
    val goalAmountCents: Long = 0L,
    val raisedAmountCents: Long = 0L,
    val supporterCount: Long = 0L,
    val repaymentMonths: Long = 0L,
    val urgency: String = UrgencyLevel.STANDARD.raw,
    val status: String = CaseStatus.ACTIVE.raw,
    val visibility: String = "public",
    val coverAssetKey: String = CoverAssetKey.HERO.raw,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val categoryEnum: CaseCategory get() = CaseCategory.fromRaw(category)
    val supportTypeEnum: SupportType get() = SupportType.fromRaw(supportType)
    val urgencyEnum: UrgencyLevel get() = UrgencyLevel.fromRaw(urgency)
    val statusEnum: CaseStatus get() = CaseStatus.fromRaw(status)
    val coverAssetEnum: CoverAssetKey get() = CoverAssetKey.fromRaw(coverAssetKey)
    val progressFraction: Float
        get() = if (goalAmountCents <= 0L) 0f
        else (raisedAmountCents.toFloat() / goalAmountCents.toFloat()).coerceIn(0f, 1f)
    val remainingCents: Long get() = (goalAmountCents - raisedAmountCents).coerceAtLeast(0L)
}

@Keep
@IgnoreExtraProperties
data class ImpactUpdate(
    @DocumentId val id: String = "",
    val caseId: String = "",
    val authorId: String = "",
    val headline: String = "",
    val message: String = "",
    val milestoneType: String = MilestoneType.PROGRESS.raw,
    val visibility: String = "public",
    val createdAt: Timestamp? = null
) {
    val milestoneTypeEnum: MilestoneType get() = MilestoneType.fromRaw(milestoneType)
}

@Keep
@IgnoreExtraProperties
data class UserPledge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val caseId: String = "",
    val caseTitle: String = "",
    val beneficiaryName: String = "",
    val supportType: String = SupportType.SPONSORSHIP.raw,
    val amountCents: Long = 0L,
    val repaidCents: Long = 0L,
    val repaymentMonths: Long = 0L,
    val note: String? = null,
    val status: String = PledgeStatus.ACTIVE.raw,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val supportTypeEnum: SupportType get() = SupportType.fromRaw(supportType)
    val statusEnum: PledgeStatus get() = PledgeStatus.fromRaw(status)
    val repaymentFraction: Float
        get() = if (amountCents <= 0L) 0f
        else (repaidCents.toFloat() / amountCents.toFloat()).coerceIn(0f, 1f)
}

@Keep
@IgnoreExtraProperties
data class OreNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topic: String = "",
    val prompt: String = "",
    val summary: String = "",
    val scopeType: String = OreScopeType.CUSTOM.raw,
    val createdAt: Timestamp? = null
) {
    val scopeTypeEnum: OreScopeType get() = OreScopeType.fromRaw(scopeType)
}

fun formatCentsToDollars(cents: Long): String {
    val whole = cents / 100L
    val rem = (cents % 100L).toInt()
    return if (rem == 0) {
        "$$whole"
    } else {
        "$$whole.${rem.toString().padStart(2, '0')}"
    }
}
