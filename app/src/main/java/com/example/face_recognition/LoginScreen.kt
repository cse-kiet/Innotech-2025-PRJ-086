package com.example.face_recognition

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthException

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.face_recognition.ui.theme.Face_recognitionTheme
import kotlinx.coroutines.launch
import kotlin.Result

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(

    navController: NavController,
    authViewModel: AuthViewModel
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            HealLensLottieAnimation(
                assetName = "login.json",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = "Login to continue your journey",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            FilledTextField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                leading = { Icon(Icons.Default.Email, null) },
                keyboardTypeEmail = true
            )

            Spacer(Modifier.height(12.dp))

            FilledTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                isPassword = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword }
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    // Launch a coroutine to call suspending login
                    coroutineScope.launch {
                        try {
                            // Suppose login returns Unit on success or throws an exception on failure
                            authViewModel.login(email.trim(), password)
                            // success -> navigate or whatever
//                            navController.navigate(/* your destination */)
                        } catch (e: Exception) {
                            val message = when (e) {
                                is FirebaseAuthException -> {
                                    when (e.errorCode) {
                                        "ERROR_USER_NOT_FOUND" -> "User does not exist. Please sign up."
                                        "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
                                        "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                                        "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
                                        else -> "Authentication failed: ${e.message}"
                                    }
                                }
                                else -> "Unexpected error: ${e.localizedMessage ?: "Please try again"}"
                            }

                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                },
                enabled = email.isNotBlank() && password.length >= 6,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }


//    // Whole screen layout
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background) // BackgroundGray
//            .padding(24.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//
//        HealLensLottieAnimation(
//            assetName = "login.json",
//            modifier = Modifier
//                .size(200.dp)
//                .padding(bottom = 16.dp)
//        )
//
//        Text(
//            text = "Welcome Back",
//            style = MaterialTheme.typography.headlineSmall.copy(
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.primary
//            )
//        )
//
//        Text(
//            text = "Login to continue your journey",
//            style = MaterialTheme.typography.bodyMedium.copy(
//                color = MaterialTheme.colorScheme.onBackground
//            )
//        )
//
//
//        Spacer(modifier = Modifier.height(32.dp)) // Space before fields
//        var email by rememberSaveable { mutableStateOf("") }
//        var password by rememberSaveable { mutableStateOf("")}
//        var showPassword by rememberSaveable{mutableStateOf(false)}
//        FilledTextField(
//            label = "Email",
//            value = email,
//            onValueChange = { email = it },
//            leading = { Icon(Icons.Default.Email, null) },
//            keyboardTypeEmail = true
//        )
//
//        Spacer(Modifier.height(12.dp))
//
//        FilledTextField(
//            label = "Password",
//            value = password,
//            onValueChange = { password = it },
//            isPassword = true,
//            showPassword = showPassword,
//            onTogglePassword = { showPassword = !showPassword },
//
//            )
//        Spacer(Modifier.height(12.dp))
//
//        Button(
//            onClick = {authViewModel.login(email,password)},
//            enabled =  email.isNotBlank() && password.length >= 6,
//            shape = RoundedCornerShape(20.dp),
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(52.dp)
////            colors = ButtonDefaults.buttonColors(containerColor = app_color)
//        ) {
//            Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold )
//        }

            Spacer(Modifier.height(16.dp))

            // divider "Or sign in with"
            OrDivider(text = "Or sign in with")

            Spacer(Modifier.height(14.dp))

            // ---- Social buttons ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                SocialIconButton(
                    painterResource(id = R.drawable.facebook),
                    label = "F",
                    onClick = { })
                SocialIconButton(painterResource(id = R.drawable.search), onClick = { })
            }

            Spacer(Modifier.height(18.dp))


            // Navigate to Signup
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don’t have an account?", color = MaterialTheme.colorScheme.onBackground)
//            Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { navController.navigate(Screen.SignUpScreen.route) }) {
                    Text("Sign up", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

