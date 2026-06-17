package com.sd.kupka_pieniedzy_client.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
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
        val typeId = provider.registeredTypeIdentifiers.firstOrNull() as? String ?: "public.image"
        provider.loadDataRepresentationForTypeIdentifier(typeId) { data, _ ->
            val picked = data?.let { PickedImage(path = identifier, bytes = it.toByteArray()) }
            // completionHandler woła z prywatnej kolejki — wracamy na main do zmiany stanu Compose.
            dispatch_async(dispatch_get_main_queue()) { onResult(picked) }
        }
    }
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
