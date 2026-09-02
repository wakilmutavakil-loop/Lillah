package com.lillah.dhikr.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lillah.dhikr.data.guide.GuideContent
import com.lillah.dhikr.ui.components.AuroraBackground
import com.lillah.dhikr.ui.screens.collections.CollectionDetailScreen
import com.lillah.dhikr.ui.screens.collections.CollectionDetailViewModel
import com.lillah.dhikr.ui.screens.collections.CollectionsScreen
import com.lillah.dhikr.ui.screens.collections.CollectionsViewModel
import com.lillah.dhikr.ui.screens.editor.CollectionEditorScreen
import com.lillah.dhikr.ui.screens.editor.CollectionEditorViewModel
import com.lillah.dhikr.ui.screens.editor.DhikrEditorScreen
import com.lillah.dhikr.ui.screens.editor.DhikrEditorViewModel
import com.lillah.dhikr.ui.screens.guide.GuideArticleScreen
import com.lillah.dhikr.ui.screens.guide.GuideScreen
import com.lillah.dhikr.ui.screens.home.HomeScreen
import com.lillah.dhikr.ui.screens.home.HomeViewModel
import com.lillah.dhikr.ui.screens.manage.ManageDhikrScreen
import com.lillah.dhikr.ui.screens.manage.ManageDhikrViewModel
import com.lillah.dhikr.ui.screens.progress.ProgressScreen
import com.lillah.dhikr.ui.screens.progress.ProgressViewModel
import com.lillah.dhikr.ui.screens.settings.SettingsScreen
import com.lillah.dhikr.ui.screens.settings.SettingsViewModel
import com.lillah.dhikr.ui.screens.universal.UniversalScreen
import com.lillah.dhikr.ui.screens.universal.UniversalViewModel
import com.lillah.dhikr.ui.theme.Motion
import com.lillah.dhikr.ui.theme.Spacing
import com.lillah.dhikr.ui.vm.LocalActivity
import com.lillah.dhikr.ui.vm.containerViewModel

private const val SLIDE = 60

