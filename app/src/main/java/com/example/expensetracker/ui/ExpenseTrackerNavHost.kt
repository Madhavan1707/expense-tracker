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
import com.example.expensetracker.ui.categories.CategoryScreen
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
private const val CATEGORIES_ROUTE = "categories"
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
                onManageCategories = { navController.navigate(CATEGORIES_ROUTE) },
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

        composable(CATEGORIES_ROUTE) {
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
