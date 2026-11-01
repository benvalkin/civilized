package com.uncreated.civilized.core.villagerinfo.inactive;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;

import net.minecraft.world.level.saveddata.SavedData;

public abstract class InactiveVillagerStore<V extends IInactiveVillagerRecord, DB extends InactiveVillagerDB<V>>
      extends SavedData {

   protected static final Logger LOGGER = LogUtils.getLogger();

   protected DB villagers;

   protected InactiveVillagerStore() {
      villagers = createDB();
   }

   public ImmutableList<V> all() {
      return villagers.all();
   }

   public Optional<V> find(UUID villagerId) {
      return villagers.find(villagerId);
   }

   public V get(UUID villagerId) {
      return villagers.get(villagerId);
   }

   protected abstract @NotNull DB createDB();
}
