package com.example.groupproject_m2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.groupproject_m2.ui.theme.GroupProject_m2Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GroupProject_m2Theme {
                GroupProject_m2Root()
            }
        }
    }
}

@Composable
fun GroupProject_m2Root() {
    val auth = remember { FirebaseAuth.getInstance() }
    val startsUnverified = auth.currentUser != null && auth.currentUser?.isEmailVerified == false
    var isLoggedIn by rememberSaveable { mutableStateOf(auth.currentUser?.isEmailVerified == true) }
    var authMessage by rememberSaveable {
        mutableStateOf(
            if (startsUnverified) "Please verify your email before logging in." else null
        )
    }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showVerificationActions by rememberSaveable { mutableStateOf(startsUnverified) }

    if (isLoggedIn) {
        GroupProject_m2App(
            onLogoutClick = {
                auth.signOut()
                isLoggedIn = false
                authMessage = null
                showVerificationActions = false
            }
        )
    } else {
        LoginScreen(
            isLoading = isLoading,
            authMessage = authMessage,
            showVerificationActions = showVerificationActions,
            onLoginClick = { email, password ->
                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val verified = auth.currentUser?.isEmailVerified == true
                            if (verified) {
                                authMessage = null
                                showVerificationActions = false
                                isLoggedIn = true
                            } else {
                                authMessage = "Email not verified. Please check your inbox."
                                showVerificationActions = true
                            }
                        } else {
                            showVerificationActions = false
                            authMessage = when (task.exception) {
                                is FirebaseAuthInvalidUserException,
                                is FirebaseAuthInvalidCredentialsException ->
                                    "Username or password is incorrect."
                                else -> "Login failed. Please try again."
                            }
                        }
                    }
            },
            onRegisterClick = { email, password ->
                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            auth.currentUser?.sendEmailVerification()
                                ?.addOnCompleteListener { verificationTask ->
                                    isLoading = false
                                    showVerificationActions = true
                                    authMessage = if (verificationTask.isSuccessful) {
                                        "Verification email sent. Check your inbox and verify before logging in."
                                    } else {
                                        "Account created, but verification email could not be sent."
                                    }
                                }
                        } else {
                            isLoading = false
                            showVerificationActions = false
                            authMessage = when (task.exception) {
                                is FirebaseAuthUserCollisionException ->
                                    "An account with this email already exists."
                                is FirebaseAuthInvalidCredentialsException ->
                                    "Please enter a valid email address."
                                else -> "Registration failed. Please try again."
                            }
                        }
                    }
            },
            onResendVerificationClick = {
                val user = auth.currentUser
                if (user == null) {
                    authMessage = "Log in first, then you can resend verification."
                    showVerificationActions = false
                } else {
                    isLoading = true
                    user.sendEmailVerification().addOnCompleteListener { task ->
                        isLoading = false
                        showVerificationActions = true
                        authMessage = if (task.isSuccessful) {
                            "Verification email resent."
                        } else {
                            "Could not resend verification email."
                        }
                    }
                }
            },
            onCheckVerificationClick = {
                val user = auth.currentUser
                if (user == null) {
                    authMessage = "No active user. Please log in again."
                    showVerificationActions = false
                } else {
                    isLoading = true
                    user.reload().addOnCompleteListener {
                        isLoading = false
                        if (auth.currentUser?.isEmailVerified == true) {
                            authMessage = null
                            showVerificationActions = false
                            isLoggedIn = true
                        } else {
                            authMessage = "Email still not verified."
                            showVerificationActions = true
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    authMessage: String?,
    showVerificationActions: Boolean,
    onLoginClick: (email: String, password: String) -> Unit,
    onRegisterClick: (email: String, password: String) -> Unit,
    onResendVerificationClick: () -> Unit,
    onCheckVerificationClick: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = if (isRegisterMode) "Register" else "Log in")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        label = { Text("Email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                onRegisterClick(email.trim(), password)
                            } else {
                                onLoginClick(email.trim(), password)
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(vertical = 2.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isRegisterMode) "Create account" else "Log in")
                        }
                    }
                    authMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    TextButton(
                        onClick = { isRegisterMode = !isRegisterMode },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(if (isRegisterMode) "Already have an account? Log in" else "Register")
                    }
                    if (showVerificationActions) {
                        TextButton(
                            onClick = onResendVerificationClick,
                            enabled = !isLoading
                        ) {
                            Text("Resend verification email")
                        }
                        TextButton(
                            onClick = onCheckVerificationClick,
                            enabled = !isLoading
                        ) {
                            Text("I've verified my email")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GroupProject_m2App(
    onLogoutClick: () -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("groupProject_m2") },
                    actions = {
                        TextButton(onClick = onLogoutClick) {
                            Text("Log out")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Greeting(
                name = "Jumper",
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Map", Icons.Default.Public),
    FAVORITES("Liked", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to our cliff locater app",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    GroupProject_m2Theme {
        LoginScreen(
            isLoading = false,
            authMessage = null,
            showVerificationActions = false,
            onLoginClick = { _, _ -> },
            onRegisterClick = { _, _ -> },
            onResendVerificationClick = {},
            onCheckVerificationClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    GroupProject_m2Theme {
        GroupProject_m2App(onLogoutClick = {})
    }
}
