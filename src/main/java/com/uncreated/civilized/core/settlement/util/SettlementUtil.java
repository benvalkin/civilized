package com.uncreated.civilized.core.settlement.util;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.settlement.Settlement;

import net.minecraft.world.phys.AABB;

public class SettlementUtil {
   public static Optional<Settlement> findExtendedEncapsulating(
         List<Settlement> inThisDimension,
         AABB bounds,
         int boundsSearchExtension) {
      return inThisDimension.stream()
            .filter(s -> s.getBounds().getEncapsulatingAABB().inflate(boundsSearchExtension).intersects(bounds))
            .findFirst();
   }

}
