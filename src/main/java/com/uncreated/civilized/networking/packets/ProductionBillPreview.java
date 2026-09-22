package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.core.building.production.bills.RecipeAllowed;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.EditProductionBillTab;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The server's answer to {@link PreviewProductionBill}, containing bill's inputs and whether the recipe is allowed.
 */
public record ProductionBillPreview(int sequence, ItemStack resultItem, RecipeAllowed recipeAllowed)
      implements CustomPacketPayload {

   public static final Type<ProductionBillPreview> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "production_bill_preview"));

   public static final StreamCodec<RegistryFriendlyByteBuf, ProductionBillPreview> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               ProductionBillPreview::sequence,
               ItemStack.OPTIONAL_STREAM_CODEC,
               ProductionBillPreview::resultItem,
               NeoForgeStreamCodecs.enumCodec(RecipeAllowed.class),
               ProductionBillPreview::recipeAllowed,
               ProductionBillPreview::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void clientReceiveProductionBillPreview(ProductionBillPreview packet, IPayloadContext context) {
      // the player may have left the edit tab or closed the screen while the server was answering
      if (Minecraft.getInstance().screen instanceof ABuildingMenuScreen screen
            && screen.getCurrentTab() instanceof EditProductionBillTab editTab)
         editTab.receivePreview(packet.sequence, packet.resultItem, packet.recipeAllowed);
   }
}
