package com.uncreated.civilized.core.villagerinfo.inactive.visitor;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.inactive.InactiveVillagerRecord;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class RecurringVisitorRecord extends InactiveVillagerRecord {
   public static final String FIELD_DEPARTED_DAY = "departed_day";
   public static final String FIELD_ACCRUED_HOSPITALITY = "accrued_hospitality";
   private final long departedDay;
   private final float accruedHospitality;

   public RecurringVisitorRecord(VillagerInfo info, long departedDay, float accruedHospitality) {
      super(info);
      this.departedDay = departedDay;
      this.accruedHospitality = accruedHospitality;
   }

   public RecurringVisitorRecord(CompoundTag recordTag, HolderLookup.Provider registryAccess) {
      super(recordTag, registryAccess);
      departedDay = recordTag.getLong(FIELD_DEPARTED_DAY);
      accruedHospitality = recordTag.getFloat(FIELD_ACCRUED_HOSPITALITY);
   }

   @Override
   protected void writeAdditionalData(CompoundTag additionalDataTag, HolderLookup.Provider registryAccess) {
      additionalDataTag.putLong(FIELD_DEPARTED_DAY, departedDay);
      additionalDataTag.putFloat(FIELD_DEPARTED_DAY, accruedHospitality);
   }
}
