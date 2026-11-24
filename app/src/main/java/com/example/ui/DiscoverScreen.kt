package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.UiState
import com.example.data.model.BeneficiaryCase
import com.example.data.model.CaseCategory

@Composable
fun DiscoverScreen(
    casesState: UiState<List<BeneficiaryCase>>,
    filterState: DiscoverFilterState,
    onSearchChange: (String) -> Unit,
    onToggleCategory: (CaseCategory) -> Unit,
    onToggleUrgent: () -> Unit,
    onToggleMicroloans: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenCaseDetail: (BeneficiaryCase) -> Unit,
    onQuickSupport: (BeneficiaryCase) -> Unit,
    onAskOreEverything: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoColumn = maxWidth >= 680.dp

        when (casesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Gathering community stories…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Could not load community cases",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = casesState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                val cases = casesState.data
                if (isTwoColumn) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            DiscoverHeaderSection(
                                filterState = filterState,
                                onSearchChange = onSearchChange,
                                onToggleCategory = onToggleCategory,
                                onToggleUrgent = onToggleUrgent,
                                onToggleMicroloans = onToggleMicroloans,
                                onAskOreEverything = onAskOreEverything
                            )
                        }
                        if (cases.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                EmptyCasesNotice(onClearFilters = onClearFilters)
                            }
                        } else {
                            items(cases, key = { it.id }) { item ->
                                BeneficiaryCaseCard(
                                    item = item,
                                    onOpenDetail = { onOpenCaseDetail(item) },
                                    onQuickSupport = { onQuickSupport(item) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "header") {
                            DiscoverHeaderSection(
                                filterState = filterState,
                                onSearchChange = onSearchChange,
                                onToggleCategory = onToggleCategory,
                                onToggleUrgent = onToggleUrgent,
                                onToggleMicroloans = onToggleMicroloans,
                                onAskOreEverything = onAskOreEverything
                            )
                        }
                        if (cases.isEmpty()) {
                            item(key = "empty") {
                                EmptyCasesNotice(onClearFilters = onClearFilters)
                            }
                        } else {
                            items(cases, key = { it.id }) { item ->
                                BeneficiaryCaseCard(
                                    item = item,
                                    onOpenDetail = { onOpenCaseDetail(item) },
                                    onQuickSupport = { onQuickSupport(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverHeaderSection(
    filterState: DiscoverFilterState,
    onSearchChange: (String) -> Unit,
    onToggleCategory: (CaseCategory) -> Unit,
    onToggleUrgent: () -> Unit,
    onToggleMicroloans: () -> Unit,
    onAskOreEverything: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Back a neighbor’s healing or livelihood",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Ore quick-summarize invitation banner
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onAskOreEverything)
                .testTag("discover_ask_ore_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ore_avatar),
                    contentDescription = "Ore assistant avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ask Ore to summarize everything",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Get an instant digest of urgent medical needs, loans & your impact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by name, region, or medical need…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("discover_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterState.onlyUrgent,
                onClick = onToggleUrgent,
                label = { Text("Urgent") },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("filter_urgent")
            )
            FilterChip(
                selected = filterState.onlyMicroloans,
                onClick = onToggleMicroloans,
                label = { Text("0% Microloans") },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("filter_microloans")
            )
            CaseCategory.entries.forEach { category ->
                FilterChip(
                    selected = filterState.selectedCategory == category,
                    onClick = { onToggleCategory(category) },
                    label = { Text(category.label) },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("filter_${category.raw}")
                )
            }
        }
    }
}

@Composable
private fun EmptyCasesNotice(onClearFilters: () -> Unit) {
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
                text = "No matching cases right now",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Try clearing your search or category filter to view all community stories.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onClearFilters,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Show All Cases")
            }
        }
    }
}
