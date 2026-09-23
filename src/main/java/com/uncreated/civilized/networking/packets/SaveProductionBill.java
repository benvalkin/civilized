package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.production.bills.ItemFilters;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionRecipe;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.core.building.production.bills.RecipeAllowed;
import com.uncreated.civilized.core.building.production.bills.strategy.ProductionStrategyType;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Saves a production bill the player finished editing. The server looks up the recipe again and checks the house may
 * make it, rather than trusting what the client was shown.
 *
 * @param billIndex
 *           the bill being replaced, or {@link #NEW_BILL} to add a new one
 * @param amount
 *           how many to keep in stock, only used by strategies that need an amount
 */
public record SaveProductionBill(UUID buildingId, ProductionType productionType, int billIndex, List<ItemStack> inputs,
      ItemFilters ingredientFilters, ProductionStrategyType strategy, int amount, boolean enabled)
      implements CustomPacketPayload {

   public static final int NEW_BILL = -1;

   private static final int MAX_AMOUNT = 999;

   public static final Type<SaveProductionBill> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "save_production_bill"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SaveProductionBill> STREAM_CODEC =
         StreamCodec.of((buffer, packet) -> packet.encode(buffer), SaveProductionBill::decode);

   private void encode(RegistryFriendlyByteBuf buffer) {
      UUIDUtil.STREAM_CODEC.encode(buffer, buildingId);
      ProductionTypes.STREAM_CODEC.encode(buffer, productionType);
      ByteBufCodecs.VAR_INT.encode(buffer, billIndex);
      ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buffer, inputs);
      ItemFilters.STREAM_CODEC.encode(buffer, ingredientFilters);
      NeoForgeStreamCodecs.enumCodec(ProductionStrategyType.class).encode(buffer, strategy);
      ByteBufCodecs.VAR_INT.encode(buffer, amount);
      ByteBufCodecs.BOOL.encode(buffer, enabled);
   }

   private static SaveProductionBill decode(RegistryFriendlyByteBuf buffer) {
      return new SaveProductionBill(
            UUIDUtil.STREAM_CODEC.decode(buffer),
            ProductionTypes.STREAM_CODEC.decode(buffer),
            ByteBufCodecs.VAR_INT.decode(buffer),
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buffer),
            ItemFilters.STREAM_CODEC.decode(buffer),
            NeoForgeStreamCodecs.enumCodec(ProductionStrategyType.class).decode(buffer),
            ByteBufCodecs.VAR_INT.decode(buffer),
            ByteBufCodecs.BOOL.decode(buffer));
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveSaveProductionBill(SaveProductionBill packet, IPayloadContext context) {

      Optional<ProductionBillEditValidation.EditableArtisanHouse> artisanHouse =
            ProductionBillEditValidation.findArtisanHouseForRecipeValidation(
                  packet.buildingId,
                  packet.productionType,
                  packet.inputs,
                  context);
      if (artisanHouse.isEmpty() || !(context.player() instanceof ServerPlayer serverPlayer))
         return;

      ArtisanHouseState artisanHouseState = artisanHouse.get().state();
      List<ProductionBill> existingBills = artisanHouseState.getProductionBills(packet.productionType);
      boolean isNewBill = packet.billIndex == NEW_BILL;
      if (!isNewBill && (packet.billIndex < 0 || packet.billIndex >= existingBills.size()))
         return;

      // a bill has one filter per recipe slot, so a packet with any other number of them is not for this recipe
      if (packet.ingredientFilters.size() != packet.productionType.recipeSlots().size())
         return;

      List<ItemStack> inputs = ProductionBillEditValidation.singleItems(packet.inputs);
      ItemFilters ingredientFilters = ProductionBillEditValidation.sanitized(packet.ingredientFilters);

      ArtisanHouseState.RecipeEvaluation evaluation =
            artisanHouseState.serverEvaluateRecipe(packet.productionType, inputs, serverPlayer.serverLevel());
      if (evaluation.recipeAllowed() != RecipeAllowed.ALLOWED)
         return;

      ProductionRecipe recipe = evaluation.recipe().orElseThrow();
      int amount = packet.strategy.requiresAmount() ? Math.clamp(packet.amount, 1, MAX_AMOUNT) : -1;

      ProductionBill bill =
            new ProductionBill(
                  recipe.recipe().id().location().toString(),
                  packet.productionType,
                  packet.strategy,
                  amount,
                  packet.enabled,
                  inputs,
                  ingredientFilters,
                  recipe.result());

      if (isNewBill)
         artisanHouseState.addBill(bill);
      else
         artisanHouseState.replaceBill(packet.billIndex, bill);

      ServerBuildingsStore.INSTANCE.replicateChange(artisanHouse.get().building(), StoreOperation.UPDATE);
      ServerBuildingsStore.INSTANCE.setDirty();
   }
}
