package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent to the server to ask what a production bill with these input items would make while the player is editing the recipe.
 * Answered with {@link ProductionBillPreview}.
 *
 * @param sequence
 *           echoed back in the answer, so the client can ignore answers to inputs it has since changed
 */
public record PreviewProductionBill(UUID buildingId, ProductionType productionType, List<ItemStack> inputs, int sequence)
      implements CustomPacketPayload {

   public static final Type<PreviewProductionBill> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "preview_production_bill"));

   public static final StreamCodec<RegistryFriendlyByteBuf, PreviewProductionBill> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               PreviewProductionBill::buildingId,
               ProductionTypes.STREAM_CODEC,
               PreviewProductionBill::productionType,
               ItemStack.OPTIONAL_LIST_STREAM_CODEC,
               PreviewProductionBill::inputs,
               ByteBufCodecs.VAR_INT,
               PreviewProductionBill::sequence,
               PreviewProductionBill::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceivePreviewProductionBill(PreviewProductionBill packet, IPayloadContext context) {

      Optional<ProductionBillEditValidation.EditableArtisanHouse> artisanHouse =
            ProductionBillEditValidation
                  .findArtisanHouseForRecipeValidation(packet.buildingId, packet.productionType, packet.inputs, context);
      if (artisanHouse.isEmpty() || !(context.player() instanceof ServerPlayer serverPlayer))
         return;

      ArtisanHouseState.RecipeEvaluation evaluation =
            artisanHouse.get()
                  .state()
                  .serverEvaluateRecipe(
                        packet.productionType,
                        ProductionBillEditValidation.singleItems(packet.inputs),
                        serverPlayer.serverLevel());

      PacketDistributor.sendToPlayer(
            serverPlayer,
            new ProductionBillPreview(packet.sequence, evaluation.resultItem(), evaluation.recipeAllowed()));
   }
}
