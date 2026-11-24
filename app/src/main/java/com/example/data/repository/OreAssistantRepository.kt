package com.example.data.repository

import com.example.data.model.BeneficiaryCase
import com.example.data.model.ImpactUpdate
import com.example.data.model.UserPledge
import com.example.data.model.formatCentsToDollars
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OreAssistantRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are Ore, the calm, compassionate, and sharp personal helper inside CareBridge.
                    Your role is to take any information the user gives you—beneficiary stories, medical bills,
                    microloan portfolios, updates, or raw personal notes—and summarize everything clearly,
                    warmly, and accurately.

                    Formatting rules:
                    - Lead with a 1-2 sentence executive summary.
                    - Use concise plain-text bullet points ("• ...") for key facts, amounts, urgency, and next steps.
                    - Never invent numbers, medical diagnoses, or repayment facts not present in the provided context.
                    - Keep tone respectful, dignified, and actionable.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamOreSummary(userPrompt: String): Flow<String> {
        return model.generateContentStream(userPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }

    fun buildContextSnapshot(
        cases: List<BeneficiaryCase>,
        pledges: List<UserPledge>
    ): String {
        val caseLines = if (cases.isEmpty()) {
            "No active beneficiary cases loaded yet."
        } else {
            cases.joinToString("\n") { c ->
                "- [${c.urgencyEnum.label}] ${c.title} (${c.beneficiaryName}, ${c.region}) | " +
                    "Category: ${c.categoryEnum.label} | Type: ${c.supportTypeEnum.label} | " +
                    "Raised ${formatCentsToDollars(c.raisedAmountCents)} of ${formatCentsToDollars(c.goalAmountCents)} " +
                    "(${c.supporterCount} supporters, status: ${c.status}) | Allocation: ${c.allocationPlan}"
            }
        }

        val pledgeLines = if (pledges.isEmpty()) {
            "User has not made any sponsorships or microloans yet."
        } else {
            pledges.joinToString("\n") { p ->
                "- ${p.supportTypeEnum.label} of ${formatCentsToDollars(p.amountCents)} to ${p.beneficiaryName} " +
                    "(${p.caseTitle}) | Repaid: ${formatCentsToDollars(p.repaidCents)} | Status: ${p.status}"
            }
        }

        return """
            === CAREBRIDGE COMMUNITY CASES (${cases.size}) ===
            $caseLines

            === USER'S PERSONAL IMPACT PORTFOLIO (${pledges.size}) ===
            $pledgeLines
        """.trimIndent()
    }

    fun buildSingleCasePrompt(
        beneficiaryCase: BeneficiaryCase,
        updates: List<ImpactUpdate>
    ): String {
        val updatesText = if (updates.isEmpty()) {
            "No creator updates posted yet."
        } else {
            updates.joinToString("\n") { u ->
                "- [${u.milestoneTypeEnum.label}] ${u.headline}: ${u.message}"
            }
        }
        return """
            Summarize this CareBridge case clearly so a supporter can decide whether to sponsor or offer a 0% microloan.
            Include: (1) Core need & urgency, (2) Funding status & support option, (3) How funds are allocated, and (4) Latest updates.

            Case Title: ${beneficiaryCase.title}
            Beneficiary: ${beneficiaryCase.beneficiaryName} (${beneficiaryCase.region})
            Category: ${beneficiaryCase.categoryEnum.label}
            Support Model: ${beneficiaryCase.supportTypeEnum.label} (Term: ${beneficiaryCase.repaymentMonths} months if loan)
            Urgency: ${beneficiaryCase.urgencyEnum.label}
            Progress: ${formatCentsToDollars(beneficiaryCase.raisedAmountCents)} raised of ${formatCentsToDollars(beneficiaryCase.goalAmountCents)} (${beneficiaryCase.supporterCount} supporters)
            Story: ${beneficiaryCase.story}
            Allocation Plan: ${beneficiaryCase.allocationPlan}

            Impact Updates:
            $updatesText
        """.trimIndent()
    }
}
