package com.uncreated.civilized.core.building.production.bills;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class NbtHelper {
    public static List<ItemStack> readItemList(ListTag listTag, HolderLookup.Provider registryAccess) {
        List<ItemStack> result = new ArrayList<>();
        for (Tag tag : listTag) {
            if (!(tag instanceof CompoundTag itemTag))
                continue;

            ItemStack itemStack = ItemStack.parseOptional(registryAccess, itemTag);
            result.add(itemStack);
        }
        return result;
    }

    public static ListTag writeItemList(List<ItemStack> items, HolderLookup.Provider registryAccess) {
        ListTag list = new ListTag();
        for (ItemStack item : items) {
            list.add(item.saveOptional(registryAccess));
        }

        return list;
    }
}
