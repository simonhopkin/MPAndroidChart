package com.github.mikephil.charting.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBandLineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by simon.hopkin on 24/05/2016.
 */
public class BandLineChartRenderer extends LineChartRenderer {


    protected Path topCubicPath = new Path();
    protected Path bottomCubicPath = new Path();


    public BandLineChartRenderer(LineDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }


    @Override
    public void drawData(Canvas c) {

        int width = (int) mViewPortHandler.getChartWidth();
        int height = (int) mViewPortHandler.getChartHeight();

        if (mDrawBitmap == null
                || (mDrawBitmap.get().getWidth() != width)
                || (mDrawBitmap.get().getHeight() != height)) {

            if (width > 0 && height > 0) {

                mDrawBitmap = new WeakReference<Bitmap>(Bitmap.createBitmap(width, height, mBitmapConfig));
                mBitmapCanvas = new Canvas(mDrawBitmap.get());
            } else
                return;
        }

        mDrawBitmap.get().eraseColor(Color.TRANSPARENT);

        LineData lineData = mChart.getLineData();

        for (ILineDataSet set : lineData.getDataSets()) {

            if (set.isVisible() && set.getEntryCount() > 0) {
                if (set instanceof IBandLineDataSet) {
                    drawBandDataSet(c, (IBandLineDataSet) set);
                }
                else {
                    drawDataSet(c, set);
                }
            }
        }

        c.drawBitmap(mDrawBitmap.get(), 0, 0, mRenderPaint);
    }

    protected void drawBandDataSet(Canvas c, IBandLineDataSet dataSet) {

        if (dataSet.getEntryCount() < 1)
            return;

        mRenderPaint.setStrokeWidth(dataSet.getLineWidth());
        mRenderPaint.setPathEffect(dataSet.getDashPathEffect());

        /*
        switch (dataSet.getMode()) {
            default:
            case LINEAR:
            case STEPPED:
                drawLinear(c, dataSet);
                break;

            case CUBIC_BEZIER:
                drawCubicBezier(c, dataSet);
                break;

            case HORIZONTAL_BEZIER:
                drawHorizontalBezier(c, dataSet);
                break;
        }
        */

        drawBandCubicBezier(c, dataSet);


        mRenderPaint.setPathEffect(null);
    }


    protected void drawBandCubicBezier(Canvas c, IBandLineDataSet dataSet) {

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        int entryCount = dataSet.getEntryCount();

        Entry entryFrom = dataSet.getEntryForXIndex((mMinX < 0) ? 0 : mMinX, DataSet.Rounding.DOWN);
        Entry entryTo = dataSet.getEntryForXIndex(mMaxX, DataSet.Rounding.UP);

        int diff = (entryFrom == entryTo) ? 1 : 0;
        int minx = Math.max(dataSet.getEntryIndex(entryFrom) - diff, 0);
        int maxx = Math.min(Math.max(minx + 2, dataSet.getEntryIndex(entryTo) + 1), entryCount);

        float phaseX = Math.max(0.f, Math.min(1.f, mAnimator.getPhaseX()));

        RectF topPathBounds = createCubitPathForDataSet(dataSet.getTopDataSet(), topCubicPath, false);
        RectF bottomPathBounds = createCubitPathForDataSet(dataSet.getBottomDataSet(), bottomCubicPath, true);

        int size = (int) Math.ceil((maxx - minx) * phaseX + minx);

        Path mergedPath = new Path();
        mergedPath.addPath(topCubicPath);
        mergedPath.lineTo(bottomPathBounds.left, bottomPathBounds.top);
        mergedPath.addPath(bottomCubicPath);
        mergedPath.setLastPoint(bottomPathBounds.right, bottomPathBounds.bottom);
        mergedPath.lineTo(topPathBounds.left, topPathBounds.top);

        cubicFillPath.reset();
        cubicFillPath.addPath(mergedPath);
        // create a new path, this is bad for performance
        drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, minx, size);

        mRenderPaint.setColor(dataSet.getColor());

        mRenderPaint.setStyle(Paint.Style.STROKE);

        trans.pathValueToPixel(topCubicPath);
        mBitmapCanvas.drawPath(topCubicPath, mRenderPaint);

        trans.pathValueToPixel(bottomCubicPath);
        mBitmapCanvas.drawPath(bottomCubicPath, mRenderPaint);

        //trans.pathValueToPixel(mergedPath);
        //mBitmapCanvas.drawPath(mergedPath, mRenderPaint);

