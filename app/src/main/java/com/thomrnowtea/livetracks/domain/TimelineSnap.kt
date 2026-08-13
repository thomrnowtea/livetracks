package com.thomrnowtea.livetracks.domain

import kotlin.math.abs

/**
 * Snaps a Timeline position to the visible time grid. Nearby musical or clip anchors can guide
 * the result, but the final position always remains on the selected time-grid unit.
 * UI drag previews remain continuous and call this for the drop target.
 */
fun snapTimelineFrames(
    proposedFrames: Long,
    gridStepFrames: Long,
    magneticTargets: Iterable<Long> = emptyList(),
    magneticToleranceFrames: Long = 0,
    enabled: Boolean = true,
): Long {
    val proposed = proposedFrames.coerceAtLeast(0)
    if (!enabled) return proposed
    val step = gridStepFrames.coerceAtLeast(1)
    val tolerance = magneticToleranceFrames.coerceAtLeast(0)
    val magneticTarget = magneticTargets
        .asSequence()
        .filter { it >= 0 }
        .minByOrNull { abs(it - proposed) }

    val guidedTarget = if (magneticTarget != null && abs(magneticTarget - proposed) <= tolerance) {
        magneticTarget
    } else {
        proposed
    }
    return ((guidedTarget + step / 2) / step) * step
}
