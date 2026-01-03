# Aegis Auth Android SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.navchetna.aegis/aegis-auth-android.svg)](https://search.maven.org/artifact/com.navchetna.aegis/aegis-auth-android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

Aegis Auth is a unified identity management system providing secure Android authentication. Consolidation of disparate identity providers into a single canonical source.

## Features

- 🔐 **Email/Password Authentication** - Traditional login with secure password handling
- 🔑 **WebAuthn/Passkey Support** - Passwordless authentication using biometrics
- 🛡️ **Multi-Factor Authentication (MFA)** - TOTP-based additional security layer
- 📱 **Biometric Authentication** - Fingerprint and face recognition
- 🔗 **OAuth/SSO Integration** - Google, GitHub, Microsoft, and custom providers
- 🎫 **JWT Token Management** - Automatic token refresh and secure storage
- 📊 **Session Analytics** - User behavior tracking and insights
- 🏢 **Multi-Tenant Support** - Organization and project-based user management

## Installation

Add the dependency to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.navchetna.aegis:aegis-auth-android:1.0.0'
}
```

## Quick Start

### 1. Initialize the SDK

```kotlin
import com.navchetna.aegis.AegisAuth

class MyApplication : Application() {
    lateinit var aegisAuth: AegisAuth
    
    override fun onCreate() {
        super.onCreate()
        
        aegisAuth = AegisAuth(
            apiKey = "your_api_key_here",
            baseUrl = "https://your-aegis-instance.com" // Optional
        )
    }
}
```

### 2. User Registration

```kotlin
class AuthActivity : AppCompatActivity() {
    private lateinit var aegisAuth: AegisAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        aegisAuth = (application as MyApplication).aegisAuth
        
        // Register new user
        lifecycleScope.launch {
            val result = aegisAuth.register(
                email = "user@example.com",
                password = "securePassword123",
                firstName = "John",
                lastName = "Doe"
            )
            
            when (result) {
                is AuthResult.Success -> {
                    // Registration successful
                    val user = result.user
                    navigateToMainActivity()
                }
                is AuthResult.Error -> {
                    // Handle error
                    showError(result.message)
                }
            }
        }
    }
}
```

### 3. User Login

```kotlin
// Email/Password login
lifecycleScope.launch {
    val result = aegisAuth.login("user@example.com", "password123")
    
    when (result) {
        is AuthResult.Success -> {
            // Login successful
            val user = result.user
            navigateToMainActivity()
        }
        is AuthResult.Error -> {
            // Handle login error
            showError(result.message)
        }
    }
}
```

### 4. WebAuthn/Passkey Authentication

```kotlin
// Initiate WebAuthn registration
lifecycleScope.launch {
    val challenge = aegisAuth.initiateWebAuthnRegistration()
    
    challenge?.let {
        // Use Android's credential manager or WebAuthn library
        // to create credential with the challenge
        val credential = createWebAuthnCredential(it)
        
        // Complete registration
        val success = aegisAuth.completeWebAuthnRegistration(credential)
        if (success) {
            showMessage("WebAuthn registration successful")
        }
    }
}
```

### 5. Multi-Factor Authentication (MFA)

```kotlin
// Enable MFA
lifecycleScope.launch {
    val mfaSetup = aegisAuth.enableMFA()
    
    mfaSetup?.let {
        // Show QR code for authenticator app
        showQRCode(it.qrCode)
        
        // Store backup codes securely
        storeBackupCodes(it.backupCodes)
    }
}

// Verify MFA token
lifecycleScope.launch {
    val isValid = aegisAuth.verifyMFA("123456")
    if (isValid) {
        showMessage("MFA verification successful")
    }
}
```

### 6. User Profile Management

```kotlin
// Get current user
lifecycleScope.launch {
    val user = aegisAuth.getCurrentUser()
    user?.let {
        displayUserProfile(it)
    }
}

// Check authentication status
if (aegisAuth.isAuthenticated()) {
    // User is logged in
    navigateToMainActivity()
} else {
    // Show login screen
    navigateToLoginActivity()
}
```

### 7. Logout

```kotlin
// Logout user
lifecycleScope.launch {
    val success = aegisAuth.logout()
    if (success) {
        navigateToLoginActivity()
    }
}
```

## Advanced Usage

### Custom Configuration

```kotlin
val aegisAuth = AegisAuth(
    apiKey = "your_api_key",
    baseUrl = "https://custom-domain.com"
)
```

### Error Handling

```kotlin
lifecycleScope.launch {
    try {
        val result = aegisAuth.login(email, password)
        when (result) {
            is AuthResult.Success -> handleSuccess(result.user)
            is AuthResult.Error -> handleError(result.message)
        }
    } catch (e: Exception) {
        handleNetworkError(e)
    }
}
```

## API Reference

### AegisAuth Class

#### Methods

- `suspend fun register(email: String, password: String, firstName: String?, lastName: String?): AuthResult`
- `suspend fun login(email: String, password: String): AuthResult`
- `suspend fun logout(): Boolean`
- `suspend fun getCurrentUser(): User?`
- `suspend fun initiateWebAuthnRegistration(): WebAuthnChallenge?`
- `suspend fun completeWebAuthnRegistration(credential: String): Boolean`
- `suspend fun enableMFA(): MFASetup?`
- `suspend fun verifyMFA(token: String): Boolean`
- `fun isAuthenticated(): Boolean`

### Data Classes

#### User
```kotlin
data class User(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val emailVerified: Boolean,
    val mfaEnabled: Boolean,
    val createdAt: String
)
```

#### AuthResult
```kotlin
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
```

## Requirements

- Android API level 21 (Android 5.0) or higher
- Kotlin 1.8.0 or higher
- Internet permission in AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Security Features

- 🔒 **TLS 1.3 Encryption** - All API communications encrypted
- 🎫 **JWT Tokens** - Secure token-based authentication
- 🔄 **Automatic Token Refresh** - Seamless session management
- 🛡️ **Certificate Pinning** - Protection against MITM attacks
- 📱 **Biometric Integration** - Hardware-backed security
- 🔐 **Secure Storage** - Android Keystore integration

## Support

- **Documentation**: https://aegis.navchetna.tech
- **GitHub Issues**: https://github.com/navchetnaofficialllp/aegis-auth-sdk-android/issues
- **Email Support**: hello@navchetna.tech

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## About Navchetna Technologies

Aegis Auth is developed and maintained by [Navchetna Technologies](https://navchetna.tech), a leading provider of identity and access management solutions.

---

**Aegis Auth by Navchetna Technologies** - Secure, Scalable, Simple Authentication for Android