package com.ajrpachon.chatapp.ui.call

import androidx.compose.ui.graphics.ColorMatrix

enum class CameraFilter(val displayName: String) {
    NONE("Original") {
        override val colorMatrix: ColorMatrix get() = ColorMatrix() // identity
    },
    GRAYSCALE("Blanco y negro") {
        override val colorMatrix: ColorMatrix
            get() = ColorMatrix(
                floatArrayOf(
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0f,    0f,    0f,    1f, 0f,
                )
            )
    },
    SEPIA("Sepia") {
        override val colorMatrix: ColorMatrix
            get() = ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f,     0f,     0f,     1f, 0f,
                )
            )
    },
    HIGH_CONTRAST("Contraste alto") {
        override val colorMatrix: ColorMatrix
            get() = ColorMatrix(
                floatArrayOf(
                    2f, 0f, 0f, 0f, -128f,
                    0f, 2f, 0f, 0f, -128f,
                    0f, 0f, 2f, 0f, -128f,
                    0f, 0f, 0f, 1f,    0f,
                )
            )
    },
    WARM("Calidez") {
        override val colorMatrix: ColorMatrix
            get() = ColorMatrix(
                floatArrayOf(
                    1.2f, 0f,   0f,   0f, 10f,
                    0f,   1.0f, 0f,   0f,  5f,
                    0f,   0f,   0.8f, 0f,  0f,
                    0f,   0f,   0f,   1f,  0f,
                )
            )
    };

    abstract val colorMatrix: ColorMatrix
}
