package com.example.face_recognition

sealed class Screen(val title :String ,val route:String) {
    object SplashScreen : Screen("SPLASH SCREEN" , "splash screen")
    object LoginScreen : Screen("LOG IN", "loginscreen")
    object SignUpScreen : Screen("SIGN UP", "signupscreen")
    object homeScreen : Screen("Home Screen" , "homescreen")
    object heartCamera : Screen("CameraScreen","camerascreen")
    object heartScreen : Screen("HeartRateScreen","heartratescreen")
    object vitalsResult : Screen("VitalsResult","vitalsresult")
}