package com.lillah.dhikr.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val COLLECTIONS = "collections"
    const val PROGRESS = "progress"
    const val GUIDE = "guide"
    const val SETTINGS = "settings"
    const val UNIVERSAL = "universal"

    const val COLLECTION_DETAIL = "collection/{collectionId}"
    const val DHIKR_EDITOR = "dhikr_editor?dhikrId={dhikrId}&collectionId={collectionId}"
    const val COLLECTION_EDITOR = "collection_editor?collectionId={collectionId}"
    const val GUIDE_ARTICLE = "guide_article/{articleId}"
    const val MILESTONES = "milestones"
    const val MANAGE_DHIKR = "manage_dhikr"

    fun collectionDetail(id: Long) = "collection/$id"
    fun dhikrEditor(dhikrId: Long? = null, collectionId: Long? = null): String {
        val d = dhikrId ?: 0L
        val c = collectionId ?: 0L
        return "dhikr_editor?dhikrId=$d&collectionId=$c"
    }
    fun collectionEditor(collectionId: Long? = null) =
        "collection_editor?collectionId=${collectionId ?: 0L}"
    fun guideArticle(articleId: String) = "guide_article/$articleId"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    Home(Routes.HOME, "Count", Icons.Rounded.RadioButtonChecked, Icons.Outlined.RadioButtonChecked),
    Collections(Routes.COLLECTIONS, "Adhkar", Icons.Rounded.ViewCarousel, Icons.Outlined.ViewCarousel),
    Progress(Routes.PROGRESS, "Progress", Icons.Rounded.Insights, Icons.Outlined.Insights),
    Universal(Routes.UNIVERSAL, "World", Icons.Rounded.Public, Icons.Outlined.Public),
    Guide(Routes.GUIDE, "Guide", Icons.Rounded.AutoStories, Icons.Outlined.AutoStories),
    Settings(Routes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
}
