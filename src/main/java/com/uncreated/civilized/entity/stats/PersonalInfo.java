package com.uncreated.civilized.entity.stats;

import com.uncreated.civilized.core.villagerinfo.Gender;

import com.uncreated.civilized.entity.CivilizedVillager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public class PersonalInfo {
   private static final String FIELD_CULTURE_NAME = "civilized_culture_name";
   private static final String FIELD_SKIN_TEXTURE = "civilized_culture_name";
   private Gender gender;
   private String cultureName = "default";
   private int skinIndex = 0;

   public PersonalInfo(CivilizedVillager villager) {
      RandomSource consistentRandom = villager.perLifetimeRandom();
      skinIndex = consistentRandom.nextInt();
   }

   public void loadFromNbt(CompoundTag tag) {
      tag.getString(FIELD_CULTURE_NAME);
      tag.getInt(FIELD_SKIN_TEXTURE);
   }

   public void saveToNbt(CompoundTag tag) {
      tag.putString(FIELD_CULTURE_NAME, cultureName);
      tag.putInt(FIELD_SKIN_TEXTURE, skinIndex);
   }
}
