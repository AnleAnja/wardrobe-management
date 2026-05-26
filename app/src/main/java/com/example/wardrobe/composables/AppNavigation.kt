package com.example.wardrobe.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wardrobe.R
import com.example.wardrobe.navigation.NavigationEvent
import com.example.wardrobe.view_models.CalendarEvent
import com.example.wardrobe.view_models.CalendarNavigationEvent
import com.example.wardrobe.view_models.CalendarViewModel
import com.example.wardrobe.view_models.ItemDetailViewModel
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
    val currentBackStackEntry = navBackStackEntry
    val isOutfitsGalleryRoute = Screen.isOutfitsGalleryRoute(currentRoute)
    val outfitsViewModel: OutfitsViewModel? = if (isOutfitsGalleryRoute && currentBackStackEntry != null) {
        hiltViewModel(currentBackStackEntry)
    } else {
        null
    }
    val titleCalendar = stringResource(R.string.title_calendar)
    val titleWardrobe = stringResource(R.string.title_wardrobe)
    val titleOutfits = stringResource(R.string.title_outfits)
    val titleItemDetails = stringResource(R.string.title_item_details)
    val titleOutfitDetails = stringResource(R.string.title_outfit_details)
    BoxWithConstraints {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                GalleryNavigationRail(navController, currentRoute)
            }

            Scaffold(
                modifier = Modifier.weight(1f),
        topBar = {
            val route = currentRoute
            when {
                Screen.isCalendarTopLevelRoute(route) -> {
                    val uiState by calendarViewModel.uiState.collectAsState()
                    CalendarTopAppBar(
                        currentView = uiState.calendarView,
                        title = titleCalendar,
                        onToggleView = { calendarViewModel.onEvent(CalendarEvent.ToggleView) }
                    )
                }

                route == Screen.Wardrobe.route -> GalleryTopAppBar(
                    viewModel = wardrobeViewModel,
                    title = titleWardrobe,
                    onAddClick = { wardrobeViewModel.onEvent(WardrobeScreenEvent.AddItemClicked) }
                )

                Screen.isOutfitsGalleryRoute(route) -> {
                    if (outfitsViewModel != null) {
                        GalleryTopAppBar(
                            viewModel = wardrobeViewModel,
                            title = titleOutfits,
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
                        titleItemDetails,
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
                        titleOutfitDetails,
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
            if (!useNavigationRail) {
                GalleryBottomNavBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Wardrobe.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Wardrobe.route) {
                LaunchedEffect(Unit) {
                    wardrobeViewModel.navigationEvent.collect { event ->
                        event?.let {
                            when (it) {
                                is NavigationEvent.NavigateToAddItem ->
                                    navController.navigate(Screen.Wardrobe.route + "/add_item")
                                is NavigationEvent.NavigateToItemDetail ->
                                    navController.navigate(Screen.Wardrobe.route + "/item_detail/${it.item.id}")
                                else -> {}
                            }
                            wardrobeViewModel.navigationEventConsumed()
                        }
                    }
                }
                WardrobeGalleryScreen(wardrobeViewModel)
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
                val outfitsVm: OutfitsViewModel = hiltViewModel(backStackEntry)
                LaunchedEffect(Unit) {
                    outfitsVm.navigationEvent.collect { event ->
                        event?.let {
                            when (it) {
                                is NavigationEvent.NavigateToAddOutfit ->
                                    navController.navigate(Screen.Outfits.route + "/add_outfit")
                                is NavigationEvent.NavigateToOutfitDetail ->
                                    navController.navigate(Screen.Outfits.route + "/outfit_detail/${it.outfit.id}")
                                is NavigationEvent.NavigateToScheduleOutfit ->
                                    navController.navigate(Screen.Calendar.createRoute(it.outfit.id))
                                is NavigationEvent.NavigateBack -> navController.popBackStack()
                                else -> {}
                            }
                            outfitsVm.navigationEventConsumed()
                        }
                    }
                }
                OutfitGalleryScreen(outfitsVm)
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

                ItemDetailsContent(
                    uiState = uiState,
                    onOutfitClick = { outfitId ->
                        navController.navigate(Screen.Outfits.route + "/outfit_detail/$outfitId")
                    }
                )
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
                    },
                    onDelete = { scheduledOutfitId ->
                        detailViewModel.deleteScheduledOutfit(scheduledOutfitId)
                        navController.popBackStack()
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
            val label = stringResource(screen.labelResId)
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = isSelected,
                onClick = { navigateToTopLevelScreen(navController, currentRoute, screen) }
            )
        }
    }
}

@Composable
fun GalleryNavigationRail(
    navController: NavController,
    currentRoute: String?
) {
    val screens = listOf(Screen.Wardrobe, Screen.Calendar, Screen.Outfits, Screen.Inspiration)

    NavigationRail {
        screens.forEach { screen ->
            val isSelected = currentRoute?.startsWith(screen.route) ?: false
            val label = stringResource(screen.labelResId)
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = isSelected,
                onClick = { navigateToTopLevelScreen(navController, currentRoute, screen) }
            )
        }
    }
}

private fun navigateToTopLevelScreen(
    navController: NavController,
    currentRoute: String?,
    screen: Screen
) {
    val destination = Screen.topLevelRoute(screen)
    if (currentRoute?.startsWith(screen.route) == true) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
        }
    } else {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector, @StringRes val labelResId: Int) {
    data object Wardrobe : Screen("wardrobe", Icons.Filled.Checkroom, R.string.nav_wardrobe)
    data object Calendar : Screen("calendar", Icons.Outlined.CalendarToday, R.string.nav_calendar) {
        const val routeWithArgs = "calendar?outfitId={outfitId}"
        fun createRoute(outfitId: Int) = "calendar?outfitId=$outfitId"
    }

    data object Outfits : Screen("outfits", Icons.Filled.Accessibility, R.string.nav_outfits) {
        const val routeWithArgs = "outfits?date={date}"
        fun createRoute(date: LocalDate) = "outfits?date=${date.toEpochDay()}"
    }

    data object Inspiration : Screen("inspiration", Icons.Outlined.Lightbulb, R.string.nav_inspiration)

    companion object {
        fun isOutfitsGalleryRoute(route: String?): Boolean =
            route?.startsWith("outfits?date=") == true

        fun isCalendarTopLevelRoute(route: String?): Boolean =
            route?.startsWith("calendar?outfitId=") == true

        fun topLevelRoute(screen: Screen): String = when (screen) {
            Calendar -> "calendar?outfitId=-1"
            Outfits -> "outfits?date=-1"
            else -> screen.route
        }
    }
}
