package com.example.groupproject_m2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import android.content.Intent
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var selectedSpot by remember { mutableStateOf<CliffSpot?>(null) }
    val likedSpots = remember { mutableStateListOf<CliffSpot>() }

    if (selectedSpot != null) {
        val spot = selectedSpot!!
        val isLiked = likedSpots.contains(spot)
        DetailScreen(
            spot = spot,
            weatherApiKey = "af5cdfa47871a592fb68ea185f67f94b",
            isLiked = isLiked,
            onToggleLike = {
                if (likedSpots.contains(spot)) {
                    likedSpots.remove(spot)
                } else {
                    likedSpots.add(spot)
                }
            },
            onBackClick = { selectedSpot = null }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = { Icon(it.icon, contentDescription = it.label) },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = {
                            if (it == AppDestinations.REELS) {
                                context.startActivity(
                                    Intent(context, ReelsActivity::class.java).apply {
                                        putExtra(ReelsActivity.EXTRA_SPOT_NAME, "")
                                        putExtra(ReelsActivity.EXTRA_SPOT_LOCATION, "")
                                    }
                                )
                            } else {
                                currentDestination = it
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("Cliff Jumper") },
                        actions = {
                            TextButton(onClick = onLogoutClick) {
                                Text("Log out")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentDestination) {
                        AppDestinations.HOME -> MapScreen(
                            onSpotClick = { spot -> selectedSpot = spot }
                        )
                        AppDestinations.FAVORITES -> LikedSpotsScreen(
                            spots = likedSpots,
                            onSpotClick = { spot -> selectedSpot = spot }
                        )
                        AppDestinations.REELS -> { /* launched as Activity, nothing to render */ }
                        AppDestinations.PROFILE -> Text(
                            "Profile Screen - Coming Soon",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedSpotsScreen(
    spots: List<CliffSpot>,
    onSpotClick: (CliffSpot) -> Unit
) {
    if (spots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No liked cliffs yet")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(spots) { spot ->
            val difficultyColor = when (spot.difficulty) {
                "Beginner" -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                "Intermediate" -> androidx.compose.ui.graphics.Color(0xFFF9A825)
                "Advanced" -> androidx.compose.ui.graphics.Color(0xFFC62828)
                else -> androidx.compose.ui.graphics.Color.Gray
            }

            Card(
                onClick = { onSpotClick(spot) },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(60.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            .background(difficultyColor)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = spot.name,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = spot.location,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = spot.height,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .background(difficultyColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = spot.difficulty,
                                    color = difficultyColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Map", Icons.Default.Public),
    FAVORITES("Liked", Icons.Default.Favorite),
    REELS("Explore", Icons.Default.PlayCircle),
    PROFILE("Profile", Icons.Default.AccountBox),
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





