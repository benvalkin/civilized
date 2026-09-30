package com.uncreated.civilized.core.trading;

import net.minecraft.world.item.ItemStack;

public record ItemTraded(ItemStack itemStack, int newVendorAvailableCurrency, int currencyFailedToTransfer) {
}
