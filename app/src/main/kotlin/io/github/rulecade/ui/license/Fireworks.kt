package io.github.rulecade.ui.license

import androidx.compose.animation.core.CubicBezierEasing
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// Award fireworks in the award screen's own 1707 x 1210 space, scaled when drawn. Every position
// is a closed-form function of time, so a frame needs no state, only the buffers it is written to.
internal object Fireworks {
    const val WIDTH = 1707f
    const val HEIGHT = 1210f
    const val END_MILLIS = 1800
    const val CARD_IN_END = 260
    const val TEXT_IN_START = 120
    const val TEXT_IN_END = 440
    const val DECOR_ALPHA = 0.55f
    const val ALPHA_LEVELS = 4
    val SPARK_SIZES = floatArrayOf(18f, 12f, 6f)
    private const val FLASH_MILLIS = 90
    private const val GRID = 6f
    private const val GRAVITY = 1500f
    private const val DRAG = 2.2f
    private const val SPARKS_PER_BURST = 40
    private const val DECOR_CANDIDATES = 28

    enum class Tint { Accent, Gold, Silver, Flash }

    class Decor(val x: Float, val y: Float, val size: Float, val tint: Tint)

    private class Burst(val x: Float, val y: Float, val start: Int, val tint: Tint)

    private class Particle(
        val burst: Burst,
        val vx: Float,
        val vy: Float,
        val life: Float,
        val tint: Tint
    )

    private val easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    private val bursts = listOf(
        Burst(1080f, 150f, 200, Tint.Accent),
        Burst(120f, 200f, 420, Tint.Gold),
        Burst(1560f, 1080f, 640, Tint.Accent)
    )

    // Park-Miller generator with fixed seeds, so the pattern matches the confirmed mockup.
    private class Seeded(private var seed: Long) {
        fun next(): Float {
            seed = seed * 16807 % 2147483647
            return (seed / 2147483647.0).toFloat()
        }
    }

    // Static background dots; the card and the text beside it stay clear.
    val decor: List<Decor> = run {
        val random = Seeded(11)
        val tints = listOf(Tint.Accent, Tint.Gold, Tint.Silver)
        (0 until DECOR_CANDIDATES).mapNotNull { i ->
            val x = 40f + random.next() * 1620f
            val y = 40f + random.next() * 1130f
            val size = 12f + (random.next() * 3f).roundToInt() * 4f
            if (inContent(x, y)) null else Decor(x, y, size, tints[i % 3])
        }
    }

    fun inContent(x: Float, y: Float) = x > 30f && x < 1680f && y > 250f && y < 950f

    private val sparks: List<Particle>
    private val settlers: List<Particle>

    init {
        val random = Seeded(23)
        sparks = bursts.flatMap { burst ->
            (0 until SPARKS_PER_BURST).map { k ->
                val angle = random.next() * 2 * PI.toFloat()
                val speed = 260f + random.next() * 820f
                val tint = when {
                    k % 5 == 0 -> Tint.Silver
                    k % 3 == 0 -> if (burst.tint == Tint.Accent) Tint.Gold else Tint.Accent
                    else -> burst.tint
                }
                Particle(
                    burst,
                    cos(angle) * speed,
                    sin(angle) * speed - 160f,
                    600f + random.next() * 500f,
                    tint
                )
            }
        }
        settlers = decor.mapIndexed { i, d ->
            val burst = bursts[i % 3]
            val angle = atan2(d.y - burst.y, d.x - burst.x)
            val speed = 380f + random.next() * 200f
            Particle(burst, cos(angle) * speed, sin(angle) * speed - 120f, 0f, d.tint)
        }
    }

    val sparkCount: Int get() = sparks.size
    val settlerCount: Int get() = settlers.size

    fun progress(t: Float, start: Int, end: Int) =
        easing.transform(((t - start) / (end - start)).coerceIn(0f, 1f))

