package com.iven.musicplayergo.uihelpers

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.ArtistsAdapter

/**
 * review of:
 * 波浪侧边栏
 * author: imilk
 * https://github.com/Solartisan/WaveSideBar.git
 */

// 计算波浪贝塞尔曲线的角弧长值
private const val ANGLE = Math.PI * 45 / 180
private const val ANGLE_R = Math.PI * 90 / 180

class WaveView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    View(context, attrs, defStyle) {
    private var listener: OnTouchLetterChangeListener? = null

    //RecyclerView letters
    private lateinit var mLetters: Array<String>

    //function to set and get letters
    var letters: Array<String>
        get() = mLetters
        set(letters) {
            mLetters = letters
            invalidate()
        }

    //navigation
    private var mChoose = -1
    private var oldChoose: Int = 0
    private var newChoose: Int = 0

    //paints
    private val mLetterPaint = Paint()
    private var mWavePaint = Paint()

    //paths
    private val mWavePath = Path()
    private val mBallPath = Path()

    //specs
    private var mBallLetterSize = context.resources.getDimensionPixelSize(R.dimen.wave_ball_textSize).toFloat()
    private var mWaveColor: Int = 0
    private var mLettersColor: Int = 0
    private val mRadius = context.resources.getDimensionPixelSize(R.dimen.wave_radius)
    private val mBallRadius = context.resources.getDimensionPixelSize(R.dimen.wave_ball_radius)
    private var mRatioAnimator: ValueAnimator? = null
    private var mRatio: Float = 0f
    private var mCenterY: Int = 0
    private var mBallCentreX: Float = 0.toFloat()
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    init {
        if (attrs != null) {
            val a = getContext().obtainStyledAttributes(attrs, R.styleable.WaveView)
            mLettersColor = a.getColor(R.styleable.WaveView_sidebarLettersColor, mLettersColor)
            mWaveColor = a.getColor(R.styleable.WaveView_sidebarAccentColor, mWaveColor)
            a.recycle()
        }

        mWavePaint = Paint()
        mWavePaint.isAntiAlias = true
        mWavePaint.style = Paint.Style.FILL
        mWavePaint.color = mWaveColor

        mLetterPaint.isAntiAlias = true
        mLetterPaint.color = mLettersColor
        mLetterPaint.style = Paint.Style.FILL
        mLetterPaint.textSize = mBallLetterSize
        mLetterPaint.textAlign = Paint.Align.CENTER
        mLetterPaint.typeface = ResourcesCompat.getFont(context, R.font.raleway_black)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val y = event.y
        val x = event.x

        oldChoose = mChoose
        newChoose = (y / mHeight * mLetters.size).toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                if (x < mWidth - 2 * mRadius) {
                    return false
                }
                mCenterY = y.toInt()
                startAnimator(mRatio, 1.0f)
            }
            MotionEvent.ACTION_MOVE -> {

                mCenterY = y.toInt()
                if (oldChoose != newChoose) {
                    if (newChoose >= 0 && newChoose < mLetters.size) {
                        mChoose = newChoose
                        if (listener != null) {
                            listener!!.onLetterChange(mLetters[newChoose])
                        }
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {

                startAnimator(mRatio, 0f)
                mChoose = -1
            }
            else -> {
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec)
        mWidth = measuredWidth
    }

    internal fun setOnWaveTouchListener(
        artistsRecyclerView: RecyclerView,
        artistsAdapter: ArtistsAdapter,
        artistsLayoutManager: LinearLayoutManager
    ) {
        listener = object : OnTouchLetterChangeListener {
            override fun onLetterChange(letter: String) {

                val pos = artistsAdapter.getLetterPosition(letter)

                if (pos != -1) {
                    artistsRecyclerView.scrollToPosition(pos)
                    artistsLayoutManager.scrollToPositionWithOffset(pos, 0)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //绘制波浪
        drawWavePath(canvas)

        //绘制圆
        drawBallPath(canvas)

        //绘制选中的字体
        drawBallLetter(canvas)
    }

    private fun drawBallLetter(canvas: Canvas) {
        if (mChoose != -1 && mRatio >= 0.9f) {
            // 绘制提示字符
            val target = mLetters[mChoose]
            val fontMetrics = mLetterPaint.fontMetrics
            val baseline = Math.abs(-fontMetrics.bottom - fontMetrics.top)
            val x = mBallCentreX
            val y = mCenterY + baseline / 2
            canvas.drawText(target, x, y, mLetterPaint)
        }
    }

    /**
     * 绘制波浪
     *
     * @param canvas
     */
    private fun drawWavePath(canvas: Canvas) {
        mWavePath.reset()
        // 移动到起始点
        mWavePath.moveTo(mWidth.toFloat(), (mCenterY - 3 * mRadius).toFloat())
        //计算上部控制点的Y轴位置
        val controlTopY = mCenterY - 2 * mRadius

        //计算上部结束点的坐标
        val endTopX = (mWidth - mRadius.toDouble() * Math.cos(ANGLE) * mRatio.toDouble()).toInt()
        val endTopY = (controlTopY + mRadius * Math.sin(ANGLE)).toInt()
        mWavePath.quadTo(mWidth.toFloat(), controlTopY.toFloat(), endTopX.toFloat(), endTopY.toFloat())

        //计算中心控制点的坐标
        val controlCenterX = (mWidth - 1.8 * mRadius.toDouble() * Math.sin(ANGLE_R) * mRatio.toDouble()).toInt()
        val controlCenterY = mCenterY
        //计算下部结束点的坐标
        val controlBottomY = mCenterY + 2 * mRadius
        val endBottomY = (controlBottomY - mRadius * Math.cos(ANGLE)).toInt()
        mWavePath.quadTo(controlCenterX.toFloat(), controlCenterY.toFloat(), endTopX.toFloat(), endBottomY.toFloat())

        mWavePath.quadTo(
            mWidth.toFloat(),
            controlBottomY.toFloat(),
            mWidth.toFloat(),
            (controlBottomY + mRadius).toFloat()
        )

        mWavePath.close()
        canvas.drawPath(mWavePath, mWavePaint)
    }

    private fun drawBallPath(canvas: Canvas) {
        //x轴的移动路径
        mBallCentreX = mWidth + mBallRadius - (2.0f * mRadius + 2.0f * mBallRadius) * mRatio

        mBallPath.reset()
        mBallPath.addCircle(mBallCentreX, mCenterY.toFloat(), mBallRadius.toFloat(), Path.Direction.CW)
        mBallPath.op(mWavePath, Path.Op.DIFFERENCE)
        mBallPath.close()
        canvas.drawPath(mBallPath, mWavePaint)
    }


    private fun startAnimator(vararg values: Float) {
        if (mRatioAnimator == null) {
            mRatioAnimator = ValueAnimator()
        }
        mRatioAnimator!!.cancel()
        mRatioAnimator!!.setFloatValues(*values)
        mRatioAnimator!!.addUpdateListener { value ->
            mRatio = value.animatedValue as Float
            //球弹到位的时候，并且点击的位置变了，即点击的时候显示当前选择位置
            if (mRatio == 1f && oldChoose != newChoose) {
                if (newChoose >= 0 && newChoose < mLetters.size) {
                    mChoose = newChoose
                    if (listener != null) {
                        listener!!.onLetterChange(mLetters[newChoose])
                    }
                }
            }
            invalidate()
        }
        mRatioAnimator!!.start()
    }

    interface OnTouchLetterChangeListener {
        fun onLetterChange(letter: String)
    }
}