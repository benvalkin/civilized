package com.uncreated.civilized.ui.menu.building.residence.artisan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.RecipeAllowed;
import com.uncreated.civilized.core.building.production.bills.strategy.ProductionStrategyType;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;
import com.uncreated.civilized.networking.packets.PreviewProductionBill;
import com.uncreated.civilized.networking.packets.SaveProductionBill;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.components.widget.EyeDropperSlotWidget;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.components.widget.ItemQuantitySelectorWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.widgets.RecipeAllowedIcon;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Adds a new production bill or edits an existing one. The player drags items from their inventory into the recipe
 * slots and sends them to the server to validate. Saving only happens when "Done" is pressed.
 */
public class EditProductionBillTab extends ABuildingScreenTab {

   private static final int SLOT_SPACING = 18;
   private static final int DEFAULT_PRODUCTION_AMOUNT = 16;

   private static final ResourceLocation ARROW_SPRITE =
         ResourceLocation.withDefaultNamespace("container/villager/trade_arrow");
   private static final int ARROW_WIDTH = 10;
   private static final int ARROW_HEIGHT = 9;
   private static final int RECIPE_ALLOWED_ICON_SIZE = 18;

   private final ProductionType productionType;
   private final int billIndex;
   private final Runnable returnToBills;

   /** The item chosen for each input slot. Only sent to the server when previewing or saving. */
   private final ItemStack[] inputs;
   private final boolean enabled;
   private ProductionStrategyType strategy;
   private int amount;
   private RecipeAllowed recipeAllowed;

   /** Counts the previews asked for, so that answers to older inputs can be told apart and ignored. */
   private int previewSequence;

   private final List<EyeDropperSlotWidget> inputSlots = new ArrayList<>();
   private final ItemDisplayWidget output;
   private final int arrowX;
   private final int arrowY;
   private final RecipeAllowedIcon recipeAllowedIcon;
   private final CycleButton<ProductionStrategyType> strategyButton;
   private final ItemQuantitySelectorWidget quantitySelector;
   private final Button done;
   private final Button cancel;

   /**
    * @param layout
    *           where the recipe slots go, which should suit the production type's input grid
    * @param billIndex
    *           the bill to edit, or {@link SaveProductionBill#NEW_BILL} to add a new one
    * @param returnToBills
    *           a runnable so that you can switch back to the previous tab once the bill is saved or canceled
    */
   public EditProductionBillTab(
         ITabHost tabHost,
         Font font,
         BuildingScreenContext context,
         AbstractContainerScreen<?> screen,
         ProductionType productionType,
         EditProductionBillLayout layout,
         int billIndex,
         Runnable returnToBills) {
      super(tabHost, font, productionType.heading(), context);
      this.productionType = productionType;
      this.billIndex = billIndex;
      this.returnToBills = returnToBills;

      inputs = new ItemStack[productionType.inputSlotCount()];
      Arrays.fill(inputs, ItemStack.EMPTY);

      @Nullable
      ProductionBill existingBill = findExistingBill();
      ItemStack resultItem;
      if (existingBill != null) {
         List<ItemStack> billInputs = existingBill.getInputItems();
         for (int i = 0; i < inputs.length && i < billInputs.size(); i++)
            inputs[i] = billInputs.get(i).copy();

         enabled = existingBill.isEnabled();
         strategy = existingBill.getProductionStrategy().getType();
         amount = existingBill.getBillAmount();
         resultItem = existingBill.getDisplayItem();
      } else {
         enabled = true;
         strategy = ProductionStrategyType.PRODUCE_INFINITE;
         amount = -1;
         resultItem = ItemStack.EMPTY;
      }

      int inputsX = getX() + layout.inputsX();
      int inputsY = getY() + layout.inputsY();

      for (int i = 0; i < inputs.length; i++) {
         int inputSlot = i;
         inputSlots.add(
               new EyeDropperSlotWidget(
                     screen,
                     inputsX + (i % productionType.inputGridWidth()) * SLOT_SPACING,
                     inputsY + (i / productionType.inputGridWidth()) * SLOT_SPACING,
                     () -> inputs[inputSlot],
                     stack -> true,
                     stack -> onInputChosen(inputSlot, stack)));
      }

      output = new ItemDisplayWidget(getX() + layout.outputX(), getY() + layout.outputY(), resultItem);

      // arrow is centered in the gap between the input grid and the output slot, level with the output slot
      int inputsRightEdge = inputsX + (productionType.inputGridWidth() - 1) * SLOT_SPACING + EyeDropperSlotWidget.SIZE;
      int arrowCenterX = (inputsRightEdge + output.getX()) / 2;
      int arrowCenterY = output.getY() + EyeDropperSlotWidget.SIZE / 2;
      arrowX = arrowCenterX - ARROW_WIDTH / 2;
      arrowY = arrowCenterY - ARROW_HEIGHT / 2;

      // the icon draws its cross over the arrow, and hovering the arrow shows which recipes the house may make
      recipeAllowedIcon =
            new RecipeAllowedIcon(
                  arrowCenterX - RECIPE_ALLOWED_ICON_SIZE / 2,
                  arrowCenterY - RECIPE_ALLOWED_ICON_SIZE / 2,
                  RECIPE_ALLOWED_ICON_SIZE);

      strategyButton =
            CycleButton.builder(ProductionStrategyType::getSimpleDescription)
                  .withValues(ProductionStrategyType.values())
                  .withInitialValue(strategy)
                  .create(
                        getRight() - 130,
                        getY() + 15,
                        100,
                        18,
                        Component.translatable("production_bill.production_strategy.heading"),
                        this::onStrategyChanged);

      quantitySelector =
            new ItemQuantitySelectorWidget(
                  getRight() - 65,
                  getY() + 38,
                  resultItem,
                  Math.max(amount, 1),
                  newAmount -> amount = newAmount);
      quantitySelector.visible = strategy.requiresAmount();

      done =
            Button.builder(Component.translatable("gui.misc.button.done"), button -> save())
                  .pos(getRight() - 60, getBottom() - 18)
                  .size(60, 18)
                  .build();

      cancel =
            Button.builder(Component.translatable("gui.misc.button.cancel"), button -> returnToBills.run())
                  .pos(getRight() - 60, getBottom() - 40)
                  .size(60, 18)
                  .build();

      // an existing bill was checked when it was saved, and a blank one has no recipe yet
      setRecipeAllowed(existingBill != null ? RecipeAllowed.ALLOWED : RecipeAllowed.INVALID_RECIPE);
   }

