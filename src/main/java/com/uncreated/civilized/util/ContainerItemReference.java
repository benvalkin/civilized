package com.uncreated.civilized.util;

import net.minecraft.world.item.ItemStack;

public record ContainerItemReference(
    ItemStack item,
    int container,
    int slot) {
}
