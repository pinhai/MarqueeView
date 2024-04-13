package com.hpx.widget.marqueeview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TintTypedArray;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 跑马灯View
 */
public class MarqueeView extends View {
    private static final String TAG = "MarqueeView";

    private int repeatType = REPEAT_CONTINUOUS;//滚动模式
    public static final int REPEAT_ONCE_TIME = 0;//一次结束
    public static final int REPEAT_CONTINUOUS = 1;//连续的，当前字段完全播完，再接着播
    public static final int REPEAT_CONTINUOUS_CUSTOM_INTERVAL = 2;//连续的，两段距离使用marqueeview_item_distance设置
    public static final int REPEAT_ALWAYS_SHOW = 3; //右边全部显示出来后就结束滚动，依此循环
    public static final int REPEAT_ALWAYS_SHOW_ONCE_TIME = 4; //右边全部显示出来后就结束滚动，只滚动一次

    private String content = "";
    private String finalContent;//最终要绘制的文本
    private float speed = 1;//移动速度
    private static final int SCROLL_INTERVAL_TIME = 10; //每次滚动的间隔时间，单位ms
    private int textColor = Color.BLACK;//文字颜色,默认黑色
    private float textSize = 12;//文字颜色,默认黑色
    private static final int paragraphIntervalInDp = 20;//段落默认间距，单位dp
    private int paragraphIntervalInPx = 0;//段落间距，单位px

    private int paragraphWidth;//一段内容的宽度，REPEAT_CONTINUOUS模式中一段内容包括显示文本+一些空格
//    private float textHeight;

    private int stayTimeBeforeScroll = 0; //滚动前停留的时间，单位ms
    private int stayTimeScrollFinished = 0;  //一次滚动循环结束后停留的时间，单位ms

    private float startLocationDistance = 1.0f;//开始的位置选取，百分比来的，距离左边，0~1，0代表不间距，1的话代表，从右面，1/2代表中间。

    private boolean isClickPause = false; //点击是否暂停
    private boolean isResetLocation = true; //控制重新设置文本内容的时候，是否初始化xLocation
    private float xLocation = 0;//文本的x坐标
    private float yLocation = 0;

    private boolean isScroll = false;//是否继续滚动

    private TextPaint paint;//画笔
    private Rect textRect;

    private int repeatCount = 0;

    private Timer timer;

    public MarqueeView(Context context) {
        this(context, null);
    }

    public MarqueeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarqueeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        initPaint();
        initClick();
        initScrollData();

