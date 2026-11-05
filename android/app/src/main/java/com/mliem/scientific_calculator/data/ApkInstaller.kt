package com.mliem.scientific_calculator.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.mliem.scientific_calculator.R
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val percent: Int) : InstallState
    data object Installing : InstallState
    data class Failed(val message: String) : InstallState
}

/** Downloads a release APK, checks it against the digest GitHub reports, and hands it to the system installer. */
class ApkInstaller(private val context: Context) {
    private val _state = MutableStateFlow<InstallState>(InstallState.Idle)
    val state: StateFlow<InstallState> = _state.asStateFlow()

    fun reset() {
        _state.value = InstallState.Idle
    }

    /** Called by [InstallResultReceiver] when the system installer refuses the APK. */
    fun onInstallFailed(message: String) {
        _state.value = InstallState.Failed(message)
    }

    suspend fun install(info: UpdateInfo) {
        val url = info.apkUrl
        if (url == null || !isTrustedUrl(url)) return fail(R.string.install_failed)
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return fail(R.string.install_allow)
        }
        _state.value = InstallState.Downloading(0)
        try {
            val apk = withContext(Dispatchers.IO) { download(url, info) }
            _state.value = InstallState.Installing
            withContext(Dispatchers.IO) { commit(apk) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ChecksumException) {
            fail(R.string.install_checksum)
        } catch (e: Exception) {
            fail(R.string.install_failed)
        }
    }

    private fun fail(message: Int) {
        _state.value = InstallState.Failed(context.getString(message))
    }

    private fun download(url: String, info: UpdateInfo): File {
        val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
        val target = File(dir, "update.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
            var done = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) _state.value = InstallState.Downloading((done * 100 / total).toInt())
                    }
                }
            }
            if (info.sizeBytes > 0 && done != info.sizeBytes) throw ChecksumException()
        } finally {
            connection.disconnect()
        }
        if (!checksumMatches(digest.digest().joinToString("") { "%02x".format(it) }, info.sha256)) {
            target.delete()
            throw ChecksumException()
        }
        return target
    }

    private fun commit(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("update.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            // The installer fills in the status extras, so this PendingIntent has to be mutable.
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
    }

    private class ChecksumException : Exception()

    companion object {
        /** Only https downloads are installed. */
        internal fun isTrustedUrl(url: String) = url.startsWith("https://")

        /** GitHub reports a digest for new assets; an older asset has none and is accepted on size alone. */
        internal fun checksumMatches(actualHex: String, expected: String?) =
            expected == null || actualHex.equals(expected, ignoreCase = true)
    }
}
