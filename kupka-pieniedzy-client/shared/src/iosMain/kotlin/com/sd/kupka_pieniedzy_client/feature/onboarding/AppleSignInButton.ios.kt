package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.runtime.Composable
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.designsystem.component.SocialButton
import com.sd.kupka_pieniedzy_client.designsystem.icon.BrandLogos
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithApple
import org.koin.compose.koinInject

@Composable
actual fun AppleSignInButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onStarted: () -> Unit,
    onSignedIn: () -> Unit,
    onError: (DomainError) -> Unit,
) {
    val supabase = koinInject<SupabaseClientProvider>()
    val action =
        supabase.composeAuth.rememberSignInWithApple(
            onResult = { result ->
                when (result) {
                    is NativeSignInResult.Success -> onSignedIn()
                    is NativeSignInResult.ClosedByUser -> onError(DomainError.AuthCancelled)
                    is NativeSignInResult.Error -> onError(DomainError.Unknown(result.message))
                    is NativeSignInResult.NetworkError -> onError(DomainError.Network)
                }
            }
        )
    SocialButton(
        text = text,
        logo = BrandLogos.Apple,
        onClick = {
            onStarted()
            action.startFlow()
        },
        loading = loading,
        enabled = enabled,
    )
}

actual val isAppleSignInAvailable: Boolean = true
