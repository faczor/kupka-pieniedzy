package com.sd.kupka_pieniedzy_client.core.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Android: systemowy Photo Picker (`PickVisualMedia`). Nie wymaga uprawnień do galerii — system
 * sam udostępnia wybrane zdjęcie. Bajty czytamy synchronicznie z `contentResolver` (paragony to
 * małe pliki; w MVP akceptowalne na wątku UI).
 */
@Composable
actual fun rememberImagePicker(onResult: (PickedImage?) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                currentOnResult.value(null)
                return@rememberLauncherForActivityResult
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            currentOnResult.value(bytes?.let { PickedImage(path = uri.toString(), bytes = it) })
        }

    return remember {
        ImagePickerLauncher {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}
