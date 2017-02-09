package com.pinger.reside;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.nineoldandroids.view.ViewHelper;



/**
 * user：  Pinger
 * date：  2017/1/30 14:20
 * desc：  仿酷狗音乐的侧滑菜单，可以拖拽的视图控件
 *
 * ================== API ===================
 *
 *
 *
 *
 *
 * ================= 使用教程 =================
 */
public class ResideLayout extends FrameLayout{

    private static final String TAG = "DragLayout";

    /**
     * 视图拖拽辅助类
     * 从第三方动画jar包nineoldandroids-library-2.4.0.jar引入
     */
    private ViewDragHelper mDragHelper;

    /**
     * ResideLayout控件的高度
     * 通过测量获得
     */
    private int mHeight;

    /**
     * ResideLayout控件的宽度
     */
    private int mWidth;

    /**
     * 控件在水平方向拖拽的距离范围
     */
    private int mSlideRange;


    /**
     * 左侧菜单面板容器对象
     */
    private ViewGroup mMenuContainer;

    /**
     * 主内容面板容器对象
     */
    private ViewGroup mMainContainer;


    /**
     * 控件的状态集合，包括3种状态
     */
    private enum Status{
        CLOSE,      // 打开
        OPEN,       // 关闭
        SLIDING     // 正在滑动
    }

    /**
     * 记录当前的状态，初始化为关闭状态
     */
    private Status currentStatus = Status.CLOSE;

    /**
     * 申明一个面板滑动监听
     */
    private PanelSlideListener mPanelSlideListener;

    /**
     * 控件滑动的距离占屏幕的百分比，默认为百分之60
     */
    private float mRangePercent = 0.6f;

    public ResideLayout(Context context) {
        this(context, null);
    }

