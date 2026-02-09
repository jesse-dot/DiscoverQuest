package com.discoverquest.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class DiscoveryAudioManager(private val context: Context) {

    companion object {
        private const val TAG = "DiscoveryAudio"
        private const val CUSTOM_SOUND_FILENAME = "custom_discovery_sound.mp3"
    }

    private val customSoundFile: File
        get() = File(context.filesDir, CUSTOM_SOUND_FILENAME)

    fun copyCustomSound(context: Context, uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                customSoundFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy custom sound", e)
        }
    }

    fun playDiscoverySound() {
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            if (customSoundFile.exists()) {
                player.setDataSource(customSoundFile.absolutePath)
            } else {
                Log.d(TAG, "No custom sound file configured")
                player.release()
                return
            }

            player.setOnCompletionListener { it.release() }
            player.prepare()
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play discovery sound", e)
        }
    }

    fun hasCustomSound(): Boolean = customSoundFile.exists()
}
