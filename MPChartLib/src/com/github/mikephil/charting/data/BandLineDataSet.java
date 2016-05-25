package com.github.mikephil.charting.data;

import android.graphics.Color;

import com.github.mikephil.charting.interfaces.datasets.IBandLineDataSet;

import java.util.List;

/**
 * Created by simon.hopkin on 24/05/2016.
 */
public class BandLineDataSet extends LineDataSet implements IBandLineDataSet {

    private LineDataSet topDataSet;
    private LineDataSet bottomDataSet;
    private int highlightOutlineColor;

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

    @Override
    public int getEntryCount() {
        return Math.max(topDataSet.getEntryCount(), bottomDataSet.getEntryCount());
    }

    public void setHighlightOutlineColor(int highlightOutlineColor) {
        this.highlightOutlineColor = highlightOutlineColor;
    }

    public int getHighlightOutlineColor() {
        return highlightOutlineColor;
    }

    @Override
    public float getYMax() {
        return Math.max(topDataSet.getYMax(), bottomDataSet.getYMax());
    }

    @Override
    public float getYMin() {
        return Math.min(topDataSet.getYMax(), bottomDataSet.getYMax());
    }
}
