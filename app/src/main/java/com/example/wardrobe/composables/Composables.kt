import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.wardrobe.composables.AddItemScreen
import com.example.wardrobe.composables.AddOutfitScreen
import com.example.wardrobe.composables.AddScheduledItemScreen
import com.example.wardrobe.composables.AddTopAppBar
import com.example.wardrobe.composables.CalendarContent
import com.example.wardrobe.composables.CalendarTopAppBar
import com.example.wardrobe.composables.DetailsTopAppBar
import com.example.wardrobe.composables.ItemDetailsContent
import com.example.wardrobe.composables.OutfitDetailsContent
import com.example.wardrobe.composables.ScheduledOutfitDetailsContent
import com.example.wardrobe.filter_sort.FilterChips
import com.example.wardrobe.filter_sort.FilterDialog
import com.example.wardrobe.filter_sort.OutfitFilters
import com.example.wardrobe.filter_sort.OutfitSortOption
import com.example.wardrobe.filter_sort.SortDialog
import com.example.wardrobe.filter_sort.WardrobeFilters
import com.example.wardrobe.filter_sort.WardrobeSortOption
import com.example.wardrobe.view_models.CalendarEvent
import com.example.wardrobe.view_models.CalendarNavigationEvent
import com.example.wardrobe.view_models.CalendarViewModel
import com.example.wardrobe.view_models.ItemDetailViewModel
import com.example.wardrobe.view_models.NavigationEvent
import com.example.wardrobe.view_models.OutfitDetailViewModel
import com.example.wardrobe.view_models.OutfitScreenEvent
import com.example.wardrobe.view_models.OutfitsViewModel
import com.example.wardrobe.view_models.WardrobeScreenEvent
import com.example.wardrobe.view_models.WardrobeViewModel
import java.time.LocalDate


