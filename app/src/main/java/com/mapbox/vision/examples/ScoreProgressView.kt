package com.mapbox.vision.examples

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.Slide
import androidx.transition.TransitionManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.mapbox.vision.examples.utils.dpToPx
import com.mapbox.vision.examples.utils.hide
import com.mapbox.vision.examples.utils.show
import kotlinx.android.synthetic.main.score_progress_view.view.*

class ScoreProgressView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val initScore = 75

    private var currentScore = initScore
        set(value) {
            if (value !in SCORE_MIN..SCORE_MAX) return
            field = value
            redrawProgress()
            redrawScore()
            redrawScoreColor()
        }

    private val gradientColors = intArrayOf(
        Color.parseColor("#57ff3f"), // green
        Color.parseColor("#e9f40e"), // yellow
        Color.parseColor("#ff3636") // red
    )

    private val argbEvaluator = ArgbEvaluator()

    private val warningAndScoreShowDuration = 2000L
    private val animationAppearDuration = 350L
    private val animationDisappearDuration = 350L
    private val betweenNewScoreDuration = 300L
    private var animationWillEndAt = 0L

    private var handlerHideWarning: Handler = Handler(Looper.getMainLooper())
    private var handlerHideScore: Handler = Handler(Looper.getMainLooper())

    private val handlerScore = Handler(Looper.getMainLooper())

    @get:ColorInt
    private val currentColor: Int
        get() =
            when (currentScore) {
                0 -> gradientColors[2]
                50 -> gradientColors[1]
                100 -> gradientColors[0]
                in SCORE_MIN..(SCORE_MAX / 2) -> {
                    argbEvaluator.evaluate(
                        (currentScore.toFloat() / (SCORE_MAX / 2)),
                        gradientColors[2],
                        gradientColors[1]
                    ) as Int
                }
                in (SCORE_MAX / 2)..SCORE_MAX -> {
                    argbEvaluator.evaluate(
                        ((currentScore - 50).toFloat() / (SCORE_MAX / 2)),
                        gradientColors[1],
                        gradientColors[0]
                    ) as Int

                }
                else -> gradientColors[1]
            }


    private val progressGradientWidth by lazy(LazyThreadSafetyMode.NONE) { progress_gradient.width }

    companion object {
        private const val SCORE_MIN = 0
        private const val SCORE_MAX = 100
    }

    init {
        View.inflate(context, R.layout.score_progress_view, this)
        progressViewContainer.bringToFront()
        progress_score_container.bringToFront()

        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
            .apply {
                cornerRadius = context.dpToPx(3f)
            }
        progress_gradient.background = gradientDrawable

        setupInitScoreMark()
    }

    private fun setupInitScoreMark() {
        val markFactor = 1 - initScore.toFloat() / SCORE_MAX
        init_score_mark.layoutParams =
            (init_score_mark.layoutParams as ConstraintLayout.LayoutParams).apply { horizontalBias = markFactor }
    }

    fun plusScore(amount: Int) = onNewScore(DriverScore.Positive(amount))

    fun minusScore(amount: Int, reason: String) = onNewScore(DriverScore.Negative(amount, reason))

    private fun onNewScore(nextScore: DriverScore) {
        val delta = (animationWillEndAt - System.currentTimeMillis()).let { if (it < 0) 0 else it }

        val delayMills: Long = delta + betweenNewScoreDuration

        animationWillEndAt = when (nextScore) {
            is DriverScore.Positive -> 0
            is DriverScore.Negative -> animationAppearDuration + animationDisappearDuration + warningAndScoreShowDuration
        } + delta + System.currentTimeMillis()

        handlerScore.postDelayed({

            when (nextScore) {
                is DriverScore.Positive -> {
                    currentScore += nextScore.score
                }
                is DriverScore.Negative -> {
                    currentScore -= nextScore.score
                    showWarning(nextScore.reason)
                    showWarningScore(nextScore.score)
                }
            }

        }, delayMills)
    }

    private fun redrawProgress() {
        val newWidth = (1f - currentScore.toFloat() / SCORE_MAX).let { it * progressGradientWidth }.toInt()
        progress_gray.layoutParams.width = newWidth
        progress_gray.requestLayout()
    }

    private fun redrawScore() {
        score_amount.text = "$currentScore"
    }

    private fun redrawScoreColor() {
        currentColor.let { color ->
            score_amount.setTextColor(color)
            score_amount_circle.circleColor = color
        }
    }

    private fun showWarning(message: String) {
        startTimerHideWarningTextAnimation()

        TransitionManager.beginDelayedTransition(
            warning_text_container,
            Slide(Gravity.TOP).apply { duration = animationAppearDuration })
        if (warning_text_container.visibility == View.GONE) {
            warning_text_container.show()
        }
        warning_text.text = message
    }

    private fun showWarningScore(score: Int) {
        startTimerHideScore()
        score_warning_container.show()
        score_warning_text.text = "-$score"
    }

    private fun startTimerHideWarningTextAnimation() {
        handlerHideWarning.removeCallbacksAndMessages(null)
        handlerHideWarning.postDelayed({
            TransitionManager.beginDelayedTransition(
                warning_text_container,
                Slide(Gravity.TOP).apply { duration = animationDisappearDuration }
            )
            warning_text_container.hide()
            warning_text.text = ""
        }, warningAndScoreShowDuration)
    }

    private fun startTimerHideScore() {
        handlerHideScore.removeCallbacksAndMessages(null)
        handlerHideScore.postDelayed({
            score_warning_container.hide()
            score_warning_text.text = ""
        }, warningAndScoreShowDuration)
    }
}

private sealed class DriverScore(score: Int) {
    data class Positive(val score: Int) : DriverScore(score)
    data class Negative(val score: Int, val reason: String) : DriverScore(score)
}