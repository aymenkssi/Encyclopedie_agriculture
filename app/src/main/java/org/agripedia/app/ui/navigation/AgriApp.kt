package org.agripedia.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.agripedia.app.R
import org.agripedia.app.data.prefs.Settings
import org.agripedia.app.data.repo.CatalogState
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.appContainer
import org.agripedia.app.ui.calendar.CalendarScreen
import org.agripedia.app.ui.components.ErrorState
import org.agripedia.app.ui.components.LoadingState
import org.agripedia.app.ui.crops.CropDetailScreen
import org.agripedia.app.ui.crops.GardenDetailScreen
import org.agripedia.app.ui.crops.MyCropsScreen
import org.agripedia.app.ui.encyclopedia.EncyclopediaScreen
import org.agripedia.app.ui.home.HomeScreen
import org.agripedia.app.ui.onboarding.OnboardingScreen
import org.agripedia.app.ui.plant.PlantDetailScreen
import org.agripedia.app.ui.profile.HistoryScreen
import org.agripedia.app.ui.profile.NotesScreen
import org.agripedia.app.ui.profile.ProfileScreen
import org.agripedia.app.ui.profile.SourcesScreen
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val ENCYCLOPEDIA = "encyclopedia?query={query}&category={category}&adapted={adapted}"
    const val PLANT = "plant/{id}"
    const val CALENDAR = "calendar"
    const val CROPS = "crops"
    const val GARDEN = "garden/{id}"
    const val CROP = "crop/{id}"
    const val PROFILE = "profile"
    const val NOTES = "notes"
    const val HISTORY = "history"
    const val SOURCES = "sources"
    const val ONBOARDING = "onboarding"

    fun encyclopedia(query: String = "", category: String = "", adapted: Boolean = false) =
        "encyclopedia?query=${URLEncoder.encode(query, "UTF-8")}&category=$category&adapted=$adapted"
    fun plant(id: String) = "plant/$id"
    fun garden(id: String) = "garden/$id"
    fun crop(id: String) = "crop/$id"
}

private data class Tab(val route: String, val base: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "home", R.string.tab_home, Icons.Outlined.Home),
    Tab(Routes.encyclopedia(), "encyclopedia", R.string.tab_encyclopedia, Icons.AutoMirrored.Outlined.MenuBook),
    Tab(Routes.CALENDAR, "calendar", R.string.tab_calendar, Icons.Outlined.CalendarMonth),
    Tab(Routes.CROPS, "crops", R.string.tab_crops, Icons.Outlined.Yard),
    Tab(Routes.PROFILE, "profile", R.string.tab_profile, Icons.Outlined.Person),
)

/** Navigation actions passed to screens. */
class Nav(private val c: NavHostController) {
    fun plant(id: String) = c.navigate(Routes.plant(id))
    fun encyclopedia(query: String = "", category: String = "", adapted: Boolean = false) = tab(Routes.encyclopedia(query, category, adapted))
    fun garden(id: String) = c.navigate(Routes.garden(id))
    fun crop(id: String) = c.navigate(Routes.crop(id))
    fun calendar() = tab(Routes.CALENDAR)
    fun crops() = tab(Routes.CROPS)
    fun notes() = c.navigate(Routes.NOTES)
    fun history() = c.navigate(Routes.HISTORY)
    fun sources() = c.navigate(Routes.SOURCES)
    fun onboarding() = c.navigate(Routes.ONBOARDING)
    fun back() { c.popBackStack() }
    fun home() = c.navigate(Routes.HOME) { popUpTo(c.graph.findStartDestination().id) { inclusive = true } }
    fun tab(route: String) = c.navigate(route) {
        popUpTo(c.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AgriApp(settings: Settings?, openCalendar: Boolean, onCalendarOpened: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val appVm: AppViewModel = viewModel { AppViewModel(container, context.applicationContext) }
    val catalog by appVm.catalog.collectAsStateWithLifecycle()
    if (settings == null) { LoadingState(); return }

    val controller = rememberNavController()
    val nav = Nav(controller)
    val backStack by controller.currentBackStackEntryAsState()
    val currentBase = backStack?.destination?.route?.substringBefore('?')?.substringBefore('/')

    LaunchedEffect(openCalendar) { if (openCalendar) { nav.calendar(); onCalendarOpened() } }

    Scaffold(
        bottomBar = {
            if (currentBase != "onboarding") NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentBase == tab.base,
                        onClick = { nav.tab(tab.route) },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(stringResource(tab.label), maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        when (val state = catalog) {
            is CatalogState.Loading -> LoadingState(Modifier.padding(padding))
            is CatalogState.Error -> ErrorState(stringResource(R.string.error_catalog, state.message), onRetry = appVm::retryCatalog, modifier = Modifier.padding(padding))
            is CatalogState.Ready -> {
                val k = state.knowledge
                NavHost(controller, startDestination = if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING, modifier = Modifier.padding(padding)) {
                    composable(Routes.ONBOARDING) { OnboardingScreen(k, appVm, onDone = { nav.home() }) }
                    composable(Routes.HOME) { HomeScreen(k, appVm, nav) }
                    composable(
                        Routes.ENCYCLOPEDIA,
                        arguments = listOf(
                            navArgument("query") { type = NavType.StringType; defaultValue = "" },
                            navArgument("category") { type = NavType.StringType; defaultValue = "" },
                            navArgument("adapted") { type = NavType.BoolType; defaultValue = false },
                        ),
                    ) { entry ->
                        val args = entry.arguments
                        EncyclopediaScreen(
                            k, appVm, nav,
                            initialQuery = args?.getString("query").orEmpty(),
                            initialCategory = args?.getString("category").orEmpty(),
                            initialAdapted = args?.getBoolean("adapted") ?: false,
                        )
                    }
                    composable(Routes.PLANT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        PlantDetailScreen(k, appVm, nav, entry.arguments?.getString("id").orEmpty())
                    }
                    composable(Routes.CALENDAR) { CalendarScreen(k, appVm, nav) }
                    composable(Routes.CROPS) { MyCropsScreen(k, appVm, nav) }
                    composable(Routes.GARDEN, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        GardenDetailScreen(k, appVm, nav, entry.arguments?.getString("id").orEmpty())
                    }
                    composable(Routes.CROP, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        CropDetailScreen(k, appVm, nav, entry.arguments?.getString("id").orEmpty())
                    }
                    composable(Routes.PROFILE) { ProfileScreen(k, appVm, nav, settings) }
                    composable(Routes.NOTES) { NotesScreen(k, nav) }
                    composable(Routes.HISTORY) { HistoryScreen(k, nav) }
                    composable(Routes.SOURCES) { SourcesScreen(k, nav) }
                }
            }
        }
    }
}
