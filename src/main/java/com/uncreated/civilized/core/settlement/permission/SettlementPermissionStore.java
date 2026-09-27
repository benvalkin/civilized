package com.uncreated.civilized.core.settlement.permission;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;

import net.minecraft.world.level.saveddata.SavedData;

public abstract class SettlementPermissionStore extends SavedData {

   protected static final Logger LOGGER = LogUtils.getLogger();

   protected final SettlementPermissionsDB permissions = new SettlementPermissionsDB();

   public Optional<SettlementPermissions> find(UUID settlementId) {
      return permissions.find(settlementId);
   }

   public abstract SettlementPermissions getOrCreate(UUID settlementId);

   public ImmutableList<SettlementPermissions> all() {
      return permissions.all();
   }
}
