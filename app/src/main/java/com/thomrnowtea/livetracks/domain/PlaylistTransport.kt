package com.thomrnowtea.livetracks.domain

/** Returns the next cue only after a valid current cue completed naturally. */
fun nextPlaylistIndexAfterCompletion(currentIndex: Int, playlistSize: Int): Int? {
    if (playlistSize <= 0 || currentIndex !in 0 until playlistSize) return null
    return (currentIndex + 1).takeIf { it < playlistSize }
}