    public ResideLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResideLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        /**
         * @param forParent 要进行触摸滑动的父控件
         * @param sensitivity 敏感度, 值越大越敏感,创建ViewDragHelper
         * @param cb 对View的事件发生改变的回调
         */
        mDragHelper = ViewDragHelper.create(this, 0.5f, mCallback);
    }

    /**
     * View拖动的回调用
     */
    ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {

        /**
         * 捕捉按下的View孩子
         * @param child 按下的子View
         * @param pointerId View的位置
         * @return 是否可以拖拽滑动，true表示可以，false表示不可以
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        /**
         * 获取子View水平方向的拖动范围
         * @param child 子View
         * @return 拖动范围
         */
        @Override
        public int getViewHorizontalDragRange(View child) {/*返回view水平方向的拖拽距离. > 0 . 决定了松手时动画执行时长, 水平方向是否可以滑动*/
            return mSlideRange;
        }

        /**
         * 获取子View水平拖动的距离
         * @param child 子View
         * @param left 距离左侧，滑动的距离
         * @param dx 每次滑动的距离差
         * @return  水平滑动的距离
         */
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            // 如果子View是mMainContainer，则需要去限制View滑动的边界
            if(child == mMainContainer)left = fixLeft(left);
            return left;
        }

        /**
         * 当子View的位置发送改变时回调
         * @param changedView 改变的子View
         * @param left 距离左边界距离
         * @param top 距离顶部距离
         * @param dx 水平滑动距离差
         * @param dy 竖直滑动距离差
         */
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            /**
             * 如果移动的是菜单面板, 直接将菜单面板的变化量转交给主面板，这样每次都是主面板在做滑动操作
             * 需要不停的给菜单面板和主面板重新绘制布局invalidate
             */
            if(changedView == mMenuContainer){
                mMenuContainer.layout(0, 0, mWidth, mHeight);
                int newLeft = mMainContainer.getLeft() + dx;
                newLeft = fixLeft(newLeft);
                mMainContainer.layout(newLeft, 0, newLeft + mWidth, mHeight);
            }

            // 处理移动事件
            performResideEvent();
            // 重绘
            invalidate();
        }
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if(xvel == 0 && mMainContainer.getLeft() > mSlideRange * 0.5f){
                open();
            } else if (xvel > 0) {
                open();
            } else {
                close();
            }

        }
        @Override
        public void onViewDragStateChanged(int state) {/*拖拽状态更新的时候调用*/
            super.onViewDragStateChanged(state);
        }

    };

    /**
     * 设置主面板滑动的边界
     * @param left 距离左侧的距离
     * @return 修正后的距离
     */
    private int fixLeft(int left) {
        if(left < 0){  // 限定左边界
            return 0;
        }else if (left > mSlideRange) {   // 限定右边界
            return mSlideRange;
        }
        return left;
    }

    /**
     * 更新子View的位置
     * 对子View进行动画处理
     * 更新控件的状态信息
     */
    protected void performResideEvent() {
        // 滑动的百分比
        float percent = mMainContainer.getLeft() * 1.0f / mSlideRange;

        // 动画处理
        performAnim(percent);

        // 上一次的滑动状态
        Status lastStatus = currentStatus;
        // 更新后的状态
        currentStatus = updateStatus(percent);

        // 比较状态的变化，回调不同的控件滑动状态
        if(lastStatus != currentStatus && mPanelSlideListener != null){
            if(currentStatus == Status.OPEN){
                mPanelSlideListener.onPanelOpened();
            }else if (currentStatus == Status.CLOSE) {
                mPanelSlideListener.onPanelClosed();
            }else{
                mPanelSlideListener.onPanelSlide(percent);
            }
        }
    }

    /**
     * 更新当前滑动的状态
     * @param percent 滑动百分比
     * @return
     */
    private Status updateStatus(float percent) {
        if(percent == 0){
            return Status.CLOSE;
        }else if (percent == 1.0f) {
            return Status.OPEN;
        }else{
            return Status.SLIDING;
        }
    }

    /**
     * 根据百分比，对子View进行动画处理，平移，缩放，渐变
     * @param percent 百分比
     */
    private void performAnim(float percent) {

        // scaleX = 0.5f + 1 * （1.0f - 0.5f）
        // 0.8f --> view的开始位置  ， 1.0f  -- > View的结束位置
        Log.d(TAG, "evaluate: --> " +  evaluate(percent, 0.8f, 1.0f));

        // 左侧菜单从0.8的大小，放大到1.0
        ViewHelper.setScaleX(mMenuContainer, evaluate(percent, 0.8f, 1.0f));
        ViewHelper.setScaleY(mMenuContainer, evaluate(percent, 0.8f, 1.0f));

        // 主容器从1.0缩小到0.7
        ViewHelper.setScaleX(mMainContainer, evaluate(percent, 1.0f, 0.7f));
        ViewHelper.setScaleY(mMainContainer, evaluate(percent, 1.0f, 0.7f));

        // 菜单平移
        ViewHelper.setTranslationX(mMenuContainer, evaluate(percent, - mWidth / 2.0f, 0));

        // 菜单渐变
        ViewHelper.setAlpha(mMenuContainer, evaluate(percent, 0.2f, 1.0f));

        // 拖拽时的背景颜色变化
        if (getBackground() == null) return;
        getBackground().setColorFilter((Integer)evaluateColor(percent, Color.TRANSPARENT, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
    }

    /** 估值器 */
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    /** 估算中间颜色 */
    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;
        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;
        return ((startA + (int)(fraction * (endA - startA))) << 24) |
                ((startR + (int)(fraction * (endR - startR))) << 16) |
                ((startG + (int)(fraction * (endG - startG))) << 8) |
                ((startB + (int)(fraction * (endB - startB))));
    }


    protected void close() {
        close(true);
    }

    public void close(boolean isSmooth){
        int finalLeft = 0;
        if(isSmooth){
            // 触发一个平滑动画Scroller
            if(mDragHelper.smoothSlideViewTo(mMainContainer, finalLeft, 0)){
                ViewCompat.postInvalidateOnAnimation(this);    // 触发界面重绘
            }
        }else {
            mMainContainer.layout(finalLeft, 0, finalLeft + mWidth, mHeight);
        }
    }

    protected void open() {
        open(true);
    }

    /**
     * 打开左侧菜单控件
     * @param isSmooth 是否带动画，默认缓慢打开
     */
    public void open(boolean isSmooth){
        int finalLeft = mSlideRange;
        if(isSmooth){
            if(mDragHelper.smoothSlideViewTo(mMainContainer, finalLeft, 0)){
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }else {
            mMainContainer.layout(finalLeft, 0, finalLeft + mWidth, mHeight);
        }
    }

    /**
     * 持续计算，持续的绘制
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if(mDragHelper.continueSettling(true)){
            ViewCompat.postInvalidateOnAnimation(this);
        }

    }

    /**
     * 转交拦截事件给辅助类
     * @param ev
     * @return
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    /**
     * 转交触摸事件给辅助类
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mDragHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (event.getAction()){
            case MotionEvent.ACTION_UP:
                if(currentStatus == Status.OPEN){
                    close();
                }
                break;
        }


        return true;
    }

    /**
     * 当控件的宽高发生变化时会回调这个方法，可以用来测量控件的宽高
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();
        /**
         * 初始化拖动的范围
         * 默认为屏幕宽度的60%
         */
        mSlideRange = (int) (mWidth * mRangePercent);
    }

    /**
     * 当子View填充结束时会调用这个方法，可以获取子View对象
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if(getChildCount() < 2){
            throw new IllegalStateException("ResideLayout控件的子View必须大于2个");
        }
        if(!((getChildAt(0) instanceof ViewGroup) && (getChildAt(1) instanceof ViewGroup))){
            throw new IllegalArgumentException("ResideLayout控件的子View必须是ViewGroup");
        }
        mMenuContainer = (ViewGroup) getChildAt(0);
        mMainContainer = (ViewGroup) getChildAt(1);
    }


    /**
     * 获取当前的状态
     * @return 当前状态
     */
    public Status getCurrentStatus() {
        return currentStatus;
    }

    /**
     * 设置当前状态
     * @param currentStatus 需要成设置的状态
     */
    public void setCurrentStatus(Status currentStatus) {
        this.currentStatus = currentStatus;
    }


    /**
     * 面板滑动监听，包括三种滑动状态
     */
    public interface PanelSlideListener {
        /**
         * 面板打开
         */
        void onPanelOpened();

        /**
         * 面板关闭
         */
        void onPanelClosed();

        /**
         * 面板正在滑动
         * @param percent  滑动的距离与滑动范围的百分比
         */
        void onPanelSlide(float percent);
    }

    /**
     * 获取滑动监听对象
     * @return 滑动监听对象
     */
    public PanelSlideListener getPanelSlideListener() {
        return mPanelSlideListener;
    }

    /**
     * 设置滑动监听对象
     * @param panelSlideListener
     */
    public void setPanelSlideListener(PanelSlideListener panelSlideListener) {
        this.mPanelSlideListener = panelSlideListener;
    }

    /**
     * 设置控件滑动的距离占屏幕的百分比
     * @param percent
     */
    public void setRangePercent(float percent){
        this.mRangePercent = percent;
    }


}