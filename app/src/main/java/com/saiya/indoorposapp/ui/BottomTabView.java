package com.saiya.indoorposapp.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.saiya.indoorposapp.R;

/**
 * 主页底部图标View
 */
public class BottomTabView extends View {
    /** 遮罩Bitmap */
    private Bitmap mBitmap;
    /** 遮罩Paint */
    private Paint mPaint;
    /** 选中时的颜色 */
    private int mColor;
    /** 遮罩透明度 0.0-1.0 */
    private float mAlpha = 0f;
    /** 图标 */
    private Bitmap mIconBitmap;
    /** 图标的绘制范围 */
    private Rect mIconRect = new Rect();
    /** 文字绘制的X坐标 */
    private float mTextX;
    /** 文字绘制的Y坐标 */
    private float mTextY;
    /** 底部文本 */
    private String mText = "";
    /** 绘制底部文本的Paint */
    private Paint mTextPaint;
    /** 底部文本的绘制范围 */
    private Rect mTextBound = new Rect();

    public BottomTabView(Context context) {
        super(context);
    }

    /**
     * 初始化自定义属性值
     * @param context 上下文
     * @param attrs 属性
     */
    public BottomTabView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 获取xml中的属性
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.BottomTabView);
        BitmapDrawable drawable = (BitmapDrawable) attributes
                .getDrawable(R.styleable.BottomTabView_tab_icon);
        if (drawable != null) {
            mIconBitmap = drawable.getBitmap();
        }
        mColor = attributes.getColor(R.styleable.BottomTabView_tab_color, 0xFF4293D6);
        mText = attributes.getString(R.styleable.BottomTabView_text);
        int mTextSize = (int) attributes.getDimension(R.styleable.BottomTabView_text_size,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10,
                        getResources().getDisplayMetrics()));
        attributes.recycle();

        //设置绘制文本的Paint
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        mTextPaint.setTextSize(mTextSize);

        // 得到text绘制范围
        mTextPaint.getTextBounds(mText, 0, mText.length(), mTextBound);

        //绘制遮罩
        mBitmap = mIconBitmap.copy(Bitmap.Config.ARGB_8888, true);
        /* 遮罩Canvas */
        Canvas mCanvas = new Canvas(mBitmap);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mCanvas.drawColor(mColor, PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 得到绘制icon的宽
        int bitmapWidth = Math.min(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - mTextBound.height());

        int left = getMeasuredWidth() / 2 - bitmapWidth / 2;
        int top = getPaddingTop();

        //设置icon的绘制范围
        mIconRect.set(left, top, left + bitmapWidth, top + bitmapWidth);

        //设置文字的绘制范围
        mTextX = getMeasuredWidth() / 2 - mTextBound.width() / 2;
        mTextY = getPaddingTop() + mIconRect.height() + mTextBound.height();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //转化透明度
        int alpha = (int) Math.ceil((255 * mAlpha));

        setupTargetBitmap(canvas, alpha);
        drawSourceText(canvas, alpha);
        drawTargetText(canvas, alpha);
    }

    private void setupTargetBitmap(Canvas canvas, int alpha) {
        //绘制原始图标
        canvas.drawBitmap(mIconBitmap, null, mIconRect, null);

        //设置遮罩透明度
        mPaint.setAlpha(alpha);

        //绘制遮罩
        canvas.drawBitmap(mBitmap, null, mIconRect, mPaint);
    }

    private void drawSourceText(Canvas canvas, int alpha) {
        mTextPaint.setColor(0xFF8A8A8A);
        mTextPaint.setAlpha(255 - alpha);
        canvas.drawText(mText, mTextX, mTextY, mTextPaint);
    }

    private void drawTargetText(Canvas canvas, int alpha) {
        mTextPaint.setColor(mColor);
        mTextPaint.setAlpha(alpha);
        canvas.drawText(mText, mTextX, mTextY, mTextPaint);
    }

    public void setIconAlpha(float alpha) {
        this.mAlpha = alpha;
        invalidate();
    }
}

