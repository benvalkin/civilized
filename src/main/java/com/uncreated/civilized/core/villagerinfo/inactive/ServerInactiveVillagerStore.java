package com.uncreated.civilized.core.villagerinfo.inactive;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public abstract class ServerInactiveVillagerStore<V extends InactiveVillagerRecord, DB extends InactiveVillagerDB<V>>
      extends InactiveVillagerStore<V, DB> {

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registryAccess) {

      ListTag tags = new ListTag();
      for (InactiveVillagerRecord record : villagers.all()) {
         CompoundTag recordTag = record.toNbt(registryAccess);
         tags.add(recordTag);
      }

      tag.put(getStorageFileName(), tags);
      return tag;
   }

   protected void loadIntl(CompoundTag tag, HolderLookup.Provider registryAccess) {
      ListTag list = tag.getList(getStorageFileName(), Tag.TAG_COMPOUND);
      for (Tag i : list) {
         if (!(i instanceof CompoundTag recordTag)) {
            continue;
         }

         V record = recordFromNbt(recordTag, registryAccess);
         villagers.add(record);
      }
   }

   protected abstract @NotNull V recordFromNbt(CompoundTag recordTag, HolderLookup.Provider registryAccess);

   protected abstract String getStorageFileName();

   public V add(V record) {
      villagers.add(record);
      setDirty();
      return record;
   }

   public Optional<V> remove(UUID villagerId) {
      Optional<V> removed = villagers.remove(villagerId);
      if (removed.isPresent())
         setDirty();

      return removed;
   }
}
