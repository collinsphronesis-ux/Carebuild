package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CaseCategory
import com.example.data.model.CoverAssetKey
import com.example.data.model.SupportType
import com.example.data.model.UrgencyLevel

@Composable
fun SubmitCaseScreen(
    isSubmitting: Boolean,
    onCreateCase: (
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
        onSuccess: () -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var beneficiaryName by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var story by remember { mutableStateOf("") }
    var allocationPlan by remember { mutableStateOf("") }
    var goalDollarsText by remember { mutableStateOf("250") }
    var repaymentMonthsText by remember { mutableStateOf("6") }
    var category by remember { mutableStateOf(CaseCategory.MEDICAL) }
    var supportType by remember { mutableStateOf(SupportType.BOTH) }
    var isUrgent by remember { mutableStateOf(false) }
    var coverKey by remember { mutableStateOf(CoverAssetKey.MEDICAL) }
    var localPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        localPreviewUri = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Submit a Beneficiary Case",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Privacy first: use a first name or alias and city/region only. Never enter phone numbers, emails, or street addresses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        OutlinedTextField(
            value = beneficiaryName,
            onValueChange = { beneficiaryName = it.take(60); validationError = null },
            label = { Text("Beneficiary first name or alias") },
            placeholder = { Text("e.g., Samuel M.") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_name_input")
        )

        OutlinedTextField(
            value = region,
            onValueChange = { region = it.take(80); validationError = null },
            label = { Text("City & country") },
            placeholder = { Text("e.g., Kisumu, Kenya") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_region_input")
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(100); validationError = null },
            label = { Text("Appeal headline") },
            placeholder = { Text("e.g., Clinic Dialysis Support & Transport") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_title_input")
        )

        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CaseCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = {
                        category = cat
                        coverKey = when (cat) {
                            CaseCategory.MEDICAL -> CoverAssetKey.MEDICAL
                            CaseCategory.LIVELIHOOD -> CoverAssetKey.LIVELIHOOD
                            else -> CoverAssetKey.HERO
                        }
                    },
                    label = { Text(cat.label) },
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        Text(
            text = "Support Type",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SupportType.entries.forEach { st ->
                FilterChip(
                    selected = supportType == st,
                    onClick = { supportType = st },
                    label = { Text(st.label) },
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = goalDollarsText,
                onValueChange = { goalDollarsText = it.filter { ch -> ch.isDigit() }.take(6); validationError = null },
                label = { Text("Goal (USD)") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("submit_goal_input")
            )

            if (supportType != SupportType.SPONSORSHIP) {
                OutlinedTextField(
                    value = repaymentMonthsText,
                    onValueChange = { repaymentMonthsText = it.filter { ch -> ch.isDigit() }.take(2); validationError = null },
                    label = { Text("Loan Months") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = story,
            onValueChange = { story = it.take(1500); validationError = null },
            label = { Text("Their story & need") },
            placeholder = { Text("Describe the medical or livelihood situation clearly…") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_story_input")
        )

        OutlinedTextField(
            value = allocationPlan,
            onValueChange = { allocationPlan = it.take(500); validationError = null },
            label = { Text("How funds will be used") },
            placeholder = { Text("e.g., 70% clinic fees, 30% nutrition staples") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_allocation_input")
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mark as Urgent Need", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Prioritizes time-sensitive medical or crisis appeals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isUrgent,
                    onCheckedChange = { isUrgent = it }
                )
            }
        }

        Text(
            text = "Editorial Cover Artwork",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoverAssetKey.entries.forEach { key ->
                FilterChip(
                    selected = coverKey == key,
                    onClick = { coverKey = key },
                    label = { Text(key.label) },
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        if (localPreviewUri != null) {
            AsyncImage(
                model = localPreviewUri,
                contentDescription = "Selected local verification photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Image(
                painter = painterResource(id = coverDrawableFor(coverKey)),
                contentDescription = "Selected editorial cover preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        OutlinedButton(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preview Local Verification Photo (Optional)")
        }

        if (validationError != null) {
            Text(
                text = validationError ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val goalDollars = goalDollarsText.toLongOrNull() ?: 0L
                val goalCents = goalDollars * 100L
                val months = (repaymentMonthsText.toLongOrNull() ?: 6L).coerceIn(3L, 36L)

                when {
                    beneficiaryName.trim().length < 2 -> validationError = "Enter a beneficiary name or alias (2+ characters)."
                    region.trim().length < 2 -> validationError = "Enter a city and country (2+ characters)."
                    title.trim().length < 4 -> validationError = "Enter a headline (4+ characters)."
                    story.trim().length < 10 -> validationError = "Share at least 10 characters for the story."
                    allocationPlan.trim().length < 4 -> validationError = "Explain how funds will be allocated (4+ characters)."
                    goalCents < 500L -> validationError = "Funding goal must be at least $5."
                    else -> {
                        onCreateCase(
                            beneficiaryName,
                            region,
                            title,
                            story,
                            allocationPlan,
                            category,
                            supportType,
                            goalCents,
                            if (supportType == SupportType.SPONSORSHIP) 0L else months,
                            if (isUrgent) UrgencyLevel.URGENT else UrgencyLevel.STANDARD,
                            coverKey
                        ) {
                            beneficiaryName = ""
                            region = ""
                            title = ""
                            story = ""
                            allocationPlan = ""
                            localPreviewUri = null
                        }
                    }
                }
            },
            enabled = !isSubmitting,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("publish_case_button")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Publishing case…")
            } else {
                Icon(Icons.Filled.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Beneficiary Case", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
