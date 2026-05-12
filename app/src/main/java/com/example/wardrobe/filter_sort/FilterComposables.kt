package com.example.wardrobe.filter_sort

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.wardrobe.CategoryHierarchy

@Composable
fun FilterChips(
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onSortClick,
            label = { Text("Sort") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Sort",
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
        AssistChip(
            onClick = onFilterClick,
            label = { Text("Filter") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDialog(
    sortOptions: List<SortOption>,
    currentSortOption: SortOption,
    onApplySort: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    // val sortOptions = listOf("Most Worn", "Least Worn", "Recently Worn", "Least Recently Worn", "Recently Purchased", "Least Recently Purchased")
    var selectedOption by remember(currentSortOption) { mutableStateOf(currentSortOption) }
    val sheetState = rememberModalBottomSheetState()
    //val viewModel = hiltViewModel<WardrobeViewModel>()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Sort as",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            sortOptions.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (option == selectedOption),
                            onClick = { selectedOption = option },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        onClick = null
                    )
                    Text(
                        text = option.displayName,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onDismiss()
                        onApplySort(selectedOption)
                    }
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    availableCategories: Set<Pair<String, String>>?,
    availableSeasons: List<String>,
    availableTemperature: Boolean,
    selectedCategories: Set<Pair<String, String>>,
    selectedSeasons: List<String>,
    selectedTemperature: Int?,
    onApplyFilters: (Set<Pair<String, String>>, List<String>, Int?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val localSelectedCategories = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(selectedCategories) } }
    val localSelectedSeasons = remember { mutableStateListOf<String>().also { it.addAll(selectedSeasons) } }
    var localTempInput by remember { mutableStateOf(selectedTemperature?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Options") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Seasons", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    availableSeasons.forEach { season ->
                        val isSelected = localSelectedSeasons.contains(season)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) localSelectedSeasons.remove(season) else localSelectedSeasons.add(season)
                            },
                            label = { Text(season) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else {
                                null
                            }
                        )
                    }
                }
                if (availableTemperature) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Temperature", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localTempInput,
                        onValueChange = {
                            localTempInput = it.filter { char -> char.isDigit() || char == '-' }
                        },
                        label = { Text("Temperature (°C)") },
                        placeholder = { Text("e.g., 15") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (availableCategories != null) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Column {
                        CategoryHierarchy.superCategories.forEach { superCategory ->
                            val subCategories =
                                CategoryHierarchy.hierarchy[superCategory] ?: emptyList()
                            val selectedSubCount = subCategories.count { subCat ->
                                Pair(superCategory, subCat) in localSelectedCategories
                            }
                            val isAllSelected =
                                selectedSubCount == subCategories.size && subCategories.isNotEmpty()
                            val isPartiallySelected = selectedSubCount > 0 && !isAllSelected

                            var isExpanded by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isAllSelected) {
                                            subCategories.forEach { subCat ->
                                                localSelectedCategories.remove(
                                                    Pair(
                                                        superCategory,
                                                        subCat
                                                    )
                                                )
                                            }
                                        } else {
                                            subCategories.forEach { subCat ->
                                                if (Pair(
                                                        superCategory,
                                                        subCat
                                                    ) !in localSelectedCategories
                                                ) {
                                                    localSelectedCategories.add(
                                                        Pair(
                                                            superCategory,
                                                            subCat
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TriStateCheckbox(
                                    state = when {
                                        isAllSelected -> ToggleableState.On
                                        isPartiallySelected -> ToggleableState.Indeterminate
                                        else -> ToggleableState.Off
                                    },
                                    onClick = {
                                        if (isAllSelected) {
                                            subCategories.forEach { subCat ->
                                                localSelectedCategories.remove(
                                                    Pair(superCategory, subCat)
                                                )
                                            }
                                        } else {
                                            subCategories.forEach { subCat ->
                                                if (Pair(
                                                        superCategory,
                                                        subCat
                                                    ) !in localSelectedCategories
                                                ) localSelectedCategories.add(
                                                    Pair(
                                                        superCategory,
                                                        subCat
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )

                                Text(
                                    superCategory,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                )

                                IconButton(onClick = { isExpanded = !isExpanded }) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand"
                                    )
                                }
                            }

                            if (isExpanded) {
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    subCategories.forEach { subCategory ->
                                        val isChecked = Pair(
                                            superCategory,
                                            subCategory
                                        ) in localSelectedCategories
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val pair = Pair(superCategory, subCategory)
                                                    if (isChecked) localSelectedCategories.remove(
                                                        pair
                                                    ) else localSelectedCategories.add(
                                                        pair
                                                    )
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(checked = isChecked, onCheckedChange = null)
                                            Text(
                                                subCategory,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApplyFilters(localSelectedCategories.toSet(), localSelectedSeasons,
                        localTempInput.toIntOrNull()
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClearFilters()
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}