package com.github.mikephil.charting.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.renderer.BandLineChartRenderer;

/**
 * Created by simon.hopkin on 24/05/2016.
 */
public class BandLineChart extends LineChart {

    public BandLineChart(Context context) {
        super(context);
    }

    public BandLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BandLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void init() {
        super.init();

        mRenderer = new BandLineChartRenderer(this, mAnimator, mViewPortHandler);

    }

    @Override
    protected void calcMinMax() {
        super.calcMinMax();

        if (mXAxis.mAxisRange == 0.0 && mData.getYValCount() > 0) {
            mXAxis.mAxisRange = 1.0f;
        }
    }
}