@Composable
fun DhikrApp(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val showNavBar = currentRoute in TopLevelDestination.entries.map { it.route }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Screens are inset individually rather than by padding the whole host, so the collection
    // cover can run edge to edge under the status bar while everything else stays clear of it.
    val contentPadding = PaddingValues(
        top = topInset,
        bottom = if (showNavBar) Spacing.navClearance else bottomInset + Spacing.l,
    )

    AuroraBackground(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideInHorizontally(tween(Motion.Medium, easing = Motion.Emphasized)) {
                    it / SLIDE
                } + fadeIn(tween(Motion.Medium))
            },
            exitTransition = { fadeOut(tween(Motion.Quick)) },
            popEnterTransition = { fadeIn(tween(Motion.Medium)) },
            popExitTransition = {
                slideOutHorizontally(tween(Motion.Medium, easing = Motion.Emphasized)) {
                    it / SLIDE
                } + fadeOut(tween(Motion.Quick))
            },
        ) {
            composable(Routes.HOME) {
                val viewModel = containerViewModel { HomeViewModel(it) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onCount = viewModel::count,
                    onUndo = viewModel::undo,
                    onReset = viewModel::resetRound,
                    onSelectDhikr = viewModel::selectDhikr,
                    onSetTarget = viewModel::setTarget,
                    onAddDhikr = { navController.navigate(Routes.dhikrEditor()) },
                    onOpenCollection = { navController.navigate(Routes.collectionDetail(it)) },
                    onOpenProgress = { navController.navigateToTab(Routes.PROGRESS) },
                    onDismissMilestone = viewModel::dismissMilestone,
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.COLLECTIONS) {
                val viewModel = containerViewModel { CollectionsViewModel(it) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                CollectionsScreen(
                    state = state,
                    onOpenCollection = { navController.navigate(Routes.collectionDetail(it)) },
                    onCreateCollection = { navController.navigate(Routes.collectionEditor()) },
                    onManageAdhkar = { navController.navigate(Routes.MANAGE_DHIKR) },
                    onSelectDhikr = { id ->
                        viewModel.selectDhikr(id) { navController.navigateToTab(Routes.HOME) }
                    },
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.PROGRESS) {
                val viewModel = containerViewModel { ProgressViewModel(it) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val range by viewModel.range.collectAsStateWithLifecycle()
                ProgressScreen(
                    state = state,
                    range = range,
                    onSelectRange = viewModel::selectRange,
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.UNIVERSAL) {
                val viewModel = containerViewModel { UniversalViewModel(it) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val activity = LocalActivity.current
                UniversalScreen(
                    state = state,
                    onSignIn = { method -> activity?.let { viewModel.signIn(it, method) } },
                    onSignOut = viewModel::signOut,
                    onSyncNow = viewModel::syncNow,
                    onDismissMessage = viewModel::dismissMessage,
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.GUIDE) {
                GuideScreen(
                    onOpenArticle = { navController.navigate(Routes.guideArticle(it)) },
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.SETTINGS) {
                val viewModel = containerViewModel { SettingsViewModel(it) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(viewModel) {
                    viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
                }
                SettingsScreen(
                    state = state,
                    isDark = isDark,
                    onSelectPalette = viewModel::setPalette,
                    onSelectMode = viewModel::setThemeMode,
                    onToggleHaptics = viewModel::setHaptics,
                    onToggleSound = viewModel::setSound,
                    onToggleKeepScreenOn = viewModel::setKeepScreenOn,
                    onToggleVolumeKeys = viewModel::setVolumeKeys,
                    onToggleArabic = viewModel::setShowArabic,
                    onToggleTransliteration = viewModel::setShowTransliteration,
                    onToggleMeaning = viewModel::setShowMeaning,
                    onSetDailyGoal = viewModel::setDailyGoal,
                    onManageAdhkar = { navController.navigate(Routes.MANAGE_DHIKR) },
                    onRestoreDefaults = viewModel::restoreDefaults,
                    onClearToday = viewModel::clearToday,
                    onClearHistory = viewModel::clearAllHistory,
                    contentPadding = contentPadding,
                )
            }

            composable(
                route = Routes.COLLECTION_DETAIL,
                arguments = listOf(navArgument("collectionId") { type = NavType.LongType }),
            ) { entry ->
                val collectionId = entry.arguments?.getLong("collectionId") ?: 0L
                val viewModel = containerViewModel(key = "collection_$collectionId") {
                    CollectionDetailViewModel(it, collectionId)
                }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                CollectionDetailScreen(
                    state = state,
                    onBack = navController::popBackStack,
                    onOpenDhikr = { id ->
                        viewModel.open(id) { navController.navigateToTab(Routes.HOME) }
                    },
                    onCountOne = viewModel::countOne,
                    onUndoOne = viewModel::undoOne,
                    onPickCover = viewModel::chooseCover,
                    onClearCover = viewModel::clearCover,
                    onEditCollection = {
                        navController.navigate(Routes.collectionEditor(collectionId))
                    },
                    onAddDhikr = {
                        navController.navigate(Routes.dhikrEditor(collectionId = collectionId))
                    },
                    onResetRounds = viewModel::resetRounds,
                    contentPadding = PaddingValues(bottom = bottomInset + Spacing.l),
                )
            }

            composable(
                route = Routes.DHIKR_EDITOR,
                arguments = listOf(
                    navArgument("dhikrId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("collectionId") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { entry ->
                val dhikrId = entry.arguments?.getLong("dhikrId") ?: 0L
                val collectionId = entry.arguments?.getLong("collectionId") ?: 0L
                val viewModel = containerViewModel(key = "dhikr_editor_${dhikrId}_$collectionId") {
                    DhikrEditorViewModel(it, dhikrId, collectionId.takeIf { id -> id != 0L })
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                val collections by viewModel.collections.collectAsStateWithLifecycle()
                DhikrEditorScreen(
                    state = state,
                    collections = collections,
                    onUpdate = viewModel::update,
                    onSave = { viewModel.save { navController.popBackStack() } },
                    onDelete = { viewModel.delete { navController.popBackStack() } },
                    onArchive = { viewModel.archive { navController.popBackStack() } },
                    onBack = navController::popBackStack,
                    contentPadding = contentPadding,
                )
            }

            composable(
                route = Routes.COLLECTION_EDITOR,
                arguments = listOf(
                    navArgument("collectionId") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { entry ->
                val collectionId = entry.arguments?.getLong("collectionId") ?: 0L
                val viewModel = containerViewModel(key = "collection_editor_$collectionId") {
                    CollectionEditorViewModel(it, collectionId)
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                CollectionEditorScreen(
                    state = state,
                    onUpdate = viewModel::update,
                    onPickCover = viewModel::chooseCover,
                    onClearCover = viewModel::clearCover,
                    onSave = { viewModel.save { navController.popBackStack() } },
                    onDelete = {
                        viewModel.delete {
                            // The detail screen for a deleted collection cannot be returned to.
                            navController.popBackStack(Routes.COLLECTIONS, inclusive = false)
                        }
                    },
                    onBack = navController::popBackStack,
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.MANAGE_DHIKR) {
                val viewModel = containerViewModel { ManageDhikrViewModel(it) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ManageDhikrScreen(
                    state = state,
                    onBack = navController::popBackStack,
                    onEdit = { navController.navigate(Routes.dhikrEditor(dhikrId = it)) },
                    onAdd = { navController.navigate(Routes.dhikrEditor()) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onArchive = viewModel::archive,
                    onRestore = viewModel::restore,
                    onMove = viewModel::move,
                    contentPadding = contentPadding,
                )
            }

            composable(
                route = Routes.GUIDE_ARTICLE,
                arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
            ) { entry ->
                val articleId = entry.arguments?.getString("articleId").orEmpty()
                GuideArticleScreen(
                    article = GuideContent.find(articleId),
                    onBack = navController::popBackStack,
                    contentPadding = contentPadding,
                )
            }
        }

        if (showNavBar) {
            DhikrNavBar(
                currentRoute = currentRoute,
                onSelect = { navController.navigateToTab(it.route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showNavBar) 92.dp else 16.dp),
        )
    }
}

/**
 * Tab switching keeps a single entry per tab and restores where the user was, so returning to a
 * tab does not reset its scroll position or lose a half-finished screen.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
