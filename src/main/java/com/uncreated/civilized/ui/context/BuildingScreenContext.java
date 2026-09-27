package com.uncreated.civilized.ui.context;

import java.util.Collection;
import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.permission.PlayerPermission;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record BuildingScreenContext(Building building, Settlement settlement, CompoundTag additionalData,
      HolderLookup.Provider registryAccess) {

   private static final String KEY_SETTLEMENT_PERMISSIONS = "settlement_permissions";

   /** Only the server knows about permissions, so it sends them along whenever a building screen is opened. */
   public static void writeSettlementPermissions(CompoundTag additionalData, Collection<PlayerPermission> permissions) {
      additionalData.put(KEY_SETTLEMENT_PERMISSIONS, PlayerPermission.toNbtList(permissions));
   }

   /** Who has access to the settlement, as of when the screen opened or the server last sent an update. */
   public List<PlayerPermission> settlementPermissions() {
      return PlayerPermission.fromNbtList(additionalData.getList(KEY_SETTLEMENT_PERMISSIONS, Tag.TAG_COMPOUND));
   }

   public void setSettlementPermissions(Collection<PlayerPermission> permissions) {
      writeSettlementPermissions(additionalData, permissions);
   }
}