    // Sparks go into one point list per tint, size, and alpha level, so a frame needs one draw
    // call per group; settlers are written one by one.
    fun fill(t: Float, frame: Frame) {
        frame.counts.fill(0)
        for (p in sparks) {
            val age = t - p.burst.start
            val f = age / p.life
            if (age < 0f || f >= 1f) continue
            val sizeIndex = if (f < 0.3f) {
                0
            } else if (f < 0.65f) {
                1
            } else {
                2
            }
            val alpha = if (f > 0.75f) (1f - f) / 0.25f else 1f
            val alphaIndex = (ceil(alpha * ALPHA_LEVELS).toInt() - 1).coerceIn(0, ALPHA_LEVELS - 1)
            val tint = if (age < FLASH_MILLIS) Tint.Flash else p.tint
            val bucket = Frame.bucket(tint.ordinal, sizeIndex, alphaIndex)
            val n = frame.counts[bucket]
            frame.points[bucket][n * 2] = snap(flightX(p, t))
            frame.points[bucket][n * 2 + 1] = snap(flightY(p, t))
            frame.counts[bucket] = n + 1
        }
        frame.settled = 0
        settlers.forEachIndexed { i, p ->
            val age = t - p.burst.start
            if (age < 0f) return@forEachIndexed
            val d = decor[i]
            val land = progress(t, p.burst.start + 300, END_MILLIS)
            val k = frame.settled++
            frame.settlerX[k] =
                lerp(flightX(p, t), d.x + d.size / 2f, land).let { if (land < 1f) snap(it) else it }
            frame.settlerY[k] =
                lerp(flightY(p, t), d.y + d.size / 2f, land).let { if (land < 1f) snap(it) else it }
            frame.settlerSize[k] = (lerp(14f, d.size, land) / 2f).roundToInt() * 2f
            frame.settlerAlpha[k] = lerp(1f, DECOR_ALPHA, land)
            frame.settlerTint[k] = if (age < FLASH_MILLIS) Tint.Flash.ordinal else d.tint.ordinal
        }
    }

    class Frame {
        val points = Array(BUCKETS) { FloatArray(sparkCount * 2) }
        val counts = IntArray(BUCKETS)
        val settlerX = FloatArray(settlerCount)
        val settlerY = FloatArray(settlerCount)
        val settlerSize = FloatArray(settlerCount)
        val settlerAlpha = FloatArray(settlerCount)
        val settlerTint = IntArray(settlerCount)
        var settled = 0

        companion object {
            val BUCKETS = Tint.entries.size * SPARK_SIZES.size * ALPHA_LEVELS

            fun bucket(tint: Int, size: Int, alpha: Int) =
                (tint * SPARK_SIZES.size + size) * ALPHA_LEVELS + alpha

            fun tintOf(bucket: Int) = bucket / (SPARK_SIZES.size * ALPHA_LEVELS)

            fun sizeOf(bucket: Int) = SPARK_SIZES[bucket / ALPHA_LEVELS % SPARK_SIZES.size]

            fun alphaOf(bucket: Int) = (bucket % ALPHA_LEVELS + 1f) / ALPHA_LEVELS
        }
    }

    private fun flightX(p: Particle, t: Float) = p.burst.x + p.vx * drift(p, t)

    private fun flightY(p: Particle, t: Float): Float {
        val dt = (t - p.burst.start).coerceAtLeast(0f) / 1000f
        val e = drift(p, t)
        return p.burst.y + p.vy * e + GRAVITY / DRAG * (dt - e)
    }

    private fun drift(p: Particle, t: Float): Float {
        val dt = (t - p.burst.start).coerceAtLeast(0f) / 1000f
        return (1f - exp(-DRAG * dt)) / DRAG
    }

    private fun snap(v: Float) = (v / GRID).roundToInt() * GRID

    private fun lerp(a: Float, b: Float, p: Float) = a + (b - a) * p
}
