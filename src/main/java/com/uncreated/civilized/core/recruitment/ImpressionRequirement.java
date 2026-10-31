package com.uncreated.civilized.core.recruitment;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ImpressionRequirement {
   NOT_RECRUITABLE(Integer.MAX_VALUE), LOW(10), MID(50), HIGH(100);

   private final int impressionRequired;

   ImpressionRequirement(int impressionRequired) {
      this.impressionRequired = impressionRequired;
   }
}
