package com.librefocus.ui.limits

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation

const val LIMITS_GRAPH_ROUTE = "limits_graph"
const val LIMIT_LIST_ROUTE = "limit_list"
const val CREATE_LIMIT_ROUTE = "create_limit"
const val CREATE_LIMIT_WITH_ID_ROUTE = "create_limit/{limitId}"
const val SET_LIMIT_ROUTE = "set_limit"
const val SCHEDULE_LIMIT_ROUTE = "schedule_limit"
const val USAGE_LIMIT_ROUTE = "usage_limit"
const val LAUNCH_COUNT_LIMIT_ROUTE = "launch_count_limit"

fun NavGraphBuilder.limitsGraph(
    navController: NavController
) {
    navigation(
        startDestination = LIMIT_LIST_ROUTE,
        route = LIMITS_GRAPH_ROUTE
    ) {
        composable(LIMIT_LIST_ROUTE) {
            LimitListScreen(
                onNavigateToCreate = { limitId ->
                    if (limitId != null) {
                        navController.navigate("create_limit/$limitId")
                    } else {
                        navController.navigate(CREATE_LIMIT_ROUTE)
                    }
                }
            )
        }

        composable(
            route = CREATE_LIMIT_ROUTE
        ) {
            CreateLimitScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToSetLimit = {
                    navController.navigate(SET_LIMIT_ROUTE)
                }
            )
        }

        composable(
            route = CREATE_LIMIT_WITH_ID_ROUTE,
            arguments = listOf(
                navArgument("limitId") { type = NavType.StringType }
            )
        ) {
            CreateLimitScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToSetLimit = {
                    navController.navigate(SET_LIMIT_ROUTE)
                }
            )
        }

        composable(SET_LIMIT_ROUTE) {
            SetLimitScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToSchedule = {
                    navController.navigate(SCHEDULE_LIMIT_ROUTE)
                },
                onNavigateToUsage = {
                    navController.navigate(USAGE_LIMIT_ROUTE)
                },
                onNavigateToLaunchCount = {
                    navController.navigate(LAUNCH_COUNT_LIMIT_ROUTE)
                }
            )
        }

        composable(SCHEDULE_LIMIT_ROUTE) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(CREATE_LIMIT_ROUTE)
            }
            
            ScheduleLimitScreen(
                onNavigateBack = { config ->
                    if (config != null) {
                        parentEntry.savedStateHandle["limit_config_result"] = config
                    }
                    navController.popBackStack(CREATE_LIMIT_ROUTE, false)
                }
            )
        }

        composable(USAGE_LIMIT_ROUTE) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(CREATE_LIMIT_ROUTE)
            }
            
            UsageLimitScreen(
                onNavigateBack = { config ->
                    if (config != null) {
                        parentEntry.savedStateHandle["limit_config_result"] = config
                    }
                    navController.popBackStack(CREATE_LIMIT_ROUTE, false)
                }
            )
        }

        composable(LAUNCH_COUNT_LIMIT_ROUTE) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(CREATE_LIMIT_ROUTE)
            }
            
            LaunchCountLimitScreen(
                onNavigateBack = { config ->
                    if (config != null) {
                        parentEntry.savedStateHandle["limit_config_result"] = config
                    }
                    navController.popBackStack(CREATE_LIMIT_ROUTE, false)
                }
            )
        }
    }
}
