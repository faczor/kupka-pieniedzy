package com.sd.kupka_pieniedzy_client.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * iOS: `PHPickerViewController` z filtrem obrazów (1 zdjęcie). Picker prezentujemy z aktualnego
 * top-most view controllera. Delegata trzymamy w polu modułowym, by nie został zebrany przez GC
 * zanim user dokona wyboru.
 */
@Composable
actual fun rememberImagePicker(onResult: (PickedImage?) -> Unit): ImagePickerLauncher {
    val currentOnResult = rememberUpdatedState(onResult)
    return remember { ImagePickerLauncher { presentPicker { currentOnResult.value(it) } } }
}

// Silna referencja na czas trwania pickera (PHPicker trzyma delegata jako `weak`).
private var retainedDelegate: PHPickerViewControllerDelegateProtocol? = null

private fun presentPicker(onResult: (PickedImage?) -> Unit) {
    val config =
        PHPickerConfiguration().apply {
            selectionLimit = 1
            filter = PHPickerFilter.imagesFilter()
        }
    val picker = PHPickerViewController(configuration = config)
    val delegate =
        PickerDelegate { picked ->
            retainedDelegate = null
            onResult(picked)
        }
    retainedDelegate = delegate
    picker.delegate = delegate
    topViewController()?.presentViewController(picker, animated = true, completion = null)
}

private class PickerDelegate(private val onResult: (PickedImage?) -> Unit) :
    NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            onResult(null)
            return
        }

        val provider = result.itemProvider
        val identifier = result.assetIdentifier ?: "ios-photo"
        val typeId = provider.preferredStillImageTypeIdentifier()
        provider.loadDataRepresentationForTypeIdentifier(typeId) { data, _ ->
            // Na tej (prywatnej) kolejce transkodujemy HEIC/duże zdjęcia -> JPEG ~1568 px,
            // bo Claude vision nie przyjmuje HEIC. Dopiero wynik wracamy na main.
            val bytes = data?.toByteArray()?.let { normalizeReceiptImageToJpeg(it) }
            val picked = bytes?.let { PickedImage(path = identifier, bytes = it) }
            dispatch_async(dispatch_get_main_queue()) { onResult(picked) }
        }
    }
}

/**
 * Wybiera identyfikator typu **statycznej klatki** zdjęcia do wczytania.
 *
 * Dla Live Photo `registeredTypeIdentifiers` zawiera też bundle (zdjęcie + film), który na początku
 * listy potrafi wygrać z `firstOrNull()`. Wczytanie bundla daje dane, których `UIImage` nie dekoduje,
 * a normalizer w fallbacku wysyła oryginał — i Claude vision dostaje nie-obraz. Dlatego pomijamy
 * bundle Live Photo i preferujemy znane formaty statyczne (JPEG/HEIC/PNG), które normalizer
 * przekoduje do JPEG.
 */
private fun NSItemProvider.preferredStillImageTypeIdentifier(): String {
    val ids = registeredTypeIdentifiers.filterIsInstance<String>()
    val still = ids.filterNot { it.contains("live-photo", ignoreCase = true) }
    return still.firstOrNull { it == "public.jpeg" }
        ?: still.firstOrNull { it == "public.heic" || it == "public.heif" }
        ?: still.firstOrNull { it == "public.png" }
        ?: still.firstOrNull()
        ?: "public.image"
}

private fun topViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    val window = app.keyWindow ?: app.windows.firstOrNull() as? UIWindow
    var top = window?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), bytes, length) } }
}