        setContent(content);
    }

    private void initClick() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isClickPause) {
                    if (isScroll) {
                        stopScroll();
                    } else {
                        scroll();
                    }
                }
            }
        });
    }

    @SuppressLint("RestrictedApi")
    private void initAttrs(AttributeSet attrs) {
        TintTypedArray tta = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                R.styleable.MarqueeView);

        content = tta.getString(R.styleable.MarqueeView_marqueeview_content);
        textColor = tta.getColor(R.styleable.MarqueeView_marqueeview_text_color, textColor);
        isClickPause = tta.getBoolean(R.styleable.MarqueeView_marqueeview_is_click_pause, isClickPause);
        isResetLocation = tta.getBoolean(R.styleable.MarqueeView_marqueeview_is_resetLocation, isResetLocation);
        speed = tta.getFloat(R.styleable.MarqueeView_marqueeview_text_speed, speed);
        textSize = tta.getFloat(R.styleable.MarqueeView_marqueeview_text_size, textSize);
        paragraphIntervalInPx = tta.getDimensionPixelSize(R.styleable.MarqueeView_marqueeview_item_distance, dp2px(paragraphIntervalInDp));
        startLocationDistance = tta.getFloat(R.styleable.MarqueeView_marqueeview_text_start_location_distance, startLocationDistance);
        repeatType = tta.getInt(R.styleable.MarqueeView_marqueeview_repeat_type, repeatType);
        stayTimeBeforeScroll = tta.getInteger(R.styleable.MarqueeView_marqueeview_stay_time_before_scroll, stayTimeBeforeScroll);
        stayTimeScrollFinished = tta.getInteger(R.styleable.MarqueeView_marqueeview_stay_time_scroll_finished, stayTimeScrollFinished);

        tta.recycle();
    }

    private void initPaint() {
        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);//初始化文本画笔
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);//文字颜色值,可以不设定
        paint.setTextSize(dp2px(textSize));//文字大小
    }

    private void initScrollData(){
        if (startLocationDistance < 0) {
            startLocationDistance = 0;
        } else if (startLocationDistance > 1) {
            startLocationDistance = 1;
        }
        xLocation = getShowAreaWidth() * startLocationDistance + getPadding().left;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        textHeight = getContentHeight();
//        (float) getHeight() / 2 + textHeight / 2;
        yLocation = getPadding().top + getContentHeight();
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
            getHeightSize(heightMeasureSpec));
    }

    private int getHeightSize(int measureSpec){
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode){
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                Rect padding = getPadding();
                result = (int) getContentHeight() + padding.top + padding.bottom;
                break;
        }
        return result;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        //把文字画出来
        if (finalContent != null) {
            Rect padding = getPadding();
            canvas.clipRect(padding.left, 0, getMeasuredWidth()-padding.right, getMeasuredHeight());
            canvas.drawText(finalContent, xLocation, yLocation, paint);
        }
    }

    /**
     * @param isStop 暂停滚动回到初始状态后是否停止
     */
    private void pauseScrollAfterFinish(boolean isStop){
//        Log.i(TAG, "pauseScrollAfterFinish-isStop:"+isStop);
        stopScroll();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                initScrollData();
                postInvalidate();
                if(!isStop){
                    scroll();
                }
            }
        }, stayTimeScrollFinished);
    }

    private void startTimerSchedule(){
        if(timer == null){
            timer = new Timer();
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                xLocation = xLocation - speed;
                Log.i(TAG, "initTimerSchedule-xLocation: "+xLocation);

                Rect padding = getPadding();

                switch (repeatType) {
                    case REPEAT_CONTINUOUS:
                        if (paragraphWidth < (-xLocation+padding.left)) {
                            //也就是说文字已经到头了
                            xLocation = getWidth()-padding.right;
                        }
                        break;
                    case REPEAT_CONTINUOUS_CUSTOM_INTERVAL:
                        if (xLocation-padding.left < 0) {
                            int beAppend = (int) ((-xLocation+padding.left) / paragraphWidth);
                            if (beAppend >= repeatCount) {
                                repeatCount++;
                                finalContent = finalContent + appendBlankSuffix(content);
                            }
                        }
                        break;
                    case REPEAT_ALWAYS_SHOW:
                    case REPEAT_ALWAYS_SHOW_ONCE_TIME:
                        if((paragraphWidth >= getShowAreaWidth() &&
                            (paragraphWidth - getShowAreaWidth()) < (-xLocation+padding.left-5))){
                            //右边已经全部显示完成，暂停滚动
                            pauseScrollAfterFinish(repeatType == REPEAT_ALWAYS_SHOW_ONCE_TIME);
                        }
                        else if(shouldNotScroll()){
                            xLocation = getShowAreaWidth() * startLocationDistance + getPadding().left;
                            stopScroll();
                        }
                        break;
                    case REPEAT_ONCE_TIME:
                    default:
                        if (paragraphWidth < (-xLocation+padding.left-5)) {
                            //也就是说文字已经到头了,此时停止线程就可以了
                            stopScroll();
                        }
                        break;
                }

                postInvalidate();
            }
        }, stayTimeBeforeScroll, SCROLL_INTERVAL_TIME);
    }

    public void setRepeatType(int repeatType) {
        this.repeatType = repeatType;
        setContent(content);
    }

    /**
     * 执行滚动
     */
    private void scroll() {
        if(shouldNotScroll()){
            return;
        }

//        Log.i(TAG, "scroll-isScroll: "+isScroll);
        if (!isScroll) {
            stopTimer();

            isScroll = true;
            startTimerSchedule(); //开启死循环线程让文字动起来
        }
    }

    /**
     * 停止滚动
     */
    private void stopScroll() {
        isScroll = false;
        stopTimer();
    }

    private void stopTimer(){
        if(timer != null){
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private boolean shouldNotScroll(){
        //这两种模式下，如果内容宽度小于view的宽度，则不滚动
        return (paragraphWidth <= getWidth() && (repeatType == REPEAT_ALWAYS_SHOW || repeatType == REPEAT_ALWAYS_SHOW_ONCE_TIME));
    }

    /**
     * 点击是否暂停，默认是不
     */
    public void setClickPause(boolean isClickStop) {
        this.isClickPause = isClickStop;
    }

    /**
     * 设置文字颜色
     */
    public void setTextColor(int textColor) {
        if (textColor != 0) {
            this.textColor = textColor;
            paint.setColor(getResources().getColor(textColor));//文字颜色值,可以不设定
        }
    }

    /**
     * 设置文字大小
     */
    public void setTextSize(float textSize) {
        if (textSize > 0) {
            this.textSize = textSize;
            paint.setTextSize(dp2px(textSize));//文字颜色值,可以不设定
            paragraphWidth = (int) (getContentWidth(content) + paragraphIntervalInPx);//大小改变，需要重新计算宽高
        }
    }

    /**
     * 设置滚动速度
     */
    public void setTextSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * 设置滚动的内容并开始滚动
     */
    public void setContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return;
        }
//        Log.i(TAG, "setContent(String)");
        if (isResetLocation) {
            initScrollData();
        }

        this.content = content;

        if (repeatType == REPEAT_CONTINUOUS_CUSTOM_INTERVAL) {
            content = appendBlankSuffix(content);

            //如果说是循环的话，则需要计算文本的宽度 ，然后再根据屏幕宽度 ， 看能一个屏幕能盛得下几个文本
            paragraphWidth = (int) (getContentWidth(content) + paragraphIntervalInPx);//可以理解为一个单元内容的长度
            //从0 开始计算重复次数了， 否则到最后会跨不过这个坎而消失。
            repeatCount = 0;
            int contentCount = (getShowAreaWidth() / paragraphWidth) + 2;
            finalContent = "";
            for (int i = 0; i <= contentCount; i++) {
                finalContent = finalContent + content;//根据重复次数去叠加。
            }

        } else if (repeatType == REPEAT_ALWAYS_SHOW || repeatType == REPEAT_ALWAYS_SHOW_ONCE_TIME) {
            paragraphWidth = (int) getContentWidth(content);
            finalContent = content;
        } else {
//            Rect padding = getPadding();
//            if (xLocation-padding.left < 0 && repeatType == REPEAT_ONCE_TIME) {
//                if (-xLocation+padding.left+padding.right > contentWidth) {
//                    initScrollData();
//                }
//            }
            paragraphWidth = (int) getContentWidth(content);
            finalContent = content;
        }

        scroll();
    }

    /**
     * 从新添加内容的时候，是否初始化位置
     */
    public void setResetLocation(boolean isReset) {
        isResetLocation = isReset;
    }

    private Rect getPadding(){
        Rect result = new Rect();
        Rect bgPadding = new Rect();
        getBackground().getPadding(bgPadding);

        if(getPaddingTop() != 0){
            result.top = getPaddingTop();
        }else {
            result.top = bgPadding.top;
        }

        if(getPaddingBottom() != 0){
            result.bottom = getPaddingBottom();
        }else {
            result.bottom = bgPadding.bottom;
        }

        if(getPaddingLeft() != 0){
            result.left = getPaddingLeft();
        }else {
            result.left = bgPadding.left;
        }

        if(getPaddingRight() != 0){
            result.right = getPaddingRight();
        }else {
            result.right = bgPadding.right;
        }

        return result;
    }

    /**
     * 计算出一个空格的宽度
     */
    private float getBlankWidth() {
        String text1 = "en en";
        String text2 = "enen";
        return getContentWidth(text1) - getContentWidth(text2);
    }

    /**
     * 给段落添加空格后缀
     */
    private String appendBlankSuffix(String content){
        String blank = " ";
        float oneBlankWidth = getBlankWidth();//空格的宽度
        int count = (int) (paragraphIntervalInPx / oneBlankWidth);//空格个数

        if (count == 0) {
            count = 1;
        }

        StringBuilder blankSuffix = new StringBuilder();
        for (int i = 0; i <= count; i++) {
            blankSuffix.append(blank);
        }

        if (!content.endsWith(blankSuffix.toString())) {
            content = content + blankSuffix;
        }

        return content;
    }

    //计算可以显示内容的宽度
    private int getShowAreaWidth(){
        Rect padding = getPadding();
        return getWidth() - padding.left - padding.right;
    }

    private float getContentWidth(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        if (textRect == null) {
            textRect = new Rect();
        }
        paint.getTextBounds(content, 0, content.length(), textRect);
//        textHeight = getContentHeight();

        return textRect.width();
    }

    private float getContentHeight() {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return Math.abs((fontMetrics.bottom - fontMetrics.top)) / 2;
    }

    private int dp2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
