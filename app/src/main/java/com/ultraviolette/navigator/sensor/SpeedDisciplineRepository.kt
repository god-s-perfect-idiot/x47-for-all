package com.ultraviolette.navigator.sensor

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Live speed-discipline score (0–10) for the right footer stripe bar.
 *
 * GPS-only metric focused on speed management (not smoothness / IMU jerk):
 * consistency of cruising speed, unnecessary fluctuations, and holding a
 * target cruise. Stops and crawl traffic are excluded; sustained decelerations
 * that settle into a new lower cruise are treated as intentional (traffic /
 * turns) rather than yo-yo instability.
 *
 * S = 10(0.5C + 0.3T + 0.2R) with each component in 0..1.
 */
data class DisciplineSample(
    /** Rolling score in 0–10. */
    val score: Float,
    /** Discrete SPD stripes 0–10. */
    val bars: Int,
    /** True when enough cruising-window data was available to trust [score]. */
    val confident: Boolean
)

class SpeedDisciplineRepository {
    private val pendingLock = Any()
    private val pending = ArrayDeque<SpeedTick>(64)

    @Volatile
    private var latestSpeedKmh: Float = 0f

    @Volatile
    private var latestMoving: Boolean = false

    /** Optional rider-chosen cruise; null → auto-establish from behaviour. */
    @Volatile
    private var configuredTargetKmh: Float? = null

    /** Feed GPS / fused speed from [com.ultraviolette.navigator.ui.ClusterViewModel]. */
    fun updateSpeed(speedKmh: Float, isMoving: Boolean) {
        val kmh = if (isMoving) speedKmh.coerceAtLeast(0f) else 0f
        latestMoving = isMoving
        latestSpeedKmh = kmh
        val tick = SpeedTick(
            speedKmh = kmh,
            isMoving = isMoving,
            elapsedMs = SystemClock.elapsedRealtime()
        )
        synchronized(pendingLock) {
            if (pending.size >= MAX_PENDING) pending.removeFirst()
            pending.addLast(tick)
        }
    }

    /** Pin a target cruise (km/h), or pass null to resume auto-target. */
    fun setTargetSpeedKmh(targetKmh: Float?) {
        configuredTargetKmh = targetKmh?.takeIf { it >= MIN_SCORE_SPEED_KMH }
    }

    fun observeDiscipline(): Flow<DisciplineSample> = flow {
        val engine = DisciplineEngine()
        while (true) {
            val batch = synchronized(pendingLock) {
                if (pending.isEmpty()) emptyList()
                else {
                    val copy = pending.toList()
                    pending.clear()
                    copy
                }
            }
            for (tick in batch) {
                engine.ingest(tick.speedKmh, tick.isMoving, tick.elapsedMs)
            }
            val sample = engine.score(
                speedKmh = latestSpeedKmh,
                isMoving = latestMoving,
                configuredTargetKmh = configuredTargetKmh,
                nowElapsedMs = SystemClock.elapsedRealtime()
            )
            emit(sample)
            delay(SCORE_INTERVAL_MS)
        }
    }

    private data class SpeedTick(
        val speedKmh: Float,
        val isMoving: Boolean,
        val elapsedMs: Long
    )

    private companion object {
        const val SCORE_INTERVAL_MS = 1_000L
        const val MAX_PENDING = 64
        const val MIN_SCORE_SPEED_KMH = 15f
    }
}

/**
 * Rolling-window speed-discipline features.
 *
 * Weights: consistency 50% · stability 30% · cruising 20%.
 * Calibration constants are engineering starting points — tune on real rides.
 */
internal class DisciplineEngine {
    private val speeds = RingStats(CONSISTENCY_WINDOW_MS)
    private val changeEvents = ArrayDeque<ChangeEvent>()

    private var prevSpeedKmh: Float? = null
    private var prevElapsedMs: Long = 0L
    private var autoTargetKmh: Float? = null
    private var cruiseStableMs: Long = 0L
    private var lastCruiseSpeed: Float? = null

    private var heldScore: Float = 0f
    private var hasHeldScore: Boolean = false

    fun ingest(speedKmh: Float, isMoving: Boolean, nowElapsedMs: Long) {
        val previous = prevSpeedKmh
        val dtMs = if (prevElapsedMs > 0L) (nowElapsedMs - prevElapsedMs) else 0L
        prevSpeedKmh = speedKmh
        prevElapsedMs = nowElapsedMs

        if (!isMoving || speedKmh < MIN_SCORE_SPEED_KMH) {
            cruiseStableMs = 0L
            lastCruiseSpeed = null
            return
        }

        speeds.add(speedKmh, nowElapsedMs)

        if (previous == null || dtMs <= 0L || dtMs > MAX_SAMPLE_GAP_MS) return

        val dtSec = dtMs / 1_000f
        val delta = speedKmh - previous
        val accelKmhPerSec = delta / dtSec

        // Sustained smooth slowdown that settles → intentional (traffic / turn).
        // Only count sharp or reversing changes as instability candidates.
        val significant = abs(delta) >= SIGNIFICANT_DELTA_KMH
        if (!significant) {
            // Near-steady → accumulate cruise lock.
            if (abs(accelKmhPerSec) < CRUISE_ACCEL_KMH_S) {
                if (lastCruiseSpeed != null &&
                    abs(speedKmh - lastCruiseSpeed!!) <= CRUISE_LOCK_BAND_KMH
                ) {
                    cruiseStableMs += dtMs
                } else {
                    cruiseStableMs = dtMs
                    lastCruiseSpeed = speedKmh
                }
            } else {
                cruiseStableMs = 0L
            }
            return
        }

        cruiseStableMs = 0L
        lastCruiseSpeed = null

        val intentionalDecel = delta < 0f &&
            abs(accelKmhPerSec) <= INTENTIONAL_DECEL_KMH_S &&
            speedKmh >= MIN_SCORE_SPEED_KMH

        changeEvents.addLast(
            ChangeEvent(
                elapsedMs = nowElapsedMs,
                deltaKmh = delta,
                intentional = intentionalDecel
            )
        )
        evictChanges(nowElapsedMs)
    }

