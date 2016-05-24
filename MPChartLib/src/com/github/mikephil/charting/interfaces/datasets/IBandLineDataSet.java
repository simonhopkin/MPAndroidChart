package com.github.mikephil.charting.interfaces.datasets;

import com.github.mikephil.charting.data.LineDataSet;

/**
 * Created by simon.hopkin on 24/05/2016.
 */
public interface IBandLineDataSet extends ILineDataSet {

    LineDataSet getTopDataSet();
    LineDataSet getBottomDataSet();
}
