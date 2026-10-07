package com.ai.vision.infra.onnx;

import com.ai.vision.domain.model.Detection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class YoloNonMaxSuppression {

  private YoloNonMaxSuppression() {}

  /** Removes boxes that overlap a stronger box. */
  static List<Detection> apply(List<Detection> candidates, float nmsThreshold) {
    List<Detection> sorted = new ArrayList<>(candidates);
    sorted.sort(Comparator.comparingDouble(Detection::getConfidence).reversed());

    List<Detection> kept = new ArrayList<>();
    boolean[] suppressed = new boolean[sorted.size()];

    for (int i = 0; i < sorted.size(); i++) {
      if (suppressed[i]) {
        continue;
      }
      Detection current = sorted.get(i);
      kept.add(current);
      for (int j = i + 1; j < sorted.size(); j++) {
        if (!suppressed[j] && calculateIou(current, sorted.get(j)) > nmsThreshold) {
          suppressed[j] = true;
        }
      }
    }
    return kept;
  }

  private static float calculateIou(Detection a, Detection b) {
    double x1 = Math.max(a.getLeft(), b.getLeft());
    double y1 = Math.max(a.getTop(), b.getTop());
    double x2 = Math.min(a.getLeft() + a.getWidth(), b.getLeft() + b.getWidth());
    double y2 = Math.min(a.getTop() + a.getHeight(), b.getTop() + b.getHeight());

    double intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
    double union = a.getWidth() * a.getHeight() + b.getWidth() * b.getHeight() - intersection;
    if (union <= 0) {
      return 0f;
    }
    return (float) (intersection / union);
  }
}
