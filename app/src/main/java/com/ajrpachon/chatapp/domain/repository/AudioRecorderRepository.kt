package com.ajrpachon.chatapp.domain.repository

interface AudioRecorderRepository {
    /** Start recording to [outputFilePath]. Returns failure if MediaRecorder setup fails. */
    fun startRecording(outputFilePath: String): Result<Unit>
    /** Stop recording. Safe to call if no recording is active. */
    fun stopRecording()
    /** Returns current max amplitude normalised to [0, 1]. */
    fun getMaxAmplitude(): Float
    /** Stop and release any active recorder resources. */
    fun release()
}
