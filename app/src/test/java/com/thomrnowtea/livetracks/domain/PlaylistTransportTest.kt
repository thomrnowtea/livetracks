package com.thomrnowtea.livetracks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistTransportTest {
    @Test
    fun `natural completion advances to the next cue`() {
        assertEquals(1, nextPlaylistIndexAfterCompletion(currentIndex = 0, playlistSize = 3))
        assertEquals(2, nextPlaylistIndexAfterCompletion(currentIndex = 1, playlistSize = 3))
    }

    @Test
    fun `last cue ends the setlist instead of wrapping`() {
        assertNull(nextPlaylistIndexAfterCompletion(currentIndex = 2, playlistSize = 3))
    }

    @Test
    fun `invalid or empty selections cannot auto advance`() {
        assertNull(nextPlaylistIndexAfterCompletion(currentIndex = -1, playlistSize = 3))
        assertNull(nextPlaylistIndexAfterCompletion(currentIndex = 0, playlistSize = 0))
    }
}