   private @Nullable ProductionBill findExistingBill() {
      if (billIndex == SaveProductionBill.NEW_BILL)
         return null;

      List<ProductionBill> bills = artisanHouseState().getProductionBills(productionType);
      return billIndex < bills.size() ? bills.get(billIndex) : null;
   }

   private ArtisanHouseState artisanHouseState() {
      return (ArtisanHouseState) context.building().getState();
   }

   private void onInputChosen(int inputSlot, ItemStack stack) {
      inputs[inputSlot] = stack;

      // the recipe is unknown until the server answers, so it can't be saved in the meantime
      done.active = false;
      previewSequence++;
      PacketDistributor.sendToServer(
            new PreviewProductionBill(
                  context.building().getBuildingId(),
                  productionType,
                  List.of(inputs),
                  previewSequence));
   }

   /** Called with the server's answer to {@link PreviewProductionBill}. */
   public void receivePreview(int sequence, ItemStack resultItem, RecipeAllowed recipeAllowed) {
      if (sequence != previewSequence)
         return;

      output.getSlot().set(resultItem.copy());
      if (strategy.requiresAmount())
         quantitySelector.setItemAndQuantity(resultItem, amount);

      setRecipeAllowed(recipeAllowed);
   }

   private void onStrategyChanged(CycleButton<ProductionStrategyType> button, ProductionStrategyType newStrategy) {
      strategy = newStrategy;
      amount = newStrategy.requiresAmount() ? DEFAULT_PRODUCTION_AMOUNT : -1;

      quantitySelector.visible = newStrategy.requiresAmount();
      if (quantitySelector.visible)
         quantitySelector.setItemAndQuantity(output.getDisplayItem(), amount);
   }

   private void setRecipeAllowed(RecipeAllowed recipeAllowed) {
      this.recipeAllowed = recipeAllowed;

      Tooltip helpTooltip = artisanHouseState().getAllowedRecipeHelpTooltip();

      switch (recipeAllowed) {
      case ALLOWED:
         done.active = true;
         done.setTooltip(null);
         recipeAllowedIcon.changeStateToAllowed(helpTooltip);
         break;
      case NOT_ALLOWED:
         done.active = false;
         done.setTooltip(
               Tooltip.create(
                     Component.translatable(
                           "menu.building.residence.production_bills.edit_recipe.done.tooltip.recipe_not_allowed")));
         recipeAllowedIcon.changeStateNotAllowed(helpTooltip);
         break;
      case INVALID_RECIPE:
         done.active = false;
         done.setTooltip(
               Tooltip.create(
                     Component.translatable(
                           "menu.building.residence.production_bills.edit_recipe.done.tooltip.invalid_recipe")));
         recipeAllowedIcon.changeStateToInvalid();
         break;
      }
   }

   private void save() {
      if (recipeAllowed != RecipeAllowed.ALLOWED)
         return;

      PacketDistributor.sendToServer(
            new SaveProductionBill(
                  context.building().getBuildingId(),
                  productionType,
                  billIndex,
                  List.of(inputs),
                  strategy,
                  amount,
                  enabled));

      returnToBills.run();
   }

   @Override
   public boolean showsPlayerInventory() {
      return true;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      // heading is not drawn here - the recipe grid needs the space above the inventory

      inputSlots.forEach(slot -> slot.render(graphics, mouseX, mouseY, partialTicks));

      graphics.blitSprite(RenderType::guiTextured, ARROW_SPRITE, arrowX, arrowY, ARROW_WIDTH, ARROW_HEIGHT);

      SlotFrameRenderer.render(graphics, output.getX(), output.getY());
      output.render(graphics, mouseX, mouseY, partialTicks);
      recipeAllowedIcon.render(graphics, mouseX, mouseY, partialTicks);

      strategyButton.render(graphics, mouseX, mouseY, partialTicks);
      if (quantitySelector.visible) {
         graphics.drawString(
               font,
               Component.translatable("gui.misc.quantity"),
               getRight() - 120,
               getY() + 43,
               Colors.MENU_TEXT_DARK,
               false);
         quantitySelector.render(graphics, mouseX, mouseY, partialTicks);
      }

      cancel.render(graphics, mouseX, mouseY, partialTicks);
      done.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>(inputSlots);
      children.add(output);
      children.add(strategyButton);
      children.add(quantitySelector);
      children.add(cancel);
      children.add(done);
      return children;
   }
}
