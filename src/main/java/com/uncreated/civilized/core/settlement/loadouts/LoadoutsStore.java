package com.uncreated.civilized.core.settlement.loadouts;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;

import net.minecraft.world.level.saveddata.SavedData;

public abstract class LoadoutsStore extends SavedData {

   protected static final Logger LOGGER = LogUtils.getLogger();

   protected final LoadoutsDB loadouts = new LoadoutsDB();

   public Optional<Loadout> find(UUID loadoutId) {
      return loadouts.find(loadoutId);
   }

   public ImmutableList<Loadout> all() {
      return loadouts.all();
   }
}
