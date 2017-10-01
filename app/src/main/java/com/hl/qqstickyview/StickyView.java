package com.hl.qqstickyview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * QQ粘性控件
 * Created by HL on 2017/9/30/0030.
 */

public class StickyView extends View {

    /**
     * 拖拽圆的半径
     */
    private float dragRadius = 12f;

    /**
     * 固定圆的半径
     */
    private float fixedRadius = 12f;

    /**
     * 拖拽的最大距离
     */
    private float maxDistance = 200f;

    /**
     * 拖拽圆的圆心
     */
    private PointF dragCenter = new PointF(200f, 400f);

    /**
     * 固定圆的圆心
     */
    private PointF fixedCenter = new PointF(280f, 400f);

    /**
     * 是否拽出范围
     */
    private boolean isDragOutRange = false;

    private Paint mPaint;
    private double mLineK;
    private Path mPath;

    //控制点的坐标
    private PointF controlPoint = new PointF(240f, 400f);

    //贝塞尔曲线与圆的相交的点
    private PointF[] dragPoint = {new PointF(200, 388f), new PointF(200f, 412f)};
    private PointF[] fixedPoint = {new PointF(280f, 388f), new PointF(250f, 412f)};

    public StickyView(Context context) {
        super(context);
        initPaint();
    }

    public StickyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public StickyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
    }

    /**
     * 动态获取固定圆的半径
     *
     * @return
     */
    private float getFixedRadius() {
        //获取两点之间的距离
        float centerDistance = GeometryUtil.getDistanceBetween2Points(dragCenter, fixedCenter);
        float fraction = centerDistance / maxDistance;
        //根据fraction的变化(0~1),计算12f~4f的变化
        float radius = GeometryUtil.evaluateValue(fraction, 12f, 4f);
        return radius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //让整体画布往上偏移
        canvas.translate(0, -Utils.getStatusBarHeight(getResources()));
        //动态获取固定圆的半径
        fixedRadius = getFixedRadius();

        float xOffset = dragCenter.x - fixedCenter.x;
        float yOffset = dragCenter.y - fixedCenter.y;
        if (xOffset != 0) {
            mLineK = yOffset / xOffset;
        }
        //获取曲线与圆的交点的坐标
        fixedPoint = GeometryUtil.getIntersectionPoints(fixedCenter, fixedRadius, mLineK);
        dragPoint = GeometryUtil.getIntersectionPoints(dragCenter, dragRadius, mLineK);

        //计算控制点 根据百分比获取两点之间的坐标
        controlPoint = GeometryUtil.getPointByPercent(dragCenter, fixedCenter, 0.618f);

        //绘制两个圆
        canvas.drawCircle(dragCenter.x, dragCenter.y, dragRadius, mPaint);

        if (!isDragOutRange) {
            canvas.drawCircle(fixedCenter.x, fixedCenter.y, dragRadius, mPaint);

            mPath = new Path();
            mPath.moveTo(fixedPoint[0].x, fixedPoint[0].y);
            //绘制贝塞尔曲线，(控制点坐标，结束点坐标)
            mPath.quadTo(controlPoint.x, controlPoint.y, dragPoint[0].x, dragPoint[0].y);
            mPath.lineTo(dragPoint[1].x, dragPoint[1].y);
            mPath.quadTo(controlPoint.x, controlPoint.y, fixedPoint[1].x, fixedPoint[1].y);
            //path.close();//默认是闭合的
            canvas.drawPath(mPath, mPaint);
        }

        //绘制最大范围
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(fixedCenter.x, fixedCenter.y, maxDistance, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //动态获取拖拽圆的圆心
                isDragOutRange = false;
                dragCenter.set(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_MOVE:
                //动态获取拖拽圆的圆心
                dragCenter.set(event.getRawX(), event.getRawY());
                if (GeometryUtil.getDistanceBetween2Points(dragCenter, fixedCenter) > maxDistance) {
                    isDragOutRange = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (GeometryUtil.getDistanceBetween2Points(dragCenter, fixedCenter) > maxDistance) {
                    isDragOutRange = true;
                    //如果拽出最大，则拖拽点回到固定点的位置
                    dragCenter.set(fixedCenter.x, fixedCenter.y);
                } else {
                    if (isDragOutRange) {
                        //曾超出最大范围   要到回到固定点的位置
                        dragCenter.set(fixedCenter.x, fixedCenter.y);
                    } else {
                        //以动画的形式回去
                        ValueAnimator animator = ObjectAnimator.ofFloat(1);
                        //获取当前拖拽点的坐标
                        final PointF startPoint = new PointF(dragCenter.x, dragCenter.y);
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                //获取动画执行的百分比
                                float fraction = animation.getAnimatedFraction();
                                PointF pointF = GeometryUtil.getPointByPercent(startPoint, fixedCenter, fraction);
                                dragCenter.set(pointF);
                                //引起重绘
                                invalidate();
                            }
                        });
                        animator.setDuration(500);
                        animator.setInterpolator(new OvershootInterpolator(3));
                        animator.start();
                    }
                }
                break;
        }
        //引起重绘
        invalidate();
        return true;
    }
}
