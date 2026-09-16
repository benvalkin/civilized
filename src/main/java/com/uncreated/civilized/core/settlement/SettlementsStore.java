package com.uncreated.civilized.core.settlement;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.SavedData;

public abstract class SettlementsStore extends SavedData {

   protected static final Logger LOGGER = LogUtils.getLogger();

   protected SettlementDB settlements;

   protected SettlementsStore() {
      settlements = new SettlementDB();
   }

   public ImmutableList<Settlement> all() {
      return ImmutableList.copyOf(settlements.all());
   }

   public Settlement createNew(UUID ownerUUID, BlockPos origin) {
      Settlement settlement =
            new Settlement.SettlementBuilder().settlementId(UUID.randomUUID())
                  .ownerId(ownerUUID)
                  .displayName(Settlement.generateRandomName())
                  .bounds(new SettlementBounds(origin))
                  .build();

      settlements.add(settlement);
      setDirty();
      return settlement;
   }

   public Optional<Settlement> find(UUID settlementId) {
      return settlements.find(settlementId);
   }

   public Settlement get(UUID settlementId) {
      return find(settlementId).orElseThrow();
   }

   public Optional<Settlement> findFromOwner(UUID ownerId) {
      return settlements.all().stream().filter(s -> s.getOwnerId().equals(ownerId)).findFirst();
   }

   public Settlement getFromOwner(UUID ownerId) {
      return findFromOwner(ownerId).orElseThrow();
   }
}
