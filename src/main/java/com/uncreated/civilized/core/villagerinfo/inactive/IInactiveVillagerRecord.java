package com.uncreated.civilized.core.villagerinfo.inactive;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface IInactiveVillagerRecord {
   VillagerInfo getInfo();

   @NotNull
   CompoundTag toNbt(HolderLookup.Provider registryAccess);
}
