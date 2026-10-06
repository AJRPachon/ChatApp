package com.ajrpachon.chatapp.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AudioRecorderRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val repo = AudioRecorderRepositoryImpl(ApplicationProvider.getApplicationContext())

    @Test
    fun `the amplitude is zero when nothing is being recorded`() {
        assertEquals(0f, repo.getMaxAmplitude(), 0f)
    }

    @Test
    fun `stopping and releasing without a recording is safe`() {
        repo.stopRecording()
        repo.release()

        assertEquals(0f, repo.getMaxAmplitude(), 0f)
    }

    @Test
    fun `startRecording succeeds with a writable path`() {
        val result = repo.startRecording(tempFolder.newFile("voice.m4a").absolutePath)

        assertTrue(result.isSuccess)
        repo.release()
    }

    @Test
    fun `the amplitude stays within zero and one while recording`() {
        repo.startRecording(tempFolder.newFile("voice.m4a").absolutePath)

        assertTrue(repo.getMaxAmplitude() in 0f..1f)
        repo.release()
    }

    @Test
    fun `a recording can be started again after it was stopped`() {
        repo.startRecording(tempFolder.newFile("first.m4a").absolutePath)
        repo.stopRecording()

        val second = repo.startRecording(tempFolder.newFile("second.m4a").absolutePath)

        assertTrue(second.isSuccess)
        repo.release()
    }
}