@Composable
fun MainAppContent() {
    val wardrobeViewModel: WardrobeViewModel = hiltViewModel()
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val outfitsViewModel: OutfitsViewModel? = if (currentRoute == Screen.Outfits.routeWithArgs) {
        hiltViewModel(navBackStackEntry!!)
    } else {
        null // Für alle anderen Routen ist es null.
    }
    Scaffold(
        topBar = {
            val route = currentRoute
            when {
                route == Screen.Calendar.routeWithArgs -> {
                    val uiState by calendarViewModel.uiState.collectAsState()
                    CalendarTopAppBar(
                        currentView = uiState.calendarView,
                        title = "Calendar",
                        onToggleView = { calendarViewModel.onEvent(CalendarEvent.ToggleView) }
                    )
                }

                route == Screen.Wardrobe.route -> GalleryTopAppBar(
                    viewModel = wardrobeViewModel,
                    title = "Wardrobe",
                    onAddClick = { wardrobeViewModel.onEvent(WardrobeScreenEvent.AddItemClicked) }
                )

                route == Screen.Outfits.routeWithArgs -> {
                    if (outfitsViewModel != null) {
                        GalleryTopAppBar(
                            viewModel = wardrobeViewModel,
                            title = "Outfits",
                            onAddClick = { outfitsViewModel.onEvent(OutfitScreenEvent.AddOutfitClicked) }
                        )
                    }
                }

                route?.contains("add_") == true ||
                        route?.contains("edit") == true -> {
                    val isEditMode = route.contains("edit")
                    val isOutfit = route.contains("outfit")
                    AddTopAppBar(
                        { navController.popBackStack() },
                        isEditMode,
                        isOutfit
                    )
                }

                route?.contains("item_detail/") == true -> {
                    val detailViewModel: ItemDetailViewModel = hiltViewModel(navBackStackEntry!!)
                    val detailUiState by detailViewModel.uiState.collectAsState()

                    DetailsTopAppBar(
                        "Item Details",
                        { navController.popBackStack() },
                        { itemId -> navController.navigate(Screen.Wardrobe.route + "/item_detail/$itemId/edit") },
                        { itemId ->
                            wardrobeViewModel.deleteItem(itemId)
                            navController.popBackStack()
                        },
                        detailUiState.item?.id
                    )
                }

                route?.contains("outfit_detail/") == true -> {
                    val detailViewModel: OutfitDetailViewModel = hiltViewModel(navBackStackEntry!!)
                    val detailUiState by detailViewModel.uiState.collectAsState()

                    DetailsTopAppBar(
                        "Outfit Details",
                        { navController.popBackStack() },
                        { outfitId -> navController.navigate(Screen.Outfits.route + "/outfit_detail/$outfitId/edit") },
                        { outfitId ->
                            detailViewModel.deleteOutfit(outfitId)
                            navController.popBackStack()
                        },
                        detailUiState.outfit?.id
                    )
                }
            }
        },
        bottomBar = {
            GalleryBottomNavBar(navController, currentRoute)
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Wardrobe.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Wardrobe.route) {
                WardrobeNavContainer(
                    wardrobeViewModel,
                    { navController.navigate(Screen.Wardrobe.route + "/add_item") },
                    { navController.navigate(Screen.Wardrobe.route + "/item_detail/$it") })
            }
            composable(
                route = Screen.Calendar.routeWithArgs,
                arguments = listOf(navArgument("outfitId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) {
                LaunchedEffect(key1 = true) {
                    calendarViewModel.navigationEvent.collect { event ->
                        event?.let {
                            when (it) {
                                is CalendarNavigationEvent.NavigateToEditScheduledOutfit -> {
                                    navController.navigate(Screen.Calendar.route + "/scheduled_outfit/${it.scheduledOutfitId}/add_item")
                                }
                            }
                            calendarViewModel.onNavigationEventConsumed()
                        }
                    }
                }
                val uiState by calendarViewModel.uiState.collectAsState()
                val allOutfits by calendarViewModel.allOutfitsFlow.collectAsState(initial = emptyList())
                if (uiState.isLoading && uiState.outfit == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    CalendarContent(
                        uiState = uiState,
                        onEvent = { event -> calendarViewModel.onEvent(event) },
                        allOutfits,
                        onOutfitClick = { scheduledOutfitId -> navController.navigate(Screen.Calendar.route + "/scheduled_outfit/$scheduledOutfitId") }
                    )
                }
            }
            composable(
                route = Screen.Outfits.routeWithArgs,
                arguments = listOf(navArgument("date") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                if (outfitsViewModel != null) {
                    OutfitsNavContainer(
                        outfitsViewModel,
                        { navController.navigate(Screen.Outfits.route + "/add_outfit") },
                        { navController.navigate(Screen.Outfits.route + "/outfit_detail/$it") },
                        { outfitId -> navController.navigate(Screen.Calendar.createRoute(outfitId)) },
                        { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.Inspiration.route) { InspirationScreen() }
            composable(Screen.Wardrobe.route + "/add_item") {
                AddItemScreen({ navController.popBackStack() })
            }

            composable(
                route = Screen.Wardrobe.route + "/item_detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) { backStackEntry ->
                val detailViewModel: ItemDetailViewModel = hiltViewModel(backStackEntry)
                val uiState by detailViewModel.uiState.collectAsState()

                ItemDetailsContent(uiState = uiState)
            }

            composable(
                route = Screen.Wardrobe.route + "/item_detail/{itemId}/edit",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) {
                AddItemScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Outfits.route + "/add_outfit") {

                AddOutfitScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.Outfits.route + "/outfit_detail/{outfitId}",
                arguments = listOf(navArgument("outfitId") { type = NavType.IntType })
            ) { backStackEntry ->
                val detailViewModel: OutfitDetailViewModel = hiltViewModel(backStackEntry)
                val uiState by detailViewModel.uiState.collectAsState()

                OutfitDetailsContent(
                    uiState = uiState,
                    onEvent = { event -> detailViewModel.onEvent(event) },
                    onItemClick = { itemId -> navController.navigate(Screen.Wardrobe.route + "/item_detail/$itemId") })
            }

            composable(
                route = Screen.Outfits.route + "/outfit_detail/{outfitId}/edit",
                arguments = listOf(navArgument("outfitId") { type = NavType.IntType })
            ) {
                AddOutfitScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Calendar.route + "/scheduled_outfit/{scheduledOutfitId}",
                arguments = listOf(navArgument("scheduledOutfitId") { type = NavType.IntType })
            ) { backStackEntry ->
                val detailViewModel: OutfitDetailViewModel = hiltViewModel(backStackEntry)
                val uiState by detailViewModel.uiState.collectAsState()

                ScheduledOutfitDetailsContent(
                    uiState = uiState,
                    onNavigateToFullOutfitDetail = { outfitId ->
                        navController.navigate(Screen.Outfits.route + "/outfit_detail/$outfitId")
                    },
                    onItemClick = { itemId ->
                        navController.navigate(Screen.Wardrobe.route + "/item_detail/$itemId")
                    },
                    onNavigateToEdit = { scheduledOutfitId ->
                        navController.navigate(Screen.Calendar.route + "/scheduled_outfit/$scheduledOutfitId/add_item")
                    }
                )
            }

            composable(
                route = Screen.Calendar.route + "/scheduled_outfit/{scheduledOutfitId}/add_item",
                arguments = listOf(navArgument("scheduledOutfitId") { type = NavType.IntType })
            ) {
                AddScheduledItemScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun WardrobeNavContainer(
    viewModel: WardrobeViewModel,
    onNavigateToAddItem: () -> Unit,
    onNavigateToItemDetail: (Int) -> Unit
) {
    val navController = rememberNavController()

    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collect { event ->
            event?.let {
                when (it) {
                    is NavigationEvent.NavigateToAddItem -> onNavigateToAddItem()
                    is NavigationEvent.NavigateToItemDetail -> onNavigateToItemDetail(it.item.id)
                    else -> {}
                }
                viewModel.navigationEventConsumed()
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Wardrobe.route) {
        composable(Screen.Wardrobe.route) {
            WardrobeGalleryScreen(viewModel)
        }
    }
}

@Composable
fun OutfitsNavContainer(
    viewModel: OutfitsViewModel,
    onNavigateToAddOutfit: () -> Unit,
    onNavigateToOutfitDetail: (Int) -> Unit,
    onNavigateToScheduleOutfit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {

    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collect { event ->
            event?.let {
                when (it) {
                    is NavigationEvent.NavigateToAddOutfit -> onNavigateToAddOutfit()
                    is NavigationEvent.NavigateToOutfitDetail -> onNavigateToOutfitDetail(it.outfit.id)
                    is NavigationEvent.NavigateToScheduleOutfit -> onNavigateToScheduleOutfit(it.outfit.id)
                    is NavigationEvent.NavigateBack -> onNavigateBack()
                    else -> {}
                }
                viewModel.navigationEventConsumed()
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Outfits.route) {
        composable(Screen.Outfits.route) {
            OutfitGalleryScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopAppBar(
    viewModel: WardrobeViewModel,
    title: String,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { fileUri ->
                viewModel.onEvent(WardrobeScreenEvent.ImportJson(context, fileUri))
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onEvent(WardrobeScreenEvent.ExportJson(context, it))
        }
    }

    TopAppBar(
        title = { Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        navigationIcon = {
            Row {
                IconButton(onClick = {
                    filePickerLauncher.launch("application/json")
                }) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Import JSON"
                    )
                }
                IconButton(onClick = {
                    exportLauncher.launch("wardrobe_export.json")
                }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export JSON"
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, "Add")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun WardrobeGalleryScreen(viewModel: WardrobeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showSortDialog) {
        SortDialog(
            sortOptions = WardrobeSortOption.entries,
            currentSortOption = uiState.currentSortOption,
            onApplySort = { sortOption ->
                viewModel.onEvent(WardrobeScreenEvent.ApplySortOption(sortOption as WardrobeSortOption))
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            availableCategories = uiState.availableCategories,
            availableSeasons = uiState.availableSeasons,
            availableTemperature = false,
            selectedCategories = uiState.currentFilters.selectedCategories,
            selectedSeasons = uiState.currentFilters.selectedSeasons,
            selectedTemperature = 999,
            onApplyFilters = { categories, seasons, _ ->
                viewModel.onEvent(
                    WardrobeScreenEvent.ApplyFilters(
                        WardrobeFilters(
                            seasons,
                            categories
                        )
                    )
                )
                showFilterDialog = false
            },
            onClearFilters = {
                viewModel.onEvent(WardrobeScreenEvent.ClearFilters)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FilterChips(
            onSortClick = { showSortDialog = true },
            onFilterClick = { showFilterDialog = true }
        )
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
                GalleryGrid(
                    items = uiState.wardrobeItems,
                    onItemClick = { item -> viewModel.onEvent(WardrobeScreenEvent.ItemClicked(item)) },
                    imageUriProvider = { wardrobeItem -> wardrobeItem.imageUri },
                    contentDescriptionProvider = { wardrobeItem -> wardrobeItem.category }
                )
            }
        }
    }
}

@Composable
fun OutfitGalleryScreen(viewModel: OutfitsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showSortDialog) {
        SortDialog(
            sortOptions = OutfitSortOption.entries,
            currentSortOption = uiState.currentSortOption,
            onApplySort = { sortOption ->
                viewModel.onEvent(OutfitScreenEvent.ApplySortOption(sortOption as OutfitSortOption))
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            availableCategories = null,
            availableSeasons = uiState.availableSeasons,
            availableTemperature = true,
            selectedCategories = emptySet(),
            selectedSeasons = uiState.currentFilters.selectedSeasons,
            selectedTemperature = uiState.currentFilters.temperature,
            onApplyFilters = { _, seasons, temp ->
                viewModel.onEvent(
                    OutfitScreenEvent.ApplyFilters(
                        OutfitFilters(
                            seasons,
                            temp
                        )
                    )
                )
                showFilterDialog = false
            },
            onClearFilters = {
                viewModel.onEvent(OutfitScreenEvent.ClearFilters)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FilterChips(
            onSortClick = { showSortDialog = true },
            onFilterClick = { showFilterDialog = true }
        )
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
                        onClick = { viewModel.onEvent(OutfitScreenEvent.RefreshRequested) }
                    ) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                GalleryGrid(
                    items = uiState.outfits,
                    onItemClick = { outfit ->
                        if (uiState.isSelectionMode) {
                            viewModel.onEvent(OutfitScreenEvent.OutfitSelectedForDate(outfit))
                        } else {
                            viewModel.onEvent(OutfitScreenEvent.OutfitClicked(outfit))
                        }
                    },
                    imageUriProvider = { outfit ->
                        outfit.imageUriTeaser
                    },
                    contentDescriptionProvider = { outfit ->
                        outfit.id.toString()
                    }
                )
            }
        }
    }
}

@Composable
fun <T> GalleryGrid(
    items: List<T>,
    imageUriProvider: (T) -> String?,
    contentDescriptionProvider: (T) -> String?,
    onItemClick: (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(items) { item ->
            GalleryCard(
                item,
                imageUriProvider,
                contentDescriptionProvider,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun <T> GalleryCard(
    item: T,
    imageUriProvider: (T) -> String?,
    contentDescriptionProvider: (T) -> String?,
    onClick: () -> Unit
) {
    val imageUri = imageUriProvider(item)
    val description = contentDescriptionProvider(item)
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(true) }
    LaunchedEffect(key1 = imageUri) {
        if (imageUri != null && imageUri.startsWith("content://")) {
            val uri = imageUri.toUri()
            val persistedUris = context.contentResolver.persistedUriPermissions
            val hasPersistedPermission = persistedUris.any { it.uri == uri && it.isReadPermission }

            if (!hasPersistedPermission) {
                hasPermission = false
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
                model = imageUri,
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
fun GalleryBottomNavBar(
    navController: NavController,
    currentRoute: String?
) {
    val screens = listOf(Screen.Wardrobe, Screen.Calendar, Screen.Outfits, Screen.Inspiration)

    NavigationBar {
        screens.forEach { screen ->
            val isSelected = currentRoute?.startsWith(screen.route) ?: false
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                label = { Text(screen.label) },
                selected = isSelected,
                onClick = {
                    if (currentRoute?.startsWith(screen.route) == true) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    data object Wardrobe : Screen("wardrobe", Icons.Filled.Checkroom, "Wardrobe")
    data object Calendar : Screen("calendar", Icons.Outlined.CalendarToday, "Calendar") {
        const val routeWithArgs = "calendar?outfitId={outfitId}"
        fun createRoute(outfitId: Int) = "calendar?outfitId=$outfitId"
    }

    data object Outfits : Screen("outfits", Icons.Filled.Accessibility, "Outfits") {
        const val routeWithArgs = "outfits?date={date}"
        fun createRoute(date: LocalDate) = "outfits?date=${date.toEpochDay()}"
    }

    data object Inspiration : Screen("inspiration", Icons.Outlined.Lightbulb, "Inspiration")
}

@Composable
fun InspirationScreen() {
    val boardUrl = "https://de.pinterest.com/aja0159/outfits/"
    var isLoading by remember { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .fillMaxSize() ,
        contentAlignment = Alignment.Center
    ) {
        InspirationView(
            url = boardUrl,
            onPageFinished = { isLoading = false },
            onPageStarted = { isLoading = true }
        )

        if (isLoading) {
            CircularProgressIndicator()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InspirationView(url: String, onPageFinished: () -> Unit, onPageStarted: () -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                with(settings) {
                    // Pinterest needs JS, but lock down the rest of the surface area.
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = false
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    setSafeBrowsingEnabled(true)
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onPageFinished()
                    }
                }

                loadUrl(url)
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}

// TODO: Refactoring / Aufräumen
