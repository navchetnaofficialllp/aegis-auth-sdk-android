package com.navchetna.aegis

import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Aegis Auth Android SDK
 * 
 * A comprehensive authentication SDK for Android applications providing:
 * - Email/Password authentication
 * - WebAuthn/Passkey support
 * - Multi-factor authentication (MFA)
 * - Biometric authentication
 * - OAuth/SSO integration
 * - JWT token management with automatic refresh
 */
class AegisAuth(
    private val apiKey: String,
    private val baseUrl: String = "https://05card1j5b.execute-api.ap-south-1.amazonaws.com/prod"
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val api: AegisApi
    
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenExpiresAt: Long = 0
    
    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-API-Key", apiKey)
                    .addHeader("User-Agent", "AegisAuth-Android/1.0.0")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(AegisApi::class.java)
    }
    
    /**
     * Register a new user with email and password
     */
    suspend fun register(email: String, password: String, firstName: String? = null, lastName: String? = null): AuthResult {
        return try {
            val request = RegisterRequest(email, password, firstName, lastName)
            val response = api.register(request)
            
            if (response.success) {
                storeTokens(response.data.accessToken, response.data.refreshToken, response.data.expiresIn)
                AuthResult.Success(response.data.user)
            } else {
                AuthResult.Error(response.message ?: "Registration failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val request = LoginRequest(email, password)
            val response = api.login(request)
            
            if (response.success) {
                storeTokens(response.data.accessToken, response.data.refreshToken, response.data.expiresIn)
                AuthResult.Success(response.data.user)
            } else {
                AuthResult.Error(response.message ?: "Login failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Logout current user
     */
    suspend fun logout(): Boolean {
        return try {
            if (accessToken != null) {
                api.logout("Bearer $accessToken")
            }
            clearTokens()
            true
        } catch (e: Exception) {
            clearTokens()
            false
        }
    }
    
    /**
     * Get current user profile
     */
    suspend fun getCurrentUser(): User? {
        return try {
            ensureValidToken()
            val response = api.getProfile("Bearer $accessToken")
            if (response.success) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Initiate WebAuthn registration
     */
    suspend fun initiateWebAuthnRegistration(): WebAuthnChallenge? {
        return try {
            ensureValidToken()
            val response = api.initiateWebAuthnRegistration("Bearer $accessToken")
            if (response.success) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Complete WebAuthn registration
     */
    suspend fun completeWebAuthnRegistration(credential: String): Boolean {
        return try {
            ensureValidToken()
            val request = WebAuthnCredentialRequest(credential)
            val response = api.completeWebAuthnRegistration("Bearer $accessToken", request)
            response.success
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Enable MFA for current user
     */
    suspend fun enableMFA(): MFASetup? {
        return try {
            ensureValidToken()
            val response = api.enableMFA("Bearer $accessToken")
            if (response.success) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Verify MFA token
     */
    suspend fun verifyMFA(token: String): Boolean {
        return try {
            ensureValidToken()
            val request = MFAVerifyRequest(token)
            val response = api.verifyMFA("Bearer $accessToken", request)
            response.success
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return accessToken != null && System.currentTimeMillis() < tokenExpiresAt
    }
    
    private suspend fun ensureValidToken() {
        if (System.currentTimeMillis() >= tokenExpiresAt && refreshToken != null) {
            refreshAccessToken()
        }
    }
    
    private suspend fun refreshAccessToken() {
        try {
            val request = RefreshTokenRequest(refreshToken!!)
            val response = api.refreshToken(request)
            
            if (response.success) {
                storeTokens(response.data.accessToken, response.data.refreshToken, response.data.expiresIn)
            } else {
                clearTokens()
            }
        } catch (e: Exception) {
            clearTokens()
        }
    }
    
    private fun storeTokens(access: String, refresh: String, expiresIn: Long) {
        accessToken = access
        refreshToken = refresh
        tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)
    }
    
    private fun clearTokens() {
        accessToken = null
        refreshToken = null
        tokenExpiresAt = 0
    }
}

// Data classes
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class WebAuthnCredentialRequest(
    val credential: String
)

data class MFAVerifyRequest(
    val token: String
)

data class User(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val emailVerified: Boolean,
    val mfaEnabled: Boolean,
    val createdAt: String
)

data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String,
    val user: User
)

data class WebAuthnChallenge(
    val challenge: String,
    val timeout: Long,
    val rpId: String,
    val allowCredentials: List<String>
)

data class MFASetup(
    val secret: String,
    val qrCode: String,
    val backupCodes: List<String>
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T,
    val timestamp: String
)

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

// Retrofit API interface
interface AegisApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthData>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthData>
    
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): ApiResponse<Unit>
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<AuthData>
    
    @GET("auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): ApiResponse<User>
    
    @POST("auth/webauthn/register/initiate")
    suspend fun initiateWebAuthnRegistration(@Header("Authorization") token: String): ApiResponse<WebAuthnChallenge>
    
    @POST("auth/webauthn/register/complete")
    suspend fun completeWebAuthnRegistration(
        @Header("Authorization") token: String,
        @Body request: WebAuthnCredentialRequest
    ): ApiResponse<Unit>
    
    @POST("auth/mfa/enable")
    suspend fun enableMFA(@Header("Authorization") token: String): ApiResponse<MFASetup>
    
    @POST("auth/mfa/verify")
    suspend fun verifyMFA(
        @Header("Authorization") token: String,
        @Body request: MFAVerifyRequest
    ): ApiResponse<Unit>
}