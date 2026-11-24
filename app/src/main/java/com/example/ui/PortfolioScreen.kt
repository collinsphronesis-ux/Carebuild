package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.data.model.PledgeStatus
import com.example.data.model.SupportType
import com.example.data.model.UserPledge
import com.example.data.model.formatCentsToDollars

@Composable
fun PortfolioScreen(
    pledgesState: UiState<List<UserPledge>>,
    isMutating: Boolean,
    onRecordRepaymentOrFulfill: (UserPledge) -> Unit,
    onAskOreSummarizePortfolio: () -> Unit,
    onBrowseCases: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterType by remember { mutableStateOf<SupportType?>(null) }

    when (pledgesState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pledgesState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is UiState.Success -> {
            val allPledges = pledgesState.data
            val totalSponsoredCents = allPledges.filter { it.supportTypeEnum == SupportType.SPONSORSHIP }
                .sumOf { it.amountCents }
            val totalLoanedCents = allPledges.filter { it.supportTypeEnum == SupportType.MICROLOAN }
                .sumOf { it.amountCents }
            val totalRepaidCents = allPledges.filter { it.supportTypeEnum == SupportType.MICROLOAN }
                .sumOf { it.repaidCents }
            val uniqueCases = allPledges.map { it.caseId }.distinct().size

            val displayed = if (filterType == null) allPledges else {
                allPledges.filter { it.supportTypeEnum == filterType }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = modifier.fillMaxSize()
            ) {
                item(key = "portfolio_summary") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Your Stewardship Impact",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ImpactStatColumn(
                                    label = "Sponsored",
                                    value = formatCentsToDollars(totalSponsoredCents)
                                )
                                ImpactStatColumn(
                                    label = "0% Loans",
                                    value = formatCentsToDollars(totalLoanedCents)
                                )
                                ImpactStatColumn(
                                    label = "Repaid",
                                    value = formatCentsToDollars(totalRepaidCents)
                                )
                                ImpactStatColumn(
                                    label = "Lives Backed",
                                    value = "$uniqueCases"
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAskOreSummarizePortfolio,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("portfolio_ask_ore_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ask Ore to Summarize My Impact", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                item(key = "portfolio_filters") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filterType == null,
                            onClick = { filterType = null },
                            label = { Text("All (${allPledges.size})") },
                            modifier = Modifier.height(48.dp)
                        )
                        FilterChip(
                            selected = filterType == SupportType.SPONSORSHIP,
                            onClick = { filterType = SupportType.SPONSORSHIP },
                            label = { Text("Sponsorships") },
                            modifier = Modifier.height(48.dp)
                        )
                        FilterChip(
                            selected = filterType == SupportType.MICROLOAN,
                            onClick = { filterType = SupportType.MICROLOAN },
                            label = { Text("0% Microloans") },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }

                if (displayed.isEmpty()) {
                    item(key = "portfolio_empty") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolunteerActivism,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No contributions yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Explore medical and livelihood cases to make your first sponsorship or 0% microloan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onBrowseCases,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Browse Community Cases")
                                }
                            }
                        }
                    }
                } else {
                    items(displayed, key = { it.id }) { pledge ->
                        PledgeLedgerCard(
                            pledge = pledge,
                            isMutating = isMutating,
                            onAction = { onRecordRepaymentOrFulfill(pledge) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpactStatColumn(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun PledgeLedgerCard(
    pledge: UserPledge,
    isMutating: Boolean,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pledge_card_${pledge.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (pledge.supportTypeEnum == SupportType.MICROLOAN) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = pledge.supportTypeEnum.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pledge.supportTypeEnum == SupportType.MICROLOAN) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (pledge.statusEnum == PledgeStatus.ACTIVE) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = pledge.statusEnum.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pledge.statusEnum == PledgeStatus.ACTIVE) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = pledge.caseTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Beneficiary: ${pledge.beneficiaryName} • ${formatCentsToDollars(pledge.amountCents)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!pledge.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "“${pledge.note}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pledge.supportTypeEnum == SupportType.MICROLOAN) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pledge.repaymentFraction.coerceAtLeast(if (pledge.repaidCents > 0) 0.03f else 0f))
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${formatCentsToDollars(pledge.repaidCents)} repaid of ${formatCentsToDollars(pledge.amountCents)} (${pledge.repaymentMonths} mo term)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pledge.statusEnum == PledgeStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAction,
                    enabled = !isMutating,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("pledge_action_${pledge.id}")
                ) {
                    Icon(
                        imageVector = if (pledge.supportTypeEnum == SupportType.MICROLOAN) {
                            Icons.Filled.Handshake
                        } else {
                            Icons.Filled.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pledge.supportTypeEnum == SupportType.MICROLOAN) {
                            "Record Repayment Installment"
                        } else {
                            "Mark Impact Fulfilled"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
