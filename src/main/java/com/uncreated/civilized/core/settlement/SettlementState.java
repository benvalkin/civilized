package com.uncreated.civilized.core.settlement;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

@Getter
@Setter
public class SettlementState {

   public static final long NOT_SCHEDULED = -1;

   private static final String FIELD_NEXT_TAX_COLLECTION_DAY = "next_tax_collection_day";
   private static final String FIELD_LAST_FESTIVAL_TIME = "last_festival_time";
   private static final String FIELD_FESTIVALS_HELD_COUNT = "festivals_held_count";
   private static final String FIELD_FESTIVAL_START_TIME = "festival_start_time";

   private long nextTaxCollectionDay = NOT_SCHEDULED;
   private long lastFestivalDay = NOT_SCHEDULED;
   private int festivalsHeldCount = 0;
   private long festivalStartTime = NOT_SCHEDULED;

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putLong(FIELD_NEXT_TAX_COLLECTION_DAY, nextTaxCollectionDay);
      tag.putLong(FIELD_LAST_FESTIVAL_TIME, lastFestivalDay);
      tag.putInt(FIELD_FESTIVALS_HELD_COUNT, festivalsHeldCount);
      tag.putLong(FIELD_FESTIVAL_START_TIME, festivalStartTime);
      return tag;
   }

   public static SettlementState fromNbt(CompoundTag tag) {
      SettlementState data = new SettlementState();
      if (tag.contains(FIELD_NEXT_TAX_COLLECTION_DAY))
         data.nextTaxCollectionDay = tag.getLong(FIELD_NEXT_TAX_COLLECTION_DAY);
      if (tag.contains(FIELD_LAST_FESTIVAL_TIME))
         data.lastFestivalDay = tag.getLong(FIELD_LAST_FESTIVAL_TIME);
      if (tag.contains(FIELD_FESTIVALS_HELD_COUNT))
         data.festivalsHeldCount = tag.getInt(FIELD_FESTIVALS_HELD_COUNT);
      if (tag.contains(FIELD_FESTIVAL_START_TIME))
         data.festivalStartTime = tag.getLong(FIELD_FESTIVAL_START_TIME);
      return data;
   }
}
