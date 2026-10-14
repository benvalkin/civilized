package com.uncreated.civilized.core.building.bounds.detection;

import java.util.function.Supplier;

import javax.annotation.Nullable;

public enum BuildingDetectionType {
   ROOMS(BoundsDetector::new), PEN(PenDetector::new), MANUAL_DRAG(null);

   private final @Nullable Supplier<BoundsDetector> detector;

   BuildingDetectionType(@Nullable Supplier<BoundsDetector> detector) {
      this.detector = detector;
   }

   /** Whether the bounds are automatically detected from a starting block, or if they need to be manually specified. */
   public boolean autoDetected() {
      return detector != null;
   }

   public BoundsDetector createDetector() {
      if (detector == null)
         throw new IllegalStateException(this + " bounds aren't detected. They're dragged out by the player.");

      return detector.get();
   }
}
