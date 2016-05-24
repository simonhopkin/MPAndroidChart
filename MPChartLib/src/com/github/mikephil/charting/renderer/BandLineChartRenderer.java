package com.github.mikephil.charting.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;

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

        PointF topPathEndPoint = createCubitPathForDataSet(dataSet.getTopDataSet(), topCubicPath);
        PointF bottomPathEndPoint = createCubitPathForDataSet(dataSet.getBottomDataSet(), bottomCubicPath);

        int size = (int) Math.ceil((maxx - minx) * phaseX + minx);

        topCubicPath.lineTo(bottomPathEndPoint.x, bottomPathEndPoint.y);
        topCubicPath.addPath(bottomCubicPath);

        cubicFillPath.reset();
        cubicFillPath.addPath(topCubicPath);
        // create a new path, this is bad for performance
        drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, minx, size);

        mRenderPaint.setColor(dataSet.getColor());

        mRenderPaint.setStyle(Paint.Style.STROKE);

        trans.pathValueToPixel(topCubicPath);

        mBitmapCanvas.drawPath(topCubicPath, mRenderPaint);

        mRenderPaint.setPathEffect(null);
    }


    private PointF createCubitPathForDataSet(ILineDataSet dataSet, Path path) {
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

        if (size - minx >= 2) {

            float prevDx = 0f;
            float prevDy = 0f;
            float curDx = 0f;
            float curDy = 0f;

            Entry prevPrev = dataSet.getEntryForIndex(minx);
            Entry prev = prevPrev;
            Entry cur = prev;
            Entry next = dataSet.getEntryForIndex(minx + 1);

            // let the spline start
            path.moveTo(cur.getXIndex(), cur.getVal() * phaseY);

            for (int j = minx + 1, count = Math.min(size, entryCount); j < count; j++) {

                prevPrev = dataSet.getEntryForIndex(j == 1 ? 0 : j - 2);
                prev = dataSet.getEntryForIndex(j - 1);
                cur = dataSet.getEntryForIndex(j);
                next = entryCount > j + 1 ? dataSet.getEntryForIndex(j + 1) : cur;

                prevDx = (cur.getXIndex() - prevPrev.getXIndex()) * intensity;
                prevDy = (cur.getVal() - prevPrev.getVal()) * intensity;
                curDx = (next.getXIndex() - prev.getXIndex()) * intensity;
                curDy = (next.getVal() - prev.getVal()) * intensity;

                path.cubicTo(prev.getXIndex() + prevDx, (prev.getVal() + prevDy) * phaseY,
                        cur.getXIndex() - curDx,
                        (cur.getVal() - curDy) * phaseY, cur.getXIndex(), cur.getVal() * phaseY);

                lastPoint.set(cur.getXIndex(), cur.getVal() * phaseY);
            }
        }

        return lastPoint;
    }



    @Override
    protected void drawCubicFill(Canvas c, ILineDataSet dataSet, Path spline, Transformer trans, int from, int to) {
        super.drawCubicFill(c, dataSet, spline, trans, from, to);
    }
}
