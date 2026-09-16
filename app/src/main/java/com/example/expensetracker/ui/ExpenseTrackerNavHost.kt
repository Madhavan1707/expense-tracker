package com.example.expensetracker.ui

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.YearMonth
import com.example.expensetracker.ui.categories.CATEGORY_ID_ARG
import com.example.expensetracker.ui.categories.CategoryDetailScreen
import com.example.expensetracker.ui.categories.CategoryScreen
import com.example.expensetracker.ui.categories.CategorySpendListScreen
import com.example.expensetracker.ui.categories.MONTH_ARG
import com.example.expensetracker.ui.entry.EXPENSE_ID_ARG
import com.example.expensetracker.ui.entry.EntryScreen
import com.example.expensetracker.ui.entry.NEW_EXPENSE
import com.example.expensetracker.ui.entry.NO_TEMPLATE
import com.example.expensetracker.ui.entry.TEMPLATE_ID_ARG
import com.example.expensetracker.ui.home.HomeScreen
import com.example.expensetracker.ui.merchant.MERCHANT_NAME_ARG
import com.example.expensetracker.ui.merchant.MerchantDetailScreen
import com.example.expensetracker.ui.merchant.MerchantListScreen

/** Long enough to read as motion, short enough not to feel like waiting. */
private const val NAV_MILLIS = 220
private const val NAV_FADE_MILLIS = 140

private const val HOME_ROUTE = "home"
/** The browse screen: what each category has cost and where it went. */
private const val CATEGORIES_ROUTE = "categories"
/** The management screen: rename, recolour, reorder, archive. */
private const val EDIT_CATEGORIES_ROUTE = "categories-edit"
private const val CATEGORY_ROUTE = "category"
private const val ENTRY_ROUTE = "entry"
private const val PLACES_ROUTE = "places"
private const val MERCHANT_ROUTE = "merchant"

@Composable
fun ExpenseTrackerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    pendingTemplateId: Long = NO_TEMPLATE,
    onTemplateHandled: () -> Unit = {},
) {
    // A launcher shortcut arrives as an intent extra; open the prefilled form.
    LaunchedEffect(pendingTemplateId) {
        if (pendingTemplateId != NO_TEMPLATE) {
            navController.navigate(entryRoute(templateId = pendingTemplateId))
            onTemplateHandled()
        }
    }

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier,
        // NavHost defaults to a 700ms crossfade, which reads as lag rather than
        // motion. This is a short directional slide instead: the new screen
        // comes in from the right, and going back sends it out the same way.
        enterTransition = {
            slideInHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { it / 6 } +
                fadeIn(tween(NAV_MILLIS))
        },
        exitTransition = { fadeOut(tween(NAV_FADE_MILLIS)) },
        popEnterTransition = { fadeIn(tween(NAV_FADE_MILLIS)) },
        popExitTransition = {
            slideOutHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { it / 6 } +
                fadeOut(tween(NAV_MILLIS))
        },
    ) {
        composable(HOME_ROUTE) {
            HomeScreen(
                onAddExpense = { navController.navigate(entryRoute()) },
                onEditExpense = { id -> navController.navigate(entryRoute(expenseId = id)) },
                onShowCategories = { month -> navController.navigate(categoriesRoute(month)) },
                onShowCategory = { id, month -> navController.navigate(categoryRoute(id, month)) },
                onShowPlaces = { navController.navigate(PLACES_ROUTE) },
                onShowMerchant = { name -> navController.navigate(merchantRoute(name)) },
            )
        }

        composable(
            route = "$ENTRY_ROUTE?$EXPENSE_ID_ARG={$EXPENSE_ID_ARG}&$TEMPLATE_ID_ARG={$TEMPLATE_ID_ARG}",
            arguments = listOf(
                navArgument(EXPENSE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = NEW_EXPENSE
                },
                navArgument(TEMPLATE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = NO_TEMPLATE
                },
            ),
        ) {
            EntryScreen(onClose = { navController.popBackStack() })
        }

        composable(
            route = "$CATEGORIES_ROUTE/{$MONTH_ARG}",
            arguments = listOf(navArgument(MONTH_ARG) { type = NavType.StringType }),
        ) { entry ->
            val month = entry.arguments?.getString(MONTH_ARG)
            CategorySpendListScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { id ->
                    navController.navigate("$CATEGORY_ROUTE/$id/$month")
                },
                onEditCategories = { navController.navigate(EDIT_CATEGORIES_ROUTE) },
            )
        }

        composable(
            route = "$CATEGORY_ROUTE/{$CATEGORY_ID_ARG}/{$MONTH_ARG}",
            arguments = listOf(
                navArgument(CATEGORY_ID_ARG) { type = NavType.LongType },
                navArgument(MONTH_ARG) { type = NavType.StringType },
            ),
        ) {
            CategoryDetailScreen(
                onBack = { navController.popBackStack() },
                onExpenseClick = { id -> navController.navigate(entryRoute(expenseId = id)) },
                onPlaceClick = { name -> navController.navigate(merchantRoute(name)) },
            )
        }

        composable(EDIT_CATEGORIES_ROUTE) {
            CategoryScreen(onBack = { navController.popBackStack() })
        }

        composable(PLACES_ROUTE) {
            MerchantListScreen(
                onBack = { navController.popBackStack() },
                onMerchantClick = { name -> navController.navigate(merchantRoute(name)) },
            )
        }

        composable(
            route = "$MERCHANT_ROUTE/{$MERCHANT_NAME_ARG}",
            arguments = listOf(navArgument(MERCHANT_NAME_ARG) { type = NavType.StringType }),
        ) {
            // Navigation percent-decodes the path itself, so the ViewModel reads
            // the real merchant name straight out of its SavedStateHandle.
            MerchantDetailScreen(
                onBack = { navController.popBackStack() },
                onExpenseClick = { id -> navController.navigate(entryRoute(expenseId = id)) },
            )
        }
    }
}

private fun entryRoute(
    expenseId: Long = NEW_EXPENSE,
    templateId: Long = NO_TEMPLATE,
): String = "$ENTRY_ROUTE?$EXPENSE_ID_ARG=$expenseId&$TEMPLATE_ID_ARG=$templateId"

/** Merchant names contain spaces and slashes, so they cannot go in a path raw. */
private fun merchantRoute(name: String): String =
    "$MERCHANT_ROUTE/${Uri.encode(name)}"

/**
 * Both category routes carry the month the home screen was showing, so the
 * figures on the destination match the ones that were tapped. YearMonth's own
 * "2026-09" form is path-safe.
 */
private fun categoriesRoute(month: YearMonth): String = "$CATEGORIES_ROUTE/$month"

private fun categoryRoute(categoryId: Long, month: YearMonth): String =
    "$CATEGORY_ROUTE/$categoryId/$month"
