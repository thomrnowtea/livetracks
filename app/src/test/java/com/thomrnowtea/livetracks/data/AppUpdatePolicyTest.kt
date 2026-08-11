package com.thomrnowtea.livetracks.data

import com.thomrnowtea.livetracks.domain.AppReleaseMetadata
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatePolicyTest {
    @Test
    fun `release with a higher version code is offered`() {
        assertTrue(isNewerRelease(41, release(versionCode = 42)))
    }

    @Test
    fun `same or older version code is never offered`() {
        assertFalse(isNewerRelease(42, release(versionCode = 42)))
        assertFalse(isNewerRelease(43, release(versionCode = 42)))
    }

    private fun release(versionCode: Long) = AppReleaseMetadata(
        schemaVersion = 1,
        version = "0.1.0-alpha.1",
        versionCode = versionCode,
        tag = "v0.1.0-alpha.1",
        apkUrl = "https://github.com/thomrnowtea/livetracks/releases/download/v0.1.0-alpha.1/LiveTracks.apk",
        sha256 = "0".repeat(64),
        packageName = "com.thomrnowtea.livetracks",
        releaseUrl = "https://github.com/thomrnowtea/livetracks/releases/tag/v0.1.0-alpha.1",
        prerelease = true,
    )
}
