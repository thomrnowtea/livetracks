package com.thomrnowtea.livetracks.data

import com.thomrnowtea.livetracks.domain.AppReleaseMetadata
import com.thomrnowtea.livetracks.domain.UpdateFailure
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface AppReleaseRepository {
    suspend fun latest(includePrereleases: Boolean): AppReleaseMetadata?
}

class UpdateRepositoryException(
    val failure: UpdateFailure,
    message: String,
) : Exception(message)

class GitHubAppReleaseRepository(
    private val userAgent: String,
) : AppReleaseRepository {
    override suspend fun latest(includePrereleases: Boolean): AppReleaseMetadata? = withContext(Dispatchers.IO) {
        val releases = JSONArray(readTrustedText(RELEASES_API, MAX_RELEASES_BYTES))
        for (index in 0 until minOf(releases.length(), MAX_RELEASES_TO_INSPECT)) {
            val release = releases.getJSONObject(index)
            if (release.optBoolean("draft", true)) continue
            val prerelease = release.optBoolean("prerelease", false)
            if (prerelease && !includePrereleases) continue
            val tag = release.optString("tag_name").trim()
            val releaseUrl = release.optString("html_url").trim()
            requireTrustedGitHubUrl(releaseUrl)
            val metadataAssetUrl = release.optJSONArray("assets")
                ?.objects()
                ?.firstOrNull { it.optString("name") == METADATA_ASSET }
                ?.optString("browser_download_url")
                ?.trim()
                ?: throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "$tag has no $METADATA_ASSET asset")
            val metadata = parseMetadata(
                text = readTrustedText(metadataAssetUrl, MAX_METADATA_BYTES),
                expectedTag = tag,
                releaseUrl = releaseUrl,
                prerelease = prerelease,
            )
            return@withContext metadata
        }
        null
    }

    private fun parseMetadata(
        text: String,
        expectedTag: String,
        releaseUrl: String,
        prerelease: Boolean,
    ): AppReleaseMetadata {
        val json = runCatching { JSONObject(text) }.getOrElse {
            throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Invalid release metadata JSON")
        }
        val schemaVersion = json.optInt("schemaVersion", -1)
        val version = json.optString("version").trim()
        val versionCode = json.optLong("versionCode", -1)
        val tag = json.optString("tag").trim()
        val apkUrl = json.optString("apk").trim()
        val sha256 = json.optString("sha256").trim().lowercase()
        val packageName = json.optString("packageName").trim()
        if (schemaVersion != SUPPORTED_SCHEMA || version.isBlank() || versionCode <= 0 || tag != expectedTag ||
            packageName != EXPECTED_PACKAGE || !SHA_256.matches(sha256)
        ) {
            throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Release metadata fields are invalid")
        }
        requireTrustedGitHubUrl(apkUrl, expectedTag)
        return AppReleaseMetadata(
            schemaVersion = schemaVersion,
            version = version,
            versionCode = versionCode,
            tag = tag,
            apkUrl = apkUrl,
            sha256 = sha256,
            packageName = packageName,
            releaseUrl = releaseUrl,
            prerelease = prerelease,
        )
    }

    private fun readTrustedText(url: String, maxBytes: Int): String {
        var current = URL(url)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            requireTrustedGitHubUrl(current.toString())
            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json, application/json")
                setRequestProperty("User-Agent", userAgent)
            }
            try {
                val response = connection.responseCode
                if (response in 300..399) {
                    if (redirectCount >= MAX_REDIRECTS) {
                        throw UpdateRepositoryException(UpdateFailure.NETWORK, "Too many redirects")
                    }
                    val location = connection.getHeaderField("Location")
                        ?: throw UpdateRepositoryException(UpdateFailure.NETWORK, "Redirect without location")
                    current = URI(current.toString()).resolve(location).toURL()
                    return@repeat
                }
                if (response == 403 && connection.getHeaderField("X-RateLimit-Remaining") == "0") {
                    throw UpdateRepositoryException(UpdateFailure.RATE_LIMITED, "GitHub API rate limit reached")
                }
                if (response !in 200..299) {
                    throw UpdateRepositoryException(UpdateFailure.NETWORK, "HTTP $response")
                }
                val declaredLength = connection.contentLengthLong
                if (declaredLength > maxBytes) {
                    throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Response is too large")
                }
                return connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > maxBytes) {
                            throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Response is too large")
                        }
                        output.write(buffer, 0, count)
                    }
                    output.toString(Charsets.UTF_8.name())
                }
            } finally {
                connection.disconnect()
            }
        }
        throw UpdateRepositoryException(UpdateFailure.NETWORK, "Redirect resolution failed")
    }

    private fun requireTrustedGitHubUrl(value: String, expectedTag: String? = null) {
        val uri = runCatching { URI(value) }.getOrNull()
            ?: throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Invalid URL")
        if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443)) {
            throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Untrusted URL")
        }
        val host = uri.host?.lowercase()
            ?: throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "URL has no host")
        if (host !in TRUSTED_HOSTS) {
            throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Untrusted host")
        }
        if (host == "api.github.com" && !uri.path.startsWith("/repos/$REPOSITORY/")) {
            throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Unexpected API path")
        }
        if (host == "github.com") {
            val prefix = "/$REPOSITORY/releases/"
            if (!uri.path.startsWith(prefix)) {
                throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "Unexpected release path")
            }
            if (expectedTag != null && !uri.path.startsWith("${prefix}download/$expectedTag/")) {
                throw UpdateRepositoryException(UpdateFailure.INVALID_METADATA, "APK tag does not match metadata")
            }
        }
    }

    private fun JSONArray.objects(): Sequence<JSONObject> = sequence {
        for (index in 0 until length()) optJSONObject(index)?.let { yield(it) }
    }

    companion object {
        const val EXPECTED_PACKAGE = "com.thomrnowtea.livetracks"
        const val SUPPORTED_SCHEMA = 1
        private const val REPOSITORY = "thomrnowtea/livetracks"
        private const val RELEASES_API = "https://api.github.com/repos/$REPOSITORY/releases?per_page=20"
        private const val METADATA_ASSET = "release.json"
        private const val MAX_RELEASES_TO_INSPECT = 20
        private const val MAX_RELEASES_BYTES = 1024 * 1024
        private const val MAX_METADATA_BYTES = 64 * 1024
        private const val MAX_REDIRECTS = 5
        private const val CONNECT_TIMEOUT_MS = 12_000
        private const val READ_TIMEOUT_MS = 20_000
        private val SHA_256 = Regex("^[0-9a-f]{64}$")
        private val TRUSTED_HOSTS = setOf(
            "api.github.com",
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "github-releases.githubusercontent.com",
        )
    }
}

fun isNewerRelease(installedVersionCode: Long, release: AppReleaseMetadata): Boolean =
    release.versionCode > installedVersionCode
