package com.uncreated.civilized.core.building.bounds.detection;

import java.util.function.Supplier;

public enum BuildingDetectionType {
   ROOMS(BoundsDetector::new),
   PEN(PenDetector::new);

   private final Supplier<BoundsDetector> detector;

   BuildingDetectionType(Supplier<BoundsDetector> detector) {
      this.detector = detector;
   }

   public BoundsDetector createDetector() {
      return detector.get();
   }
}
