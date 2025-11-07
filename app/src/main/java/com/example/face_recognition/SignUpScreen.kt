package com.example.face_recognition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.face_recognition.ui.theme.Face_recognitionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {



    // Whole screen layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // BackgroundGray
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        var name by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("")}
        var showPassword by rememberSaveable{mutableStateOf(false)}
        var showconfirmPassword by rememberSaveable{mutableStateOf(false)}
        var confirmPassword by rememberSaveable { mutableStateOf("") }
        var agree by rememberSaveable { mutableStateOf(false) }
        var showDialog by rememberSaveable { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf(false) }

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = "Join us to explore your health journey",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            )
        )


    Spacer(modifier = Modifier.height(32.dp)) // Space before fields

    FilledTextField(
            label = "Name",
            value = name,
            onValueChange = { name = it },
            leading = { Icon(Icons.Default.Person, null) }
        )

        Spacer(Modifier.height(12.dp))

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
            onValueChange = {
                password = it
                passwordError = it.length < 6 && it.isNotEmpty()
            },
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = { showPassword = !showPassword },
        )
        if (passwordError) {
            Text(
                text = "Password must be at least 6 characters",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp, top = 4.dp)
            )
        }


        Spacer(Modifier.height(12.dp))
       FilledTextField(
            label = "Confirm Password",
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            isPassword = true,
            showPassword = showconfirmPassword,
            onTogglePassword = { showconfirmPassword = !showconfirmPassword },

            trailing = {
                Icon(
                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
//                    tint = OnField
                )
            }
        )

        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = agree,
                onCheckedChange = { agree = !agree },
//                colors = CheckboxDefaults.colors(checkedColor = app_color)
            )
            val annotated = buildAnnotatedString {
                append("Agree with ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold , textDecoration = TextDecoration.Underline)) {
                    append("Terms & Condition")
                }
            }
            Text(text = annotated, fontSize = 13.sp, modifier = Modifier.padding(start = 2.dp))
        }

        Spacer(Modifier.height(4.dp))

        // ---- CTA ----
        Button(
            onClick = {authViewModel.signUp(email, password, name)
                email = ""
                password = ""
                name = ""
                confirmPassword = ""
                showDialog = true},
            enabled = agree && name.isNotBlank() && email.isNotBlank() && password.length >= 6,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
//            colors = ButtonDefaults.buttonColors(containerColor = app_color)
        ) {
            Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.SemiBold )
        }

        Spacer(Modifier.height(16.dp))

        // divider "Or sign up with"
        OrDivider(text = "Or sign up with")

        Spacer(Modifier.height(14.dp))

        // ---- Social buttons ----
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            SocialIconButton(painterResource(id = R.drawable.facebook), label = "F", onClick = {  })
            SocialIconButton(painterResource(id = R.drawable.search), onClick = {  })
        }

        Spacer(Modifier.height(18.dp))

        // ---- Bottom line ----
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", color = MaterialTheme.colorScheme.onBackground)
//            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = { navController.navigate(Screen.LoginScreen.route )}) {
                Text("Sign in", color = MaterialTheme.colorScheme.primary)
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = true },
                title = { Text("Success") },
                text = { Text("✅ Account created successfully!") },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}



@Composable
fun OrDivider(text: String) {
    val thickness = with(LocalDensity.current) { 1.dp }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(modifier = Modifier.weight(1f), thickness = thickness)
        Text(text, modifier = Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.8f), fontSize = 12.sp)
        Divider(modifier = Modifier.weight(1f), thickness = thickness)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardTypeEmail: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        leadingIcon = leading,
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            } else {
                trailing?.invoke()
            }
        }
        ,
        modifier = Modifier
            .fillMaxWidth()

            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
            disabledBorderColor = MaterialTheme.colorScheme.onBackground,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        placeholder = { Text(
            text = if (label == "Email")
                "example@gmail.com"
            else "") },
//        keyboardOptions = if (keyboardTypeEmail)
//            androidx.compose.ui.text.input.KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
//        else androidx.compose.ui.text.input.KeyboardOptions.Default
    )
}

@Composable
fun SocialIconButton(
    painter: Painter? = null,
    label: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (painter != null)
                Icon(
                    painter = painter,
                    contentDescription = label ?: "",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified // To keep original colors from image
                )
            else
                Text(label ?: "", fontSize = 22.sp)
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun SignUpPreview() {
//    Face_recognitionTheme {
//        SignUpScreen()
//    }
//}
