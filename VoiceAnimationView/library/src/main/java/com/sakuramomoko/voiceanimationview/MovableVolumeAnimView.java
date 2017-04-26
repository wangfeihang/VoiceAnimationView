package com.sakuramomoko.voiceanimationview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by wangfeihang on 2017/3/29.
 */
public class MovableVolumeAnimView extends View {


    public static final String TAG = "MovableVolumeAnimView";

    private static final int INTERVAL = 40;
    private static final int LEFT_TO_RIGHT = 0;
    private static final int RIGHT_TO_LEFT = 1;
    private static final int MID_TO_SIDES = 2;
    private long rectInterval;

    private float oneStepTime = 300;
    private float[] heights = {7, 10, 14, 18};

    private Paint paint;

    private int itemWidth;
    private int itemSpec;
    private int color;
    private float onDrawInterval;
    private List<DrawItem> rects;
    private int scale = 0; //0不变，1增加，-1减小
    private int defaultShakeHeight = 4; //原地抖动的长度
    private int shakeTime = 100;
    private int midSpec = PixelUtil.dpToPx(38);

    private int volume = 0; //0是小音量，1是大音量

    private Subscription mSubscription; //定时做声音动画
    private Subscription mAddRectSubscription; //定时做声音动画

    private int direction;

    private class DrawItem {
        public RectF rect;
        public double nowShakeHeight;
        public boolean shouldIncrease;
        public int randomHeightPosition;

        DrawItem(RectF rect, int randomHeightPosition) {
            this.rect = rect;
            nowShakeHeight = 0;
            shouldIncrease = true;
            this.randomHeightPosition = randomHeightPosition;
        }
    }

