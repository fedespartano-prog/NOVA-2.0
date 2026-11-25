package com.novatwo.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.github.csukuangfj.piper.phonemize.piperPhonemizeInitialize
import com.github.csukuangfj.piper.phonemize.piperPhonemizeText
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Paola Italian Piper voice. The model is downloaded only after the user
 * explicitly taps the install button; synthesis then runs entirely offline.
 */
class PaolaVoiceEngine(private val context: Context) {
    companion object {
        const val MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-it_IT-paola-medium-int8.tar.bz2"
        const val DOWNLOAD_BYTES = 21_143_212L
        private const val MODEL_DIR = "paola-voice"
    }

    private val modelDirectory = File(context.filesDir, MODEL_DIR)
    private val stopRequested = AtomicBoolean(false)
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var tts: OfflineTts? = null
    @Volatile private var phonemizerReady = false

    fun isInstalled(): Boolean {
        return modelDirectory.listFiles()?.any { it.extension == "onnx" } == true &&
            File(modelDirectory, "tokens.txt").isFile &&
            File(modelDirectory, "espeak-ng-data").isDirectory &&
            File(modelDirectory, "espeak-ng-data/it_dict").isFile
    }

    fun install(onProgress: (Int) -> Unit, onComplete: (Boolean, String) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            val archive = File(context.cacheDir, "paola-voice.tar.bz2")
            try {
                modelDirectory.mkdirs()
                connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                }
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("Download non riuscito: HTTP ${connection.responseCode}")
                }
                val total = connection.contentLengthLong.takeIf { it > 0 } ?: DOWNLOAD_BYTES
                connection.inputStream.use { input ->
                    FileOutputStream(archive).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            copied += count
                            onProgress(((copied * 100L) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }

                var foundModel = false
                var foundTokens = false
                TarArchiveInputStream(
                    BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive)))
                ).use { tar ->
                    while (true) {
                        val entry = tar.nextTarEntry ?: break
                        if (entry.isDirectory) continue
                        val archivePath = entry.name.replace('\\', '/')
                        val espeakMarker = "espeak-ng-data/"
                        val espeakIndex = archivePath.indexOf(espeakMarker)
                        val relativeEspeakPath = if (espeakIndex >= 0) {
                            archivePath.substring(espeakIndex + espeakMarker.length)
                        } else {
                            ""
                        }
                        val name = archivePath.substringAfterLast('/')
                        val isEspeakData = relativeEspeakPath.isNotEmpty()
                        if (name == "tokens.txt" || name.endsWith(".onnx") || isEspeakData) {
                            val output = if (isEspeakData) {
                                File(modelDirectory, "espeak-ng-data/$relativeEspeakPath")
                            } else {
                                File(modelDirectory, name)
                            }
                            if (entry.isDirectory) {
                                output.mkdirs()
                            } else {
                                output.parentFile?.mkdirs()
                                FileOutputStream(output).use { stream -> tar.copyTo(stream) }
                            }
                            if (name == "tokens.txt") foundTokens = true
                            if (name.endsWith(".onnx")) foundModel = true
                        }
                    }
                }
                if (!foundModel || !foundTokens) {
                    modelDirectory.deleteRecursively()
                    throw IllegalStateException("Il pacchetto della voce non contiene i file necessari")
                }
                archive.delete()
                onProgress(100)
                onComplete(true, "Voce Paola installata. Da ora funziona offline.")
            } catch (e: Exception) {
                archive.delete()
                onComplete(false, e.message ?: "Errore durante l'installazione della voce")
            } finally {
                connection?.disconnect()
            }
        }.apply { name = "NOVA-Paola-Install"; isDaemon = true; start() }
    }

    fun speak(
        text: String,
        onStarted: () -> Unit,
        onFinished: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        stopRequested.set(false)
        Thread {
            try {
                if (!isInstalled()) throw IllegalStateException("Installa prima la voce Paola")
                val engine = getOrCreateTts()
                val phonemes = piperPhonemizeText(text, engine.lang())
                    ?: throw IllegalStateException("Conversione fonetica non riuscita")
                val phonemeCodepoints = try {
                    Array(phonemes.numSentences) { index -> phonemes.getPhonemes(index) }
                } finally {
                    phonemes.close()
                }

                val sampleRate = engine.sampleRate()
                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT
                )
                if (minBuffer <= 0) throw IllegalStateException("Audio non supportato")
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuffer, sampleRate / 2 * 4))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    throw IllegalStateException("AudioTrack non inizializzato sul dispositivo")
                }
                audioTrack = track
                track.play()
                onStarted()

                val config = GenerationConfig(
                    sid = 0,
                    speed = 1.0f,
                    phonemeCodepoints = phonemeCodepoints
                )
                engine.generateWithConfigAndCallback("", config) { samples ->
                    if (stopRequested.get()) {
                        0
                    } else {
                        val written = track.write(
                            samples, 0, samples.size, AudioTrack.WRITE_BLOCKING
                        )
                        if (written < 0) 0 else 1
                    }
                }
                try { track.pause() } catch (_: Exception) {}
                try { track.flush() } catch (_: Exception) {}
                try { track.stop() } catch (_: Exception) {}
                track.release()
                audioTrack = null
                onFinished()
            } catch (e: LinkageError) {
                audioTrack?.let {
                    try { it.pause() } catch (_: Exception) {}
                    try { it.flush() } catch (_: Exception) {}
                    try { it.release() } catch (_: Exception) {}
                }
                audioTrack = null
                onError("Libreria nativa Sherpa/Piper non caricabile: " +
                    (e.message ?: e.javaClass.simpleName))
            } catch (e: Exception) {
                audioTrack?.let {
                    try { it.pause() } catch (_: Exception) {}
                    try { it.flush() } catch (_: Exception) {}
                    try { it.release() } catch (_: Exception) {}
                }
                audioTrack = null
                onError(e.message ?: "Errore della voce Paola")
            }
        }.apply { name = "NOVA-Paola-TTS"; isDaemon = true; start() }
    }

    private fun getOrCreateTts(): OfflineTts {
        tts?.let { return it }
        synchronized(this) {
            tts?.let { return it }
            if (!phonemizerReady) {
                piperPhonemizeInitialize(context.applicationContext)
                phonemizerReady = true
            }
            val modelFile = modelDirectory.listFiles()?.firstOrNull { it.extension == "onnx" }
                ?: throw IllegalStateException("File modello Paola mancante")
            val config = getOfflineTtsConfig(
                modelDir = modelDirectory.absolutePath,
                modelName = modelFile.name,
                dataDir = File(modelDirectory, "espeak-ng-data").absolutePath,
                numThreads = 1
            )
            return OfflineTts(assetManager = null, config = config).also { tts = it }
        }
    }

    fun stop() {
        stopRequested.set(true)
        audioTrack?.let {
            try { it.pause() } catch (_: Exception) {}
            try { it.flush() } catch (_: Exception) {}
        }
    }

    fun release() {
        stop()
        synchronized(this) {
            try { tts?.free() } catch (_: Exception) {}
            tts = null
        }
    }
}
