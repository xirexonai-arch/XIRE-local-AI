package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AttachmentReader
import com.example.data.local.ChatEntity
import com.example.data.local.MessageEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.XireDatabase
import com.example.data.model.evaluateModelCapabilities
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("XIRE AI", appName)
    }

    @Test
    fun `fresh install has no pre-stored data and saves only locally on user input`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesRepository(context)
        val initialProfile = prefs.userProfile.first()
        val initialSettings = prefs.appSettings.first()

        // Fresh install must never contain any pre-populated or hardcoded username/age
        assertEquals("", initialProfile.name)
        assertEquals(0, initialProfile.age)
        assertFalse(initialProfile.isOnboarded)
        assertEquals(512, initialSettings.maxTokens)

        // Data is stored locally only after the user explicitly completes onboarding
        prefs.saveProfile("TestUser", 24)
        val saved = prefs.userProfile.first()
        assertEquals("TestUser", saved.name)
        assertEquals(24, saved.age)
        assertTrue(saved.isOnboarded)

        val db = XireDatabase.getInstance(context)
        assertTrue(db.chatDao().observeChats().first().isEmpty())
        assertTrue(db.localModelDao().observeModels().first().isEmpty())
    }

    @Test
    fun `model capabilities accurately gate file view and image recognition`() {
        val noModel = evaluateModelCapabilities("")
        assertFalse(noModel.hasSelectedModel)
        assertFalse(noModel.supportsFileView)
        assertFalse(noModel.supportsImageRecognition)

        val textModel = evaluateModelCapabilities("Qwen2.5-0.5B-Instruct-Q4_K_M.gguf")
        assertTrue(textModel.hasSelectedModel)
        assertTrue(textModel.supportsFileView)
        assertFalse(textModel.supportsImageRecognition)
    }

    @Test
    fun `attachment reader accepts readable text files and rejects images and binary files`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reader = AttachmentReader(context)

        val textFile = File(context.cacheDir, "notes.md").apply {
            writeText("# Summary\nLine 1\nLine 2")
        }
        val textResult = reader.readTextAttachment(Uri.fromFile(textFile))
        assertTrue(textResult.isSuccess)
        val attached = textResult.getOrThrow()
        assertEquals("notes.md", attached.name)
        assertEquals(3, attached.lineCount)
        assertTrue(attached.content.contains("# Summary"))

        val imageFile = File(context.cacheDir, "photo.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        }
        val imageResult = reader.readTextAttachment(Uri.fromFile(imageFile))
        assertTrue(imageResult.isFailure)

        val binFile = File(context.cacheDir, "archive.dat").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x00, 0x7F))
        }
        val binResult = reader.readTextAttachment(Uri.fromFile(binFile))
        assertTrue(binResult.isFailure)

        textFile.delete()
        imageFile.delete()
        binFile.delete()
    }

    @Test
    fun `database persists message file attachment fields`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = XireDatabase.getInstance(context)
        val chatId = db.chatDao().insertChat(ChatEntity(title = "File Test"))
        db.messageDao().insertMessage(
            MessageEntity(
                chatId = chatId,
                role = "user",
                content = "Explain this snippet",
                attachmentName = "Sample.kt",
                attachmentSizeBytes = 128L,
                attachmentContent = "fun main() = println(\"Hello\")"
            )
        )
        val messages = db.messageDao().getMessagesOnce(chatId)
        assertEquals(1, messages.size)
        assertEquals("Sample.kt", messages[0].attachmentName)
        assertEquals(128L, messages[0].attachmentSizeBytes)
        assertTrue(messages[0].attachmentContent?.contains("println") == true)
    }
}
