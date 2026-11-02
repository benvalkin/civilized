package com.uncreated.civilized.core.villagerinfo.inactive.visitor;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.inactive.InactiveVillagerRecord;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

@Getter
public class RecurringVisitorRecord extends InactiveVillagerRecord {
   public static final String FIELD_DEPARTED_DAY = "departed_day";
   public static final String FIELD_ACCRUED_HOSPITALITY = "accrued_hospitality";
   public static final String FIELD_VISITS = "visits";
   private final long departedDay;
   private final float accruedHospitality;
   /** How many times they've visited so far. */
   private final int visits;

   public RecurringVisitorRecord(VillagerInfo info, long departedDay, float accruedHospitality, int visits) {
      super(info);
      this.departedDay = departedDay;
      this.accruedHospitality = accruedHospitality;
      this.visits = visits;
   }

   public RecurringVisitorRecord(CompoundTag recordTag, HolderLookup.Provider registryAccess) {
      super(recordTag, registryAccess);
      departedDay = recordTag.getLong(FIELD_DEPARTED_DAY);
      accruedHospitality = recordTag.getFloat(FIELD_ACCRUED_HOSPITALITY);
      visits = recordTag.getInt(FIELD_VISITS);
   }

   @Override
   protected void writeAdditionalData(CompoundTag additionalDataTag, HolderLookup.Provider registryAccess) {
      additionalDataTag.putLong(FIELD_DEPARTED_DAY, departedDay);
      additionalDataTag.putFloat(FIELD_ACCRUED_HOSPITALITY, accruedHospitality);
      additionalDataTag.putInt(FIELD_VISITS, visits);
   }
}