    fun score(
        speedKmh: Float,
        isMoving: Boolean,
        configuredTargetKmh: Float?,
        nowElapsedMs: Long
    ): DisciplineSample {
        speeds.evict(nowElapsedMs)
        evictChanges(nowElapsedMs)

        val scoring = isMoving && speedKmh >= MIN_SCORE_SPEED_KMH
        if (!scoring) {
            // Below cruise / stopped — dark-grey off state. Keep last score.
            return idleSample()
        }

        if (speeds.size < MIN_SPEED_SAMPLES) {
            return idleSample()
        }

        updateAutoTarget(speedKmh)
        val target = configuredTargetKmh ?: autoTargetKmh

        val c = consistencyScore()
        val t = stabilityScore(nowElapsedMs)
        val r = cruisingScore(speedKmh, target)

        val score = (10f * (0.5f * c + 0.3f * t + 0.2f * r)).coerceIn(0f, 10f)
        heldScore = score
        hasHeldScore = true

        return DisciplineSample(
            score = score,
            bars = score.barCount(),
            confident = true
        )
    }

    private fun updateAutoTarget(speedKmh: Float) {
        if (cruiseStableMs < CRUISE_LOCK_MS) return
        val seed = lastCruiseSpeed ?: speedKmh
        val current = autoTargetKmh
        autoTargetKmh = if (current == null) {
            seed
        } else {
            current * (1f - TARGET_EMA_ALPHA) + seed * TARGET_EMA_ALPHA
        }
    }

    /** C — low σ over the rolling window → closer to 1. */
    private fun consistencyScore(): Float {
        val sigma = speeds.stdDev()
        return exp((-K_CONSISTENCY * sigma).toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /**
     * T — penalize frequent / large unnecessary changes (yo-yo).
     * Intentional decelerations are down-weighted heavily.
     */
    private fun stabilityScore(nowElapsedMs: Long): Float {
        if (changeEvents.isEmpty()) return 1f

        var weighted = 0.0
        for (event in changeEvents) {
            val ageFrac = 1.0 - ((nowElapsedMs - event.elapsedMs).toDouble() / STABILITY_WINDOW_MS)
            val weight = ageFrac.coerceIn(0.0, 1.0)
            val magnitude = abs(event.deltaKmh).toDouble()
            val penalty = if (event.intentional) {
                magnitude * INTENTIONAL_WEIGHT
            } else {
                magnitude
            }
            weighted += penalty * weight
        }

        // Normalize roughly: ~4 meaningful flips / min of ±5 km/h → mid score.
        val windowMin = STABILITY_WINDOW_MS / 60_000.0
        val rate = if (windowMin > 0.0) weighted / windowMin else weighted
        return exp((-K_STABILITY * rate).toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /** R — closeness to target cruise when a target exists. */
    private fun cruisingScore(speedKmh: Float, targetKmh: Float?): Float {
        if (targetKmh == null || targetKmh < MIN_SCORE_SPEED_KMH) {
            // No target yet — neutral contribution so early ride is not tanked.
            return 0.75f
        }
        val err = abs(speedKmh - targetKmh)
        return exp((-K_CRUISE * err).toDouble()).toFloat().coerceIn(0f, 1f)
    }

    private fun evictChanges(nowElapsedMs: Long) {
        val cutoff = nowElapsedMs - STABILITY_WINDOW_MS
        while (changeEvents.isNotEmpty() && changeEvents.first().elapsedMs < cutoff) {
            changeEvents.removeFirst()
        }
    }

    private fun idleSample(): DisciplineSample {
        if (!hasHeldScore) {
            return DisciplineSample(score = 0f, bars = 0, confident = false)
        }
        return DisciplineSample(
            score = heldScore,
            bars = heldScore.barCount(),
            confident = false
        )
    }

    private data class ChangeEvent(
        val elapsedMs: Long,
        val deltaKmh: Float,
        val intentional: Boolean
    )

    private companion object {
        const val CONSISTENCY_WINDOW_MS = 30_000L
        const val STABILITY_WINDOW_MS = 60_000L
        const val MAX_SAMPLE_GAP_MS = 5_000L

        const val MIN_SCORE_SPEED_KMH = 15f
        const val MIN_SPEED_SAMPLES = 8

        const val SIGNIFICANT_DELTA_KMH = 2.5f
        const val INTENTIONAL_DECEL_KMH_S = 4.0f
        const val INTENTIONAL_WEIGHT = 0.25f

        const val CRUISE_ACCEL_KMH_S = 0.8f
        const val CRUISE_LOCK_BAND_KMH = 3.0f
        const val CRUISE_LOCK_MS = 4_000L
        const val TARGET_EMA_ALPHA = 0.18f

        /** ~3.2 km/h σ → C ≈ 0.79. */
        const val K_CONSISTENCY = 0.074f
        /** Soft rate penalty so a few clean changes still score well. */
        const val K_STABILITY = 0.045f
        /** ±3 km/h from target → R ≈ 0.74. */
        const val K_CRUISE = 0.10f

        fun Float.barCount(): Int = roundToInt().coerceIn(0, 10)
    }
}
