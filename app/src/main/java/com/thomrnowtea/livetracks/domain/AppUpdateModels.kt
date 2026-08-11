package com.thomrnowtea.livetracks.domain

data class AppReleaseMetadata(
    val schemaVersion: Int,
    val version: String,
    val versionCode: Long,
    val tag: String,
    val apkUrl: String,
    val sha256: String,
    val packageName: String,
    val releaseUrl: String,
    val prerelease: Boolean,
)

enum class UpdateFailure {
    NETWORK,
    RATE_LIMITED,
    NO_RELEASE,
    INVALID_METADATA,
    DOWNLOAD,
    CHECKSUM,
    INVALID_PACKAGE,
    SIGNATURE,
    INSTALLER_UNAVAILABLE,
    UNKNOWN,
}

sealed interface AppUpdateStatus {
    data object Idle : AppUpdateStatus
    data object Checking : AppUpdateStatus
    data class UpToDate(val installedVersion: String) : AppUpdateStatus
    data class Available(val release: AppReleaseMetadata) : AppUpdateStatus
    data class Downloading(
        val release: AppReleaseMetadata,
        val progress: Float?,
        val downloadedBytes: Long,
        val totalBytes: Long?,
    ) : AppUpdateStatus
    data class Verifying(val release: AppReleaseMetadata) : AppUpdateStatus
    data class ReadyToInstall(val release: AppReleaseMetadata) : AppUpdateStatus
    data class InstallPermissionRequired(val release: AppReleaseMetadata) : AppUpdateStatus
    data class InstallerOpened(val release: AppReleaseMetadata) : AppUpdateStatus
    data class Failed(
        val failure: UpdateFailure,
        val detail: String? = null,
        val release: AppReleaseMetadata? = null,
    ) : AppUpdateStatus
}
