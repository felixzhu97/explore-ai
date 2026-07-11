package com.ai.vision.infrastructure.adapter;

import com.ai.vision.domain.model.Detection;
import com.ai.vision.infrastructure.config.VisionModelProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class YoloNonMaxSuppression {

    private YoloNonMaxSuppression() {}

    static List<Detection> apply(List<Detection> candidates, float nmsThreshold) {
        List<Detection> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble(Detection::confidence).reversed());

        List<Detection> kept = new ArrayList<>();
        boolean[] suppressed = new boolean[sorted.size()];

        for (int i = 0; i < sorted.size(); i++) {
            if (suppressed[i]) {
                continue;
            }
            Detection current = sorted.get(i);
            kept.add(current);
            for (int j = i + 1; j < sorted.size(); j++) {
                if (!suppressed[j] && iou(current, sorted.get(j)) > nmsThreshold) {
                    suppressed[j] = true;
                }
            }
        }
        return kept;
    }

    private static float iou(Detection a, Detection b) {
        double x1 = Math.max(a.x(), b.x());
        double y1 = Math.max(a.y(), b.y());
        double x2 = Math.min(a.x() + a.width(), b.x() + b.width());
        double y2 = Math.min(a.y() + a.height(), b.y() + b.height());

        double intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        double union = a.width() * a.height() + b.width() * b.height() - intersection;
        if (union <= 0) {
            return 0f;
        }
        return (float) (intersection / union);
    }
}
