package com.uncreated.civilized.ui.menu.building.residence.artisan;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.strategy.ProductionStrategyType;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.networking.packets.EditProductionBillUpdateState;
import com.uncreated.civilized.networking.packets.ShowBuildingScreen;
import com.uncreated.civilized.networking.packets.TellProductionBillRecipeAllowed;
import com.uncreated.civilized.ui.menu.building.item.management.ItemManagementMenu;
import com.uncreated.civilized.ui.menu.item.management.LegacyEyedropperSlot;
import com.uncreated.civilized.ui.menu.item.management.ReadonlySlot;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public abstract class EditRecipeMenu<TRecipe extends net.minecraft.world.item.crafting.Recipe<TRecipeInput>, TRecipeInput extends RecipeInput>
      extends ItemManagementMenu implements ContainerListener {

   private final int productionBillIndex;
   @Getter
   @Nullable
   private final ProductionBill existingBill;
   @Getter
   private final SimpleContainer resultContainer;
   @Getter
   private final ReadonlySlot outputSlot;

   @Nullable
   private final ServerPlayer player;

   @Nullable
   private ServerLevel serverLevel;

   @Nullable
   private RecipeHolder<TRecipe> recipe;
   private final boolean isNewBill;

   @Getter
   @Setter
   private int desiredProductionBillAmount;

   @Getter
   @Setter
   private ProductionStrategyType desiredProductionStrategyType;

   @Getter
   @Setter
   private boolean desiredProductionEnabled;
   private boolean recipeAllowed;

   // client constructor
   public EditRecipeMenu(
         MenuType<? extends ItemManagementMenu> menuType,
         int containerId,
         Inventory playerInventory,
         FriendlyByteBuf extraDataFromServer) {
      this(
            menuType,
            containerId,
            playerInventory,
            new SimpleContainer(extraDataFromServer.readInt()),
            ClientSettlementsStore.INSTANCE.get(extraDataFromServer.readUUID()),
            ClientBuildingStore.INSTANCE.get(extraDataFromServer.readUUID()),
            extraDataFromServer.readInt(),
            extraDataFromServer.readBoolean());
   }

   public EditRecipeMenu(
         MenuType<? extends ItemManagementMenu> menuType,
         int containerId,
         Inventory playerInventory,
         Container craftingMenuContainer,
         Settlement settlement,
         Building building,
         int productionBillIndex,
         boolean isNewBill) {
      super(menuType, containerId);
      this.container = craftingMenuContainer;
      this.resultContainer = new SimpleContainer(1);
      this.settlement = settlement;
      this.building = building;
      this.productionBillIndex = productionBillIndex;
      this.isNewBill = isNewBill;
      if (playerInventory.player instanceof ServerPlayer serverPlayer)
         this.player = serverPlayer;
      else
         this.player = null;

      if (playerInventory.player.level() instanceof ServerLevel sLevel)
         this.serverLevel = sLevel;

      List<LegacyEyedropperSlot> inputSlots = setupInputSlots(craftingMenuContainer);
      if (inputSlots.isEmpty())
         throw new IllegalStateException("Edit recipe menu needs at least 1 input slot.");

      inputSlots.forEach(this::addSlot);

      outputSlot = setupOutputSlot(resultContainer);
      this.addSlot(outputSlot);

      ArtisanHouseState artisanHouseState = (ArtisanHouseState) building.getState();

      recipeAllowed = false;
      if (isNewBill) {
         existingBill = null;
         outputSlot.set(ItemStack.EMPTY);

         desiredProductionBillAmount = -1;
         desiredProductionStrategyType = ProductionStrategyType.PRODUCE_INFINITE;
         desiredProductionEnabled = true;
      } else {
         existingBill = artisanHouseState.getProductionBills(getProductionType()).get(productionBillIndex);

         List<ItemStack> inputItems = existingBill.getInputItems();
         for (int i = 0; i < inputItems.size(); i++) {
            if (i >= slots.size())
               continue;

            slots.get(i).set(inputItems.get(i));
         }

         desiredProductionBillAmount = existingBill.getBillAmount();
         desiredProductionStrategyType = existingBill.getProductionStrategy().getType();
         desiredProductionEnabled = existingBill.isEnabled();

         outputSlot.set(existingBill.getDisplayItem());
      }

      this.addStandardInventorySlots(playerInventory, 87, 88);

      this.container.startOpen(playerInventory.player);

      if (serverLevel != null) {
         this.addSlotListener(this);
      }
   }

   protected abstract List<LegacyEyedropperSlot> setupInputSlots(Container craftingMenuContainer);

   protected abstract ReadonlySlot setupOutputSlot(Container resultSlotContainer);

   @Override
   public void slotsChanged(Container container) {
      super.slotsChanged(container);
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);

      if (!(player instanceof ServerPlayer serverPlayer))
         return;

      ArtisanHouseState artisanHouseState = (ArtisanHouseState) building.getState();

      if (recipe != null && recipeAllowed) {

         ProductionBill newBill =
               new ProductionBill(
                     recipe.id().location().getPath(),
                     getProductionType(),
                     desiredProductionStrategyType,
                     desiredProductionBillAmount,
                     desiredProductionEnabled, // note: currently no support for enabling production from this menu yet
                     this.getItems().subList(0, 9),
                     outputSlot.getItem());

         if (isNewBill)
            artisanHouseState.addBill(newBill);
         else {
            artisanHouseState.replaceBill(productionBillIndex, newBill);
         }

         ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
         ServerBuildingsStore.INSTANCE.setDirty();

         // BAD IMPLEMENTATION: this method is called when the player's menu closes (e.g. when escape is pressed), so
         // this
         // currently re-opens the UI when it shouldn't.
         CompoundTag additionalData = new CompoundTag();
         building.getState().serverAddToBuildingScreenContext(additionalData, serverPlayer.serverLevel());
         PacketDistributor.sendToPlayer(serverPlayer, new ShowBuildingScreen(building.getBuildingId(), additionalData));
      }
   }

   public abstract ProductionType getProductionType();

   @Override
   public void slotChanged(AbstractContainerMenu craftingContainerMenu, int i, ItemStack itemStack) {

      if (i >= 9) // crafting slots are always the first 9, we don't care about any others
         return;

      computeRecipeAndDisplay(craftingContainerMenu);
   }

   @Override
   public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int i1) {

   }

   private void computeRecipeAndDisplay(AbstractContainerMenu craftingContainerMenu) {

      Optional<Pair<RecipeHolder<TRecipe>, TRecipeInput>> recipeResult =
            getRecipeFromInputContainer(craftingContainerMenu.getItems(), serverLevel, serverLevel.recipeAccess());

      if (recipeResult.isEmpty()) {
         outputSlot.set(ItemStack.EMPTY);
         this.recipe = null;
         tellClientRecipeAllowed(RecipeAllowed.INVALID_RECIPE);
         return;
      }

      this.recipe = recipeResult.get().getFirst();
      TRecipeInput recipeInput = recipeResult.get().getSecond();
      ItemStack resultItem = this.recipe.value().assemble(recipeInput, serverLevel.registryAccess());

      outputSlot.set(resultItem);

      ArtisanHouseState artisanHouseState = (ArtisanHouseState) building.getState();
      recipeAllowed = artisanHouseState.recipeAllowed(getProductionType(), recipeInput, resultItem, serverLevel);

      if (recipeAllowed)
         tellClientRecipeAllowed(RecipeAllowed.ALLOWED);
      else
         tellClientRecipeAllowed(RecipeAllowed.NOT_ALLOWED);
   }

   protected void tellClientRecipeAllowed(RecipeAllowed result) {
      if (player == null)
         return;

      PacketDistributor.sendToPlayer(player, new TellProductionBillRecipeAllowed(result));
   }

   protected abstract Optional<Pair<RecipeHolder<TRecipe>, TRecipeInput>> getRecipeFromInputContainer(
         NonNullList<ItemStack> recipeInputItems,
         ServerLevel serverLevel,
         RecipeManager recipeManager);

   public static void serverReceiveDesiredProductionBillAmount(
         EditProductionBillUpdateState packet,
         IPayloadContext context) {

      if (!(context.player().containerMenu instanceof EditRecipeMenu<?, ?> editRecipeMenu))
         return;

      editRecipeMenu.setDesiredProductionStrategyType(packet.desiredProductionStrategyType());
      editRecipeMenu.setDesiredProductionBillAmount(packet.desiredProductionAmount());
   }

   public int getDefaultProductionAmount() {
      return 16;
   }
}
