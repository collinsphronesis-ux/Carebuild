package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BeneficiaryCase
import com.example.data.model.SupportType
import com.example.data.model.formatCentsToDollars

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PledgeBottomSheet(
    beneficiaryCase: BeneficiaryCase,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitPledge: (SupportType, Long, Long, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialType = when (beneficiaryCase.supportTypeEnum) {
        SupportType.MICROLOAN -> SupportType.MICROLOAN
        SupportType.SPONSORSHIP, SupportType.BOTH -> SupportType.SPONSORSHIP
    }
    var selectedType by remember(beneficiaryCase.id) { mutableStateOf(initialType) }
    var amountText by remember(beneficiaryCase.id) { mutableStateOf("25") }
    var noteText by remember(beneficiaryCase.id) { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val presets = listOf(15, 30, 75, 150)
    val loanMonths = beneficiaryCase.repaymentMonths.takeIf { it > 0L } ?: 6L
    val parsedDollars = amountText.toLongOrNull() ?: 0L
    val amountCents = parsedDollars * 100L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Support ${beneficiaryCase.beneficiaryName}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = beneficiaryCase.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Choose how to help",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val allowSponsor = beneficiaryCase.supportTypeEnum != SupportType.MICROLOAN
                val allowLoan = beneficiaryCase.supportTypeEnum != SupportType.SPONSORSHIP

                if (allowSponsor) {
                    FilterChip(
                        selected = selectedType == SupportType.SPONSORSHIP,
                        onClick = { selectedType = SupportType.SPONSORSHIP },
                        label = { Text("Direct Sponsorship") },
                        leadingIcon = {
                            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pledge_type_sponsor")
                    )
                }
                if (allowLoan) {
                    FilterChip(
                        selected = selectedType == SupportType.MICROLOAN,
                        onClick = { selectedType = SupportType.MICROLOAN },
                        label = { Text("0% Microloan") },
                        leadingIcon = {
                            Icon(Icons.Filled.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pledge_type_microloan")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Contribution amount (USD)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = parsedDollars == preset.toLong(),
                        onClick = {
                            amountText = preset.toString()
                            validationError = null
                        },
                        label = { Text("$$preset") },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() }.take(6)
                    validationError = null
                },
                label = { Text("Custom amount ($5 min)") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pledge_amount_input")
            )

            if (selectedType == SupportType.MICROLOAN && parsedDollars >= 5L) {
                Spacer(modifier = Modifier.height(12.dp))
                val monthlyEstimateCents = (amountCents / loanMonths).coerceAtLeast(100L)
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "0% Interest Repayment Plan",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Estimated ${formatCentsToDollars(monthlyEstimateCents)}/month over $loanMonths months. Track each milestone in your Impact portfolio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it.take(300)
                    validationError = null
                },
                label = { Text("Encouragement note (optional)") },
                placeholder = { Text("Sending strength and steady healing…") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pledge_note_input")
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (amountCents < 500L || amountCents > 10_000_000L) {
                        validationError = "Please enter an amount between $5 and $100,000."
                        return@Button
                    }
                    onSubmitPledge(
                        selectedType,
                        amountCents,
                        if (selectedType == SupportType.MICROLOAN) loanMonths else 0L,
                        noteText
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_pledge_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Recording pledge…")
                } else {
                    val labelVerb = if (selectedType == SupportType.MICROLOAN) "Lend" else "Sponsor"
                    val displayAmt = if (parsedDollars > 0) " $$parsedDollars" else ""
                    Text("$labelVerb$displayAmt Now", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
