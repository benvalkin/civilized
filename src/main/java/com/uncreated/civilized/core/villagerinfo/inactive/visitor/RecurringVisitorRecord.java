package com.uncreated.civilized.core.villagerinfo.inactive.visitor;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.inactive.InactiveVillagerRecord;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class RecurringVisitorRecord extends InactiveVillagerRecord {
   public static final String FIELD_DEPARTED_DAY = "departed_day";
   private final long departedDay;

   public RecurringVisitorRecord(VillagerInfo info, long departedDay) {
      super(info);
      this.departedDay = departedDay;
   }

   public RecurringVisitorRecord(CompoundTag recordTag, HolderLookup.Provider registryAccess) {
      super(recordTag, registryAccess);
      departedDay = recordTag.getLong(FIELD_DEPARTED_DAY);
   }

   @Override
   protected void writeAdditionalData(CompoundTag additionalDataTag, HolderLookup.Provider registryAccess) {
      additionalDataTag.putLong(FIELD_DEPARTED_DAY, departedDay);
   }
}
