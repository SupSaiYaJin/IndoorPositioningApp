package com.saiya.indoorposapp.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 优化过的ViewPager,避免拖动地图与切换页面的冲突
 */
public class MyViewPager extends ViewPager implements MapView.OnMovingListener {

    /**  当前子控件是否处理拖动状态  */
    private boolean mChildIsBeingDragged=false;

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOffscreenPageLimit(3);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        return !mChildIsBeingDragged && super.onInterceptTouchEvent(arg0);
    }

    @Override
    public void startDrag() {
        mChildIsBeingDragged=true;
    }


    @Override
    public void stopDrag() {
        mChildIsBeingDragged=false;
    }

}