    public MovableVolumeAnimView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = null;
        try {
            ta = context.obtainStyledAttributes(attrs, R.styleable.MovableVolumeAnimView);
            color = ta.getColor(R.styleable.MovableVolumeAnimView_shapeColor, Color.WHITE);
            itemSpec = ta.getDimensionPixelSize(R.styleable.MovableVolumeAnimView_itemSpec, PixelUtil.dpToPx(3));
            itemWidth = ta.getDimensionPixelSize(R.styleable.MovableVolumeAnimView_itemWidth, PixelUtil.dpToPx(3));
            midSpec = ta.getDimensionPixelSize(R.styleable.MovableVolumeAnimView_midSpec, PixelUtil.dpToPx(38));
            direction = ta.getInt(R.styleable.MovableVolumeAnimView_animDirection, LEFT_TO_RIGHT);
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
        paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.FILL);
        rects = new ArrayList<>();
        onDrawInterval = (itemWidth + itemSpec) * INTERVAL / oneStepTime;
        rectInterval = (long) (INTERVAL * (itemWidth + itemSpec) / onDrawInterval);

    }

    public MovableVolumeAnimView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < rects.size(); i++) {
            changeRectParams(rects.get(i).rect, rects.get(i).shouldIncrease, rects.get(i).randomHeightPosition);
            if (direction == MID_TO_SIDES && rects.get(i).rect.left < getWidth() / 2 + midSpec / 2) {

            } else {
                canvas.drawRoundRect(rects.get(i).rect, 3, 3, paint);
                drawSymmetryRect(rects.get(i).rect, canvas);
            }
            changeShakeParams(i, rects.get(i).randomHeightPosition);
            boolean isBeyondScreen;
            if (direction == RIGHT_TO_LEFT) {
                isBeyondScreen = rects.get(i).rect.right < 0;
            } else {
                isBeyondScreen = rects.get(i).rect.left > getWidth();
            }
            if (isBeyondScreen) {
                rects = new LinkedList<>(rects.subList(0, i - 1));
                break;
            }
        }
    }

    private void drawSymmetryRect(RectF rectF, Canvas canvas) {
        RectF symmetryRectF = new RectF(rectF);
        if (direction == MID_TO_SIDES) {
            symmetryRectF.left = getWidth() - rectF.right;
            symmetryRectF.right = getWidth() - rectF.left;
            canvas.drawRoundRect(symmetryRectF, 3, 3, paint);
        }
    }

    private void changeShakeParams(int position, int randomHeightPosition) {
        float originalHeight = heights[randomHeightPosition];
        float shakeHeight = originalHeight / 5;
        float oneStepShake = shakeHeight / (shakeTime / INTERVAL);
        if (rects.get(position).shouldIncrease) {
            rects.get(position).nowShakeHeight += oneStepShake;
        } else {
            rects.get(position).nowShakeHeight -= oneStepShake;
        }
        double change = rects.get(position).nowShakeHeight;
        if (change > shakeHeight || change < 0) {
            rects.get(position).shouldIncrease = !rects.get(position).shouldIncrease;
        }
    }

    private void changeRectParams(RectF rect, boolean shouldIncrease, int randomHeightPosition) {
        if (direction == RIGHT_TO_LEFT) {
            rect.left -= onDrawInterval;
            rect.right -= onDrawInterval;
        } else {
            rect.left += onDrawInterval;
            rect.right += onDrawInterval;
        }
        float originalHeight = heights[randomHeightPosition];
        float shakeHeight = originalHeight / 5;
        float oneStepShake = shakeHeight / (shakeTime / INTERVAL);
        float oneStepVolume = (originalHeight / 2) / (250 / INTERVAL);
        if (scale > 0 && rect.height() < originalHeight * 2) {
            rect.top -= oneStepVolume;
            rect.bottom += oneStepVolume;
        } else if (scale < 0 && rect.height() > originalHeight) {
            rect.top += oneStepVolume;
            rect.bottom -= oneStepVolume;
        }
        if (shouldIncrease) {
            rect.top -= oneStepShake / 2;
            rect.bottom += oneStepShake / 2;
        } else {
            rect.top += oneStepShake / 2;
            rect.bottom -= oneStepShake / 2;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();

    }

    private void startAudioLoop() {
        if (null == mSubscription) {
            mSubscription = Observable.interval(INTERVAL, TimeUnit.MILLISECONDS)
                    .onBackpressureDrop()
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            invalidate();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e(TAG, "show audio anim  error.", throwable);
                            mSubscription.unsubscribe();
                            mSubscription = null;
                        }
                    });
        }
    }

    private void startAddRectLoop() {
        if (null == mAddRectSubscription) {
            mAddRectSubscription = Observable.interval(rectInterval, TimeUnit.MILLISECONDS)
                    .onBackpressureDrop()
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            DrawItem drawItem = initRect();
                            if (drawItem.rect.left > -10 && direction == LEFT_TO_RIGHT) {
                                rects.add(0, drawItem);
                            } else if (drawItem.rect.left < getWidth() + 10 && direction == RIGHT_TO_LEFT) {
                                rects.add(0, drawItem);
                            } else if (drawItem.rect.left >= getWidth() / 2 + midSpec / 2 && direction == MID_TO_SIDES) {
                                rects.add(0, drawItem);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e(TAG, "show audio anim  error.", throwable);
                            mAddRectSubscription.unsubscribe();
                            mAddRectSubscription = null;
                        }
                    });
        }
    }

    private DrawItem initRect() {
        int randomNumber = ((int) (Math.random() * 100)) % heights.length;
        float height = heights[randomNumber];
        float left, right;
        if (rects.size() == 0) {
            if (direction == LEFT_TO_RIGHT) {
                left = 0;
            } else if (direction == RIGHT_TO_LEFT) {
                left = getWidth();
            } else {
                left = getWidth() / 2 + midSpec / 2;
            }
            right = left + itemWidth;
        } else {
            if (direction == LEFT_TO_RIGHT) {
                left = rects.get(0).rect.left - itemWidth - itemSpec;
            } else if (direction == RIGHT_TO_LEFT) {
                left = rects.get(0).rect.left + itemWidth + itemSpec;
            } else {
                left = rects.get(0).rect.left - itemWidth - itemSpec;
            }
            right = left + itemWidth;
        }
        float centerY = getHeight() / 2;
        float top = centerY - height / 2;
        float bottom = centerY + height / 2;
        if (volume == 1) {
            top = centerY - height;
            bottom = centerY + height;
        }
        return new DrawItem(new RectF(left, top, right, bottom), randomNumber);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAudioLoop();
    }

    public void start() {
        if (isRectsNotEmpty()) {
            rects.clear();
        }
        startAudioLoop();
        startAddRectLoop();
    }


    public void stop() {
        if (isRectsNotEmpty()) {
            rects.clear();
        }
        stopAudioLoop();
    }

    public boolean isRectsNotEmpty() {
        return rects != null && rects.size() != 0;
    }


    private void stopAudioLoop() {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
            mSubscription = null;
        }
        if (mAddRectSubscription != null) {
            mAddRectSubscription.unsubscribe();
            mAddRectSubscription = null;
        }
    }

    public void setVolume(int volume) {
        if (volume > 30) {
            volume = 1;
        } else {
            volume = 0;
        }
        scale = volume - this.volume;
        this.volume = volume;
    }
}

