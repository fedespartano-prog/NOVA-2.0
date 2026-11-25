package com.novatwo.assistant

import android.content.Context

/** Java-friendly adapter for the offline Italian Paola voice engine. */
class PaolaVoiceBridge(context: Context) {
    interface InstallListener {
        fun onProgress(percent: Int)
        fun onComplete(success: Boolean, message: String)
    }

    interface SpeechListener {
        fun onStarted()
        fun onFinished()
        fun onError(message: String)
    }

    private val engine = PaolaVoiceEngine(context.applicationContext)

    fun isInstalled(): Boolean = engine.isInstalled()

    fun install(listener: InstallListener) {
        engine.install(
            { percent -> listener.onProgress(percent) },
            { success, message -> listener.onComplete(success, message) }
        )
    }

    fun speak(text: String, listener: SpeechListener) {
        engine.speak(
            text,
            { listener.onStarted() },
            { listener.onFinished() },
            { message -> listener.onError(message) }
        )
    }

    fun stop() = engine.stop()

    fun release() = engine.release()
}
