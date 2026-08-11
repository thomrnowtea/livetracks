package com.thomrnowtea.livetracks.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.thomrnowtea.livetracks.domain.AppReleaseMetadata
import com.thomrnowtea.livetracks.domain.UpdateFailure
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class UpdateDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?,
) {
    val fraction: Float? = totalBytes?.takeIf { it > 0 }?.let {
        (downloadedBytes.toDouble() / it).toFloat().coerceIn(0f, 1f)
    }
}

class UpdateInstallException(
    val failure: UpdateFailure,
    message: String,
) : Exception(message)

class AppUpdateInstaller(private val context: Context) {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)
    private val downloadState = context.getSharedPreferences(DOWNLOAD_STATE, Context.MODE_PRIVATE)
    private var verifiedApk: File? = null
    private var verifiedReleaseCode: Long? = null

    suspend fun download(
        release: AppReleaseMetadata,
        progress: (UpdateDownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        verifiedApk = null
        verifiedReleaseCode = null
        val externalDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw UpdateInstallException(UpdateFailure.DOWNLOAD, "App download directory is unavailable")
        val updatesDirectory = File(externalDownloads, UPDATE_DIRECTORY).apply { mkdirs() }
        val destination = File(updatesDirectory, "LiveTracks-${release.versionCode}.apk")
        val resumedDownloadId = resumableDownloadId(release)
        if (resumedDownloadId == null && destination.isFile && destination.length() in 1..MAX_APK_BYTES) {
            return@withContext destination
        }
        val downloadId = resumedDownloadId ?: startDownload(release, destination, updatesDirectory)
        try {
            while (true) {
                val snapshot = query(downloadId)
                if (snapshot.totalBytes != null && snapshot.totalBytes > MAX_APK_BYTES) {
                    downloadManager.remove(downloadId)
                    throw UpdateInstallException(UpdateFailure.DOWNLOAD, "APK exceeds the safe size limit")
                }
                progress(UpdateDownloadProgress(snapshot.downloadedBytes, snapshot.totalBytes))
                when (snapshot.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        clearDownloadState()
                        break
                    }
                    DownloadManager.STATUS_FAILED -> throw UpdateInstallException(
                        UpdateFailure.DOWNLOAD,
                        "DownloadManager failure ${snapshot.reason}",
                    )
                }
                delay(POLL_INTERVAL_MS)
            }
        } catch (cancelled: CancellationException) {
            downloadManager.remove(downloadId)
            clearDownloadState()
            throw cancelled
        } catch (failure: UpdateInstallException) {
            downloadManager.remove(downloadId)
            clearDownloadState()
            destination.delete()
            throw failure
        }
        if (!destination.isFile || destination.length() <= 0 || destination.length() > MAX_APK_BYTES) {
            destination.delete()
            throw UpdateInstallException(UpdateFailure.DOWNLOAD, "Downloaded APK is missing or invalid")
        }
        destination
    }

    private fun resumableDownloadId(release: AppReleaseMetadata): Long? {
        val id = downloadState.getLong(KEY_DOWNLOAD_ID, NO_DOWNLOAD)
        val code = downloadState.getLong(KEY_RELEASE_CODE, -1)
        val tag = downloadState.getString(KEY_RELEASE_TAG, null)
        if (id == NO_DOWNLOAD) return null
        if (code == release.versionCode && tag == release.tag) {
            val status = runCatching { query(id).status }.getOrNull()
            if (status in setOf(
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED,
                    DownloadManager.STATUS_SUCCESSFUL,
                )
            ) return id
        }
        downloadManager.remove(id)
        clearDownloadState()
        return null
    }

    private fun startDownload(release: AppReleaseMetadata, destination: File, updatesDirectory: File): Long {
        if (destination.exists() && !destination.delete()) {
            throw UpdateInstallException(UpdateFailure.DOWNLOAD, "Previous update file could not be replaced")
        }
        updatesDirectory.listFiles()?.filter { it != destination }?.forEach { old ->
            if (old.isFile && old.name.startsWith("LiveTracks-") && old.name.endsWith(".apk")) old.delete()
        }
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("LiveTracks ${release.version}")
            .setDescription("Downloading verified application update")
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "$UPDATE_DIRECTORY/${destination.name}",
            )
        val id = runCatching { downloadManager.enqueue(request) }.getOrElse {
            throw UpdateInstallException(UpdateFailure.DOWNLOAD, it.message ?: "Download could not start")
        }
        downloadState.edit()
            .putLong(KEY_DOWNLOAD_ID, id)
            .putLong(KEY_RELEASE_CODE, release.versionCode)
            .putString(KEY_RELEASE_TAG, release.tag)
            .apply()
        return id
    }

    private fun clearDownloadState() {
        downloadState.edit().clear().apply()
    }

    suspend fun validate(file: File, release: AppReleaseMetadata) = withContext(Dispatchers.IO) {
        try {
            val actualDigest = sha256(file)
            if (!MessageDigest.isEqual(actualDigest.hexToBytes(), release.sha256.hexToBytes())) {
                throw UpdateInstallException(UpdateFailure.CHECKSUM, "SHA-256 does not match release metadata")
            }
            val archive = archivePackageInfo(file)
                ?: throw UpdateInstallException(UpdateFailure.INVALID_PACKAGE, "Downloaded file is not an APK")
            if (archive.packageName != context.packageName || archive.packageName != release.packageName) {
                throw UpdateInstallException(UpdateFailure.INVALID_PACKAGE, "APK package name does not match LiveTracks")
            }
            if (archive.longVersionCodeCompat() != release.versionCode) {
                throw UpdateInstallException(UpdateFailure.INVALID_PACKAGE, "APK version code does not match metadata")
            }
            if (archive.versionName != release.version) {
                throw UpdateInstallException(UpdateFailure.INVALID_PACKAGE, "APK version name does not match metadata")
            }
            if (!signerMatchesInstalledApp(archive)) {
                throw UpdateInstallException(UpdateFailure.SIGNATURE, "APK signing certificate does not match the installed app")
            }
            verifiedApk = file
            verifiedReleaseCode = release.versionCode
        } catch (failure: UpdateInstallException) {
            file.delete()
            verifiedApk = null
            verifiedReleaseCode = null
            throw failure
        }
    }

    fun canRequestInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.getOrElse {
            throw UpdateInstallException(UpdateFailure.INSTALLER_UNAVAILABLE, it.message ?: "Install settings are unavailable")
        }
    }

    @Suppress("DEPRECATION")
    fun openInstaller(release: AppReleaseMetadata) {
        val file = verifiedApk?.takeIf { verifiedReleaseCode == release.versionCode && it.isFile }
            ?: throw UpdateInstallException(UpdateFailure.INVALID_PACKAGE, "No verified APK is ready")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false)
            putExtra(Intent.EXTRA_RETURN_RESULT, false)
        }
        runCatching { context.startActivity(intent) }.getOrElse {
            throw UpdateInstallException(UpdateFailure.INSTALLER_UNAVAILABLE, it.message ?: "Package installer is unavailable")
        }
    }

    private fun query(downloadId: Long): DownloadSnapshot {
        val query = DownloadManager.Query().setFilterById(downloadId)
        return downloadManager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) throw UpdateInstallException(UpdateFailure.DOWNLOAD, "Download disappeared")
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                .coerceAtLeast(0)
            val totalRaw = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            DownloadSnapshot(status, downloaded, totalRaw.takeIf { it > 0 }, reason)
        } ?: throw UpdateInstallException(UpdateFailure.DOWNLOAD, "DownloadManager query failed")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun archivePackageInfo(file: File): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfo(): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return context.packageManager.getPackageInfo(context.packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun signerMatchesInstalledApp(archive: PackageInfo): Boolean {
        val installed = installedPackageInfo()
        val installedCertificates = installed.certificateBytes()
        val archiveCertificates = archive.certificateBytes()
        return installedCertificates.isNotEmpty() && archiveCertificates.isNotEmpty() &&
            archiveCertificates.any { candidate ->
                installedCertificates.any { installedCertificate ->
                    MessageDigest.isEqual(candidate, installedCertificate)
                }
            }
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.certificateBytes(): List<ByteArray> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = signingInfo ?: return emptyList()
            val signatures = if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
            signatures?.map { it.toByteArray() }.orEmpty()
        } else {
            signatures?.map { it.toByteArray() }.orEmpty()
        }

    @Suppress("DEPRECATION")
    private fun PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    private fun String.hexToBytes(): ByteArray {
        if (length % 2 != 0) return byteArrayOf()
        return ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    }

    private data class DownloadSnapshot(
        val status: Int,
        val downloadedBytes: Long,
        val totalBytes: Long?,
        val reason: Int,
    )

    companion object {
        private const val DOWNLOAD_STATE = "app_update_download"
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_RELEASE_CODE = "release_code"
        private const val KEY_RELEASE_TAG = "release_tag"
        private const val NO_DOWNLOAD = -1L
        private const val UPDATE_DIRECTORY = "updates"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val POLL_INTERVAL_MS = 300L
        private const val MAX_APK_BYTES = 512L * 1024L * 1024L
    }
}
