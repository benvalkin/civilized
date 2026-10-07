package com.uncreated.civilized.core.settlement;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;


@Getter
@Setter
public class SettlementState {

   public static final long NOT_SCHEDULED = -1;

   private static final String FIELD_NEXT_TAX_COLLECTION_DAY = "next_tax_collection_day";

   /** The in-game day (counted from the world's first day) that taxes are next collected on. */
   private long nextTaxCollectionDay = NOT_SCHEDULED;

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putLong(FIELD_NEXT_TAX_COLLECTION_DAY, nextTaxCollectionDay);
      return tag;
   }

   public static SettlementState fromNbt(CompoundTag tag) {
      SettlementState data = new SettlementState();
      if (tag.contains(FIELD_NEXT_TAX_COLLECTION_DAY))
         data.nextTaxCollectionDay = tag.getLong(FIELD_NEXT_TAX_COLLECTION_DAY);
      return data;
   }
}