        mRenderPaint.setPathEffect(null);
    }

    private static class CubicPoint {
        private PointF startPoint;
        private PointF controlPoint1;
        private PointF controlPoint2;
        private PointF endPoint;

        public CubicPoint(PointF startPoint, PointF controlPoint1, PointF controlPoint2, PointF endPoint) {
            this.startPoint = startPoint;
            this.controlPoint1 = controlPoint1;
            this.controlPoint2 = controlPoint2;
            this.endPoint = endPoint;
        }

        public PointF getStartPoint() {
            return startPoint;
        }

        public PointF getControlPoint1() {
            return controlPoint1;
        }

        public PointF getControlPoint2() {
            return controlPoint2;
        }

        public PointF getEndPoint() {
            return endPoint;
        }

    }

    private RectF createCubitPathForDataSet(ILineDataSet dataSet, Path path, boolean reverseOrder) {
        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        int entryCount = dataSet.getEntryCount();

        Entry entryFrom = dataSet.getEntryForXIndex((mMinX < 0) ? 0 : mMinX, DataSet.Rounding.DOWN);
        Entry entryTo = dataSet.getEntryForXIndex(mMaxX, DataSet.Rounding.UP);

        int diff = (entryFrom == entryTo) ? 1 : 0;
        int minx = Math.max(dataSet.getEntryIndex(entryFrom) - diff, 0);
        int maxx = Math.min(Math.max(minx + 2, dataSet.getEntryIndex(entryTo) + 1), entryCount);

        float phaseX = Math.max(0.f, Math.min(1.f, mAnimator.getPhaseX()));
        float phaseY = mAnimator.getPhaseY();

        float intensity = dataSet.getCubicIntensity();

        path.reset();

        int size = (int) Math.ceil((maxx - minx) * phaseX + minx);

        PointF lastPoint = new PointF();
        PointF firstPoint = new PointF();
        RectF bounds = new RectF();

        List<CubicPoint> points = new ArrayList<>();

        if (size - minx >= 2) {

            float prevDx = 0f;
            float prevDy = 0f;
            float curDx = 0f;
            float curDy = 0f;

            Entry prevPrev = dataSet.getEntryForIndex(minx);
            Entry prev = prevPrev;
            Entry cur = prev;
            Entry next = dataSet.getEntryForIndex(minx + 1);

            firstPoint.set(cur.getXIndex(), cur.getVal() * phaseY);
            lastPoint.set(cur.getXIndex(), cur.getVal() * phaseY);

            // let the spline start
            //path.moveTo(cur.getXIndex(), cur.getVal() * phaseY);

            for (int j = minx + 1, count = Math.min(size, entryCount); j < count; j++) {

                prevPrev = dataSet.getEntryForIndex(j == 1 ? 0 : j - 2);
                prev = dataSet.getEntryForIndex(j - 1);
                cur = dataSet.getEntryForIndex(j);
                next = entryCount > j + 1 ? dataSet.getEntryForIndex(j + 1) : cur;

                prevDx = (cur.getXIndex() - prevPrev.getXIndex()) * intensity;
                prevDy = (cur.getVal() - prevPrev.getVal()) * intensity;
                curDx = (next.getXIndex() - prev.getXIndex()) * intensity;
                curDy = (next.getVal() - prev.getVal()) * intensity;

                points.add(new CubicPoint(lastPoint,
                        new PointF(prev.getXIndex() + prevDx, (prev.getVal() + prevDy) * phaseY),
                        new PointF(cur.getXIndex() - curDx, (cur.getVal() - curDy) * phaseY),
                        new PointF(cur.getXIndex(), cur.getVal() * phaseY)));

                lastPoint.set(cur.getXIndex(), cur.getVal() * phaseY);

                //path.cubicTo(prev.getXIndex() + prevDx, (prev.getVal() + prevDy) * phaseY,
                //        cur.getXIndex() - curDx,
                //        (cur.getVal() - curDy) * phaseY, cur.getXIndex(), cur.getVal() * phaseY);

            }

            if (!reverseOrder) {

                if (!points.isEmpty()) {
                    path.moveTo(points.get(0).getStartPoint().x, points.get(0).getStartPoint().y);

                    for (CubicPoint point: points) {
                        path.cubicTo(point.getControlPoint1().x, point.getControlPoint1().y,
                                point.getControlPoint2().x, point.getControlPoint2().y,
                                point.getEndPoint().x, point.getEndPoint().y);
                    }

                    bounds.set(firstPoint.x, firstPoint.y, lastPoint.x, lastPoint.y);
                }

            }
            else {
                Collections.reverse(points);
                if (!points.isEmpty()) {

                    path.moveTo(points.get(0).getEndPoint().x, points.get(0).getEndPoint().y);

                    for (CubicPoint point : points) {
                        path.cubicTo(point.getControlPoint2().x, point.getControlPoint2().y,
                                point.getControlPoint1().x, point.getControlPoint1().y,
                                point.getStartPoint().x, point.getStartPoint().y);
                    }

                    bounds.set(lastPoint.x, lastPoint.y, firstPoint.x, firstPoint.y);
                }
            }

        }

        return bounds;
    }



    @Override
    protected void drawCubicFill(Canvas c, ILineDataSet dataSet, Path spline, Transformer trans, int from, int to) {
/*
        if (to - from <= 1)
            return;

        float fillMin = dataSet.getFillFormatter()
                .getFillLinePosition(dataSet, mChart);

        // Take the from/to xIndex from the entries themselves,
        // so missing entries won't screw up the filling.
        // What we need to draw is line from points of the xIndexes - not arbitrary entry indexes!

        final Entry toEntry = dataSet.getEntryForIndex(to - 1);
        final Entry fromEntry = dataSet.getEntryForIndex(from);
        final float xTo = toEntry == null ? 0 : toEntry.getXIndex();
        final float xFrom = fromEntry == null ? 0 : fromEntry.getXIndex();

        spline.lineTo(xTo, fillMin);
        spline.lineTo(xFrom, fillMin);
        */
        spline.close();

        trans.pathValueToPixel(spline);

        final Drawable drawable = dataSet.getFillDrawable();
        if (drawable != null) {

            drawFilledPath(c, spline, drawable);
        } else {

            drawFilledPath(c, spline, dataSet.getFillColor(), dataSet.getFillAlpha());
        }
    }

    @Override
    public void drawExtras(Canvas c) {
        //super.drawExtras(c);
        //TODO
    }

    @Override
    protected void drawCircles(Canvas c) {
        //super.drawCircles(c);
        //TODO
    }

    @Override
    public void drawValues(Canvas c) {
        //super.drawValues(c);
        //TODO
    }
}
