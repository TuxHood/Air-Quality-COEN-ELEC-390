package com.example.ui_coen390;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollutantAdapter extends RecyclerView.Adapter<PollutantAdapter.PollutantViewHolder> {

    private List<Pollutant> pollutants;

    // thresholds used to normalize values to 0-100% for the gauge
    private static final Map<String, Float> THRESHOLDS = new HashMap<>();
    static {
        THRESHOLDS.put("CO2", 2000f);
        THRESHOLDS.put("TVOC", 660f);
        THRESHOLDS.put("CO", 9f);
        THRESHOLDS.put("Propane", 2100f);
        THRESHOLDS.put("Smoke", 150f);
        THRESHOLDS.put("Methane", 1000f);
        THRESHOLDS.put("Alcohol", 1000f);
        THRESHOLDS.put("H2", 4100f);
        THRESHOLDS.put("AQI", 500f); // AQI scale
    }

    public PollutantAdapter(List<Pollutant> pollutants) {
        this.pollutants = pollutants;
    }

    @NonNull
    @Override
    public PollutantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pollutant_item, parent, false);
        return new PollutantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PollutantViewHolder holder, int position) {
        Pollutant pollutant = pollutants.get(position);
        holder.pollutantNameTextView.setText(pollutant.getName());

        Context ctx = holder.itemView.getContext();
        SharedPreferences prefs = ctx.getSharedPreferences("display_prefs", Context.MODE_PRIVATE);
        String mode = prefs.getString("displayMode", "advanced");

        if ("advanced".equals(mode)) {
            // Show gauge (naming swapped: 'advanced' now displays the gauge)
            holder.pollutantValueTextView.setVisibility(View.GONE);
            holder.gaugeContainer.setVisibility(View.VISIBLE);

            // parse numeric value if present
            float numeric = parseLeadingNumber(pollutant.getValue());
            float threshold = THRESHOLDS.containsKey(pollutant.getName()) ? THRESHOLDS.get(pollutant.getName()) : 100f;
            float percent = 0f;
            if (!Float.isNaN(numeric) && threshold > 0f) {
                percent = (numeric / threshold) * 100f;
            }
            if (percent < 0f) percent = 0f;
            if (percent > 100f) percent = 100f;

            // compute width in px for fill based on gauge container width dp (160dp -> background width match_parent)
            int maxDp = 160; // parent width
            int maxPx = dpToPx(ctx, maxDp);
            int fillPx = Math.round((percent / 100f) * maxPx);

            // Update fill width and color
            ViewGroup.LayoutParams lp = holder.gaugeFill.getLayoutParams();
            lp.width = fillPx;
            holder.gaugeFill.setLayoutParams(lp);

            int color = percentToColor(percent);
            holder.gaugeFill.setBackgroundColor(color);

            // Update legend to show tuning hint and absolute threshold
            holder.gaugeLegend.setText(String.format("0–%.0f (Good→Hazardous)", threshold));

        } else {
            // Advanced mode: show number text
            holder.gaugeContainer.setVisibility(View.GONE);
            holder.pollutantValueTextView.setVisibility(View.VISIBLE);
            holder.pollutantValueTextView.setText(pollutant.getValue());
        }
    }

    @Override
    public int getItemCount() {
        return pollutants.size();
    }

    private static float parseLeadingNumber(String s) {
        if (s == null) return Float.NaN;
        s = s.trim();
        try {
            // take leading token that may contain number
            StringBuilder sb = new StringBuilder();
            boolean dotSeen = false;
            boolean minusSeen = false;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || (c == '.' && !dotSeen) || (c == '-' && !minusSeen)) {
                    sb.append(c);
                    if (c == '.') dotSeen = true;
                    if (c == '-') minusSeen = true;
                } else {
                    break;
                }
            }
            if (sb.length() == 0) return Float.NaN;
            return Float.parseFloat(sb.toString());
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    private static int dpToPx(Context c, int dp) {
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        return Math.round(dp * (metrics.densityDpi / 160f));
    }

    private static int percentToColor(float percent) {
        // 0 -> green, 50 -> yellow, 100 -> red
        int green = Color.parseColor("#4CAF50");
        int yellow = Color.parseColor("#FFEB3B");
        int red = Color.parseColor("#F44336");
        if (percent <= 50f) {
            float t = percent / 50f;
            return interpolateColor(green, yellow, t);
        } else {
            float t = (percent - 50f) / 50f;
            return interpolateColor(yellow, red, t);
        }
    }

    private static int interpolateColor(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a1 = (c1 >> 24) & 0xff;
        int r1 = (c1 >> 16) & 0xff;
        int g1 = (c1 >> 8) & 0xff;
        int b1 = (c1) & 0xff;

        int a2 = (c2 >> 24) & 0xff;
        int r2 = (c2 >> 16) & 0xff;
        int g2 = (c2 >> 8) & 0xff;
        int b2 = (c2) & 0xff;

        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }

    public static class PollutantViewHolder extends RecyclerView.ViewHolder {
        TextView pollutantNameTextView;
        TextView pollutantValueTextView;
        LinearLayout gaugeContainer;
        FrameLayout gaugeBackground;
        View gaugeFill;
        TextView gaugeLegend;

        public PollutantViewHolder(@NonNull View itemView) {
            super(itemView);
            pollutantNameTextView = itemView.findViewById(R.id.pollutantNameTextView);
            pollutantValueTextView = itemView.findViewById(R.id.pollutantValueTextView);
            gaugeContainer = itemView.findViewById(R.id.gaugeContainer);
            gaugeBackground = itemView.findViewById(R.id.gaugeBackground);
            gaugeFill = itemView.findViewById(R.id.gaugeFill);
            gaugeLegend = itemView.findViewById(R.id.gaugeLegend);
        }
    }
}
