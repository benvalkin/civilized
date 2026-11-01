package com.uncreated.civilized.core.villagerinfo.inactive;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

@Getter
public class InactiveVillagerRecord implements IInactiveVillagerRecord {

   public static final String FIELD_VILLAGER_INFO = "villager_info";
   public static final String FIELD_ADDITIONAL_DATA = "additional_data";

   private final VillagerInfo info;

   public InactiveVillagerRecord(VillagerInfo info) {
      this.info = info;
   }

   public InactiveVillagerRecord(CompoundTag recordTag, HolderLookup.Provider registryAccess) {
      this.info = VillagerInfo.fromNbt(recordTag.getCompound(FIELD_VILLAGER_INFO), registryAccess);
   }

   public final @NotNull CompoundTag toNbt(HolderLookup.Provider registryAccess) {
      CompoundTag tag = new CompoundTag();
      tag.put(FIELD_VILLAGER_INFO, info.toNbt(registryAccess));
      writeAdditionalData(tag, registryAccess);
      return tag;
   }

   protected void writeAdditionalData(CompoundTag additionalDataTag, HolderLookup.Provider registryAccess) {

   }
}
