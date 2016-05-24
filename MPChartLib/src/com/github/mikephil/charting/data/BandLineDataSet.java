package com.github.mikephil.charting.data;

import com.github.mikephil.charting.interfaces.datasets.IBandLineDataSet;

import java.util.List;

/**
 * Created by simon.hopkin on 24/05/2016.
 */
public class BandLineDataSet extends LineDataSet implements IBandLineDataSet {

    private LineDataSet topDataSet;
    private LineDataSet bottomDataSet;

    public BandLineDataSet(List<Entry> yVals, String label) {
        super(yVals, label);
    }

    public BandLineDataSet(LineDataSet topDataSet, LineDataSet bottomDataSet) {
        super(null, null);
        this.topDataSet = topDataSet;
        this.bottomDataSet = bottomDataSet;
    }

    @Override
    public LineDataSet getTopDataSet() {
        return topDataSet;
    }

    @Override
    public LineDataSet getBottomDataSet() {
        return bottomDataSet;
    }
}
