package com.example.ui_coen390;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.Viewport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateAsXAxisLabelFormatter extends DefaultLabelFormatter {
    private final SimpleDateFormat mFormatSeconds = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private final SimpleDateFormat mFormatMinutes = new SimpleDateFormat("HH:mm", Locale.US);
    private final SimpleDateFormat mFormatHours = new SimpleDateFormat("HH:mm", Locale.US);
    private final Viewport mViewport;

    public DateAsXAxisLabelFormatter(Viewport viewport) {
        mViewport = viewport;
    }

    @Override
    public String formatLabel(double value, boolean isValueX) {
        if (isValueX) {
            long timeInMillis = (long) value;
            Date date = new Date(timeInMillis);

            // Decide the format based on the range of the viewport
            double range = mViewport.getMaxX(false) - mViewport.getMinX(false);

            if (range < 60 * 1000) { // Less than a minute
                return mFormatSeconds.format(date);
            } else if (range < 60 * 60 * 1000) { // Less than an hour
                return mFormatMinutes.format(date);
            } else {
                return mFormatHours.format(date);
            }
        } else {
            return super.formatLabel(value, isValueX);
        }
    }
}
