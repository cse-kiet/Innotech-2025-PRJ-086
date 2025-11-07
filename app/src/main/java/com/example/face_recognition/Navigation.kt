package com.example.face_recognition


import android.os.Build
import androidx.annotation.RequiresApi

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Navigation(navController: NavHostController, authViewModel: AuthViewModel) {
    val vitalsViewModel: VitalsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route // Start from Splash first
    ) {

        // ✅ Splash Screen handles auth check & navigation itself
        composable(Screen.SplashScreen.route) {
            SplashScreen(
                onNavigate = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Screen.SplashScreen.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.SignUpScreen.route) {
            SignUpScreen(navController, authViewModel) // <-- Add this
        }

        // ✅ Login Screen
        composable(Screen.LoginScreen.route) {
            val user by authViewModel.currentUser.observeAsState()
            LoginScreen(navController, authViewModel)

            // If login successful → move to home
            LaunchedEffect(user) {
                if (user != null) {
                    navController.navigate(Screen.homeScreen.route) {
                        popUpTo(Screen.LoginScreen.route) { inclusive = true }
                    }
                }
            }
        }

        // ✅ Home Screen
        composable(Screen.homeScreen.route) {
            val user by authViewModel.currentUser.observeAsState()

            if (user != null) {
                HomeScreen(
                    user = user!!,
                    onSpO2Click = {},
                    onHeartRateClick = {navController.navigate(Screen.heartCamera.route)},
                    onBpClick = {},
                    onStressClick = {},
                    onLogoutClick = {
                        authViewModel.logOut()
                        navController.navigate(Screen.LoginScreen.route) {
                            popUpTo(Screen.homeScreen.route) { inclusive = true }
                        }
                    }
                )
            } else {
                // If logged out, navigate back to login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.LoginScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(
            route = Screen.heartCamera.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
        ) {
            ScanScreen(
                navController = navController,
                viewModel = vitalsViewModel
            )
        }

        composable(Screen.vitalsResult.route) {
            VitalResultScreen(vitalsViewModel)
        }
    }
}
