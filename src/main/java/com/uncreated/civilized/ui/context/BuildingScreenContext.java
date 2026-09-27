package com.uncreated.civilized.ui.context;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public record BuildingScreenContext(Building building, Settlement settlement, SettlementPermissions permissions,
      CompoundTag additionalData, HolderLookup.Provider registryAccess) {

}
