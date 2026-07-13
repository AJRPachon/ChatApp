package com.ajrpachon.chatapp.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.ajrpachon.chatapp.domain.repository.AudioRecorderRepository

class AudioRecorderRepositoryImpl(private val context: Context) : AudioRecorderRepository {

    private var recorder: MediaRecorder? = null

    override fun startRecording(outputFilePath: String): Result<Unit> = runCatching {
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFilePath)
            prepare()
            start()
        }
        recorder = rec
    }

    override fun stopRecording() {
        runCatching { recorder?.apply { stop(); release() } }
        recorder = null
    }

    override fun getMaxAmplitude(): Float =
        runCatching { (recorder?.maxAmplitude ?: 0).toFloat() / 32767f }.getOrDefault(0f)

    override fun release() {
        stopRecording()
    }
}
