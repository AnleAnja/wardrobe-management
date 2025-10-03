import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.wardrobe.AddItemEvent
import com.example.wardrobe.AddItemUiState
import com.example.wardrobe.AddItemViewModel
import com.example.wardrobe.ItemDetailUiState
import com.example.wardrobe.ItemDetailViewModel
import com.example.wardrobe.NavigationEvent
import com.example.wardrobe.WardrobeScreenEvent
import com.example.wardrobe.WardrobeViewModel
import com.example.wardrobe.database.entities.WardrobeItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@Composable
fun WardrobeAppContent() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "wardrobe_gallery"
    ) {
        composable("wardrobe_gallery") {
            WardrobeGalleryScreen(
                onNavigateToAddItem = {
                    navController.navigate("add_item")
                },
                onNavigateToItemDetail = { item ->
                    navController.navigate("item_detail/${item.id}")
                }
            )
        }

        composable("add_item") {
            AddItemScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "item_detail/{itemId}",
            arguments = listOf(navArgument("itemId") {
                type =
                    NavType.IntType
            })
        ) { backStackEntry ->
            ItemDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { itemId ->
                    navController.navigate("item_detail/$itemId/edit")
                },
                onDeleteClick = { TODO() }
            )
        }

        composable(
            route = "item_detail/{itemId}/edit",
            arguments = listOf(navArgument("itemId") {
                type =
                    NavType.IntType
            })
        ) { backStackEntry ->
            AddItemScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun WardrobeGalleryScreen(
    onNavigateToAddItem: () -> Unit,
    onNavigateToItemDetail: (WardrobeItem) -> Unit
) {
    val viewModel: WardrobeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    // TODO: Filter implementieren (01.10.)
    // Nach Jahreszeit
    // Nach Kategorie
    // TODO: Sortierung implementieren (01.10.)
    // Wie oft getragen
    // Wann zuletzt getragen
    // Wann gekauft
    /*
    2.Modal-Zustände
        Füge Zustände im WardrobeGalleryScreen Composable hinzu, um die Sichtbarkeit der Filter- und Sortier-Modals zu steuern.
    3.Filter-Leiste Composable
        Erstelle ein neues Composable für die Leiste mit den "Filter" und "Sortieren" Buttons.
    4.Filter-Modal Composable
        Erstelle ein Composable für das Filter-Modal. Darin können Benutzer Saisons und Kategorien auswählen (ähnlich wie beim AddItemScreen).
    5.Sortier-Modal Composable
        Erstelle ein Composable für das Sortier-Modal. Hier können Benutzer eine der Sortieroptionen auswählen.
    6.ViewModel-Logik
        Implementiere die Logik im WardrobeViewModel, um die Garderoben-Items basierend auf den ausgewählten Filtern und der Sortierung zu verarbeiten.
    (7.Datenbank-Abfragen
        Für eine bessere Performance sollten die Filter- und Sortieroperationen idealerweise direkt in der Datenbank über Room-Queries erfolgen, anstatt die gesamte Liste im ViewModel zu laden und dann zu filtern/sortieren.)
     */
    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            NavigationEvent.NavigateToAddItem -> {
                onNavigateToAddItem()
                viewModel.navigationEventConsumed()
            }

            is NavigationEvent.NavigateToItemDetail -> {
                onNavigateToItemDetail((navigationEvent as NavigationEvent.NavigateToItemDetail).item)
                viewModel.navigationEventConsumed()
            }

            null -> {}
        }
    }

    Scaffold(
        topBar = { WardrobeTopAppBar() },
        bottomBar = { WardrobeBottomNavBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            FilterChips()
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Column {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.onEvent(WardrobeScreenEvent.RefreshRequested) }
                        ) {
                            Text("Retry")
                        }
                    }
                }

                else -> {

                    WardrobeGrid(
                        items = uiState.wardrobeItems,
                        onItemClick = { item ->
                            viewModel.onEvent(WardrobeScreenEvent.ItemClicked(item))
                        }
                    )
                }
            }
        }
    }

    /*Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All Pieces",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(
                onClick = { viewModel.onEvent(WardrobeScreenEvent.AddItemClicked) }
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Column {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.onEvent(WardrobeScreenEvent.RefreshRequested) }
                    ) {
                        Text("Retry")
                    }
                }
            }

            else -> {

                WardrobeItemsGrid(
                    items = uiState.wardrobeItems,
                    onItemClick = { item ->
                        viewModel.onEvent(WardrobeScreenEvent.ItemClicked(item))
                    }
                )
            }
        }
    }*/
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeTopAppBar() {
    val viewModel: WardrobeViewModel = hiltViewModel()
    TopAppBar(
        title = {
            Text(
                "Wardrobe",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        actions = {
            IconButton(onClick = {
                viewModel.onEvent(WardrobeScreenEvent.AddItemClicked)
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun FilterChips() {
    val filters = listOf("All", "Tops", "Bottoms", "Shoes")
    var selectedFilter by remember { mutableStateOf("All") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { selectedFilter = filter },
                label = { Text(filter) },
                leadingIcon = if (filter == "All") null else {
                    {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "$filter options",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun ItemDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onDeleteClick: () -> Unit
) {
    val viewModel: ItemDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ItemDetailsTopAppBar(
                onNavigateBack = onNavigateBack,
                onNavigateToEdit = onNavigateToEdit,
                onDeleteClick = onDeleteClick,
                uiState
            )
        }
    ) { paddingValues ->
        ItemDetailsContent(
            uiState = uiState,
            modifier = Modifier.padding(paddingValues)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

    }
}

// TODO: bei onNavigateToEdit auch das Bild laden
// TODO: Dialog Check bei Abbruch + Löschen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailsTopAppBar(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    uiState: ItemDetailUiState
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Item Details") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            uiState.item?.let { item ->
                                onNavigateToEdit(item.id)
                            }
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeleteClick()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ItemDetailsContent(uiState: ItemDetailUiState, modifier: Modifier = Modifier) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.errorMessage != null -> {
            Column {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        else -> {
            uiState.item?.let {
                val context = LocalContext.current
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .aspectRatio(0.75f),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(it.imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selected image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column {
                        Text(
                            "Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "Category", value = it.category)
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                        val rating = if (it.rating == null || it.rating <= 0) {
                            "-"
                        } else {
                            "★".repeat(it.rating)
                        }
                        DetailRow(label = "Rating", value = rating)
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                        DetailRow(label = "Price", value = "${it.price} €")
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                        val formattedDate = it.purchaseDate?.let {
                            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Not set"
                        DetailRow(label = "Purchase Date", value = formattedDate)
                    }
                }
            } ?: Text("Item Details würden hier stehen...")

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = Color.Gray)
        if (value != null) {
            Text(value, fontSize = 16.sp, color = Color.Black)
        }
    }
}

@Composable
fun AddItemScreen(onNavigateBack: () -> Unit) {
    val viewModel: AddItemViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
            viewModel.onEvent(AddItemEvent.ClearSuccess)
        }
    }

    Scaffold(
        topBar = { AddItemTopAppBar(onNavigateBack, uiState) }
    ) { paddingValues ->
        AddItemForm(modifier = Modifier.padding(paddingValues), uiState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemTopAppBar(onNavigateBack: () -> Unit, uiState: AddItemUiState) {
    TopAppBar(
        title = {
            Text(
                text = if (uiState.itemId != null) "Edit Item" else "Add Item",
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Close screen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemForm(modifier: Modifier = Modifier, uiState: AddItemUiState) {
    val viewModel: AddItemViewModel = hiltViewModel()
    var showDatePicker by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var isSeasonsMenuExpanded by remember { mutableStateOf(false) }

    val selectedSeasonsList = remember(uiState.seasons) {
        uiState.seasons.split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            imageUri = uri.toString()
        }
        viewModel.onEvent(AddItemEvent.ImageUriChanged(imageUri))
    }

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = uiState.purchaseDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.onEvent(AddItemEvent.PurchaseDateChanged(it))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // Abstand zum TopAppBar

            ImagePickerCard(
                imageUri = imageUri,
                onPickClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onClearClick = {
                    imageUri = null
                    viewModel.onEvent(AddItemEvent.ImageUriChanged(null))
                }
            )
            Text(
                "Purchased Date",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            OutlinedTextField(
                value = uiState.purchaseDate?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Select a date",
                onValueChange = {},
                placeholder = { Text("Select Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(color = Color.Black, width = 1.dp, shape = RoundedCornerShape(8.dp))
                    .clickable { showDatePicker = true },
                enabled = false
            )
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            ExposedDropdownMenuBox(
                expanded = isCategoryMenuExpanded,
                onExpandedChange = { isCategoryMenuExpanded = it }
            ) {
                // Category
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    value = uiState.category,
                    onValueChange = { text ->
                        viewModel.onEvent(AddItemEvent.CategoryChanged(text))
                        isCategoryMenuExpanded = text.isNotBlank()
                    },
                    placeholder = { Text("Select Category") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                )
                val filteredOptions = viewModel.categories.filter {
                    it.contains(
                        uiState.category,
                        ignoreCase = true
                    ) && it.lowercase() != uiState.category.lowercase()
                }
                if (filteredOptions.isNotEmpty() && isCategoryMenuExpanded) {
                    ExposedDropdownMenu(
                        expanded = true,
                        onDismissRequest = { isCategoryMenuExpanded = false }
                    ) {
                        filteredOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.onEvent(AddItemEvent.CategoryChanged(selectionOption))
                                    isCategoryMenuExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
            Text(
                "Rating",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    val rating = index + 1
                    Button(
                        onClick = { viewModel.onEvent(AddItemEvent.RatingChanged(rating)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rating <= uiState.rating)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("★")
                    }
                }
            }
            Text(
                "Price",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            OutlinedTextField(
                value = uiState.price,
                onValueChange = { viewModel.onEvent(AddItemEvent.PriceChanged(it)) },
                placeholder = { Text("Select Price (€)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(8.dp)
            )
            Text(
                "Seasons",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            ExposedDropdownMenuBox(
                expanded = isSeasonsMenuExpanded,
                onExpandedChange = { isSeasonsMenuExpanded = it }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(), // Required
                    readOnly = true, // We will display the comma-separated string
                    value = uiState.seasons.ifEmpty { "Select seasons" }, // Display selected seasons or placeholder
                    onValueChange = {},
                    placeholder = { Text("Select Seasons") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSeasonsMenuExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                )
                ExposedDropdownMenu(
                    expanded = isSeasonsMenuExpanded,
                    onDismissRequest = { isSeasonsMenuExpanded = false }
                ) {
                    viewModel.seasons.forEach { season ->
                        val isSelected = selectedSeasonsList.contains(season)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null // Null because onClick of DropdownMenuItem handles it
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(season)
                                }
                            },
                            onClick = {
                                if (isSelected) {
                                    selectedSeasonsList.remove(season)
                                } else {
                                    selectedSeasonsList.add(season)
                                }
                                // Update the ViewModel's uiState with the new comma-separated string
                                viewModel.onEvent(
                                    AddItemEvent.SeasonsChanged(
                                        selectedSeasonsList.joinToString(
                                            ", "
                                        )
                                    )
                                )
                                // Do not close the menu: isSeasonsMenuExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.onEvent(AddItemEvent.SaveItem) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = !uiState.isLoading

        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Item", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun ImagePickerCard(
    imageUri: String?,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    if (imageUri == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .drawBehind {
                    drawRoundRect(
                        color = Color.Gray,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = dashEffect
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .clickable(onClick = onPickClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "Upload Photo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Upload a file",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "PNG / JPG",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(shape = RoundedCornerShape(8.dp), width = 1.dp, color = Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Selected image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onClearClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove"
                )
            }
            IconButton(
                onClick = onPickClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    }
}

@Composable
fun WardrobeGrid(
    items: List<WardrobeItem>,
    onItemClick: (WardrobeItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(items) { item ->
            WardrobeItemCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun WardrobeItemCard(
    item: WardrobeItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(true) }
    LaunchedEffect(key1 = item.imageUri) {
        if (item.imageUri != null && item.imageUri.startsWith("content://")) {
            val uri = item.imageUri.toUri()

            // Finde heraus, welche URIs die App persistent speichern darf
            val persistedUris = context.contentResolver.persistedUriPermissions

            // Prüfe, ob unsere URI in der Liste der erlaubten URIs ist
            val hasPersistedPermission = persistedUris.any { it.uri == uri && it.isReadPermission }

            if (!hasPersistedPermission) {
                Log.e("WardrobeItemCard", "Keine Berechtigung mehr für URI: ${item.imageUri}")
                hasPermission = false // Setze den State auf "keine Berechtigung"
            }
        }
    }
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .aspectRatio(0.75f),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (hasPermission) {
            AsyncImage(
                model = item.imageUri,
                contentDescription = item.category,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Keine Berechtigung mehr, zeige einen Fehler an
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Permission denied",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun WardrobeBottomNavBar() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val items = listOf("Wardrobe", "Calendar", "Outfits", "Inspiration")
    val icons = listOf(
        Icons.Filled.Checkroom,
        Icons.Outlined.CalendarToday,
        Icons.Filled.Accessibility,
        Icons.Outlined.Lightbulb
    )

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedIndex == index,
                onClick = { selectedIndex = index }
            )
        }
    }
}

// TODO: Daten lokal speichern (02.10.)
// TODO: Aufteilen in Dateien (03.10.)
// TODO: Design (03.10.)
// TODO: Import ermöglichen (02.10.)
// TODO: Outfit Creator (04.10.)
// TODO: Outfit Library (05.10.)
// TODO: Scheduling (06.10.)
// TODO: Data Calculation (06.10.)