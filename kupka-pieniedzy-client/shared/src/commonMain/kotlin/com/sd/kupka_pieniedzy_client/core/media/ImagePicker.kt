package com.sd.kupka_pieniedzy_client.core.media

import androidx.compose.runtime.Composable

/** Zdjęcie wybrane przez użytkownika z galerii telefonu. */
data class PickedImage(
    /** Opaque referencja do pliku: content-Uri (Android) lub identyfikator zasobu (iOS). */
    val path: String,
    /** Surowe bajty obrazu (JPEG) — pod realny upload do Supabase Storage / analizę. */
    val bytes: ByteArray,
) {
    // data class z ByteArray wymaga ręcznego equals/hashCode, by porównywać zawartość.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedImage) return false
        return path == other.path && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * path.hashCode() + bytes.contentHashCode()
}

/** Uchwyt natywnego pickera. Wywołaj [launch], aby otworzyć systemową galerię. */
class ImagePickerLauncher(val launch: () -> Unit)

/**
 * Tworzy i pamięta natywny picker zdjęć (systemowa galeria, tylko obrazy). [onResult] dostaje
 * wybrane zdjęcie albo `null`, gdy użytkownik anuluje. Implementacje platformowe:
 * Android — Photo Picker (`PickVisualMedia`, bez uprawnień), iOS — `PHPickerViewController`.
 */
@Composable expect fun rememberImagePicker(onResult: (PickedImage?) -> Unit): ImagePickerLauncher
