package com.uncreated.civilized.ui.menu.building.widgets;

import java.util.List;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.strategy.ProductionStrategyType;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;
import com.uncreated.civilized.ui.components.buttons.TrashcanButton;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ProductionBillListViewWidget extends AbstractContainerWidget {

   protected final Font font;
   private final int productionBillIndex;
   protected final Building building;
   protected ProductionBill productionBill;
   private final Button editButton;
   private final ItemDisplayWidget itemDisplay;
   private final Checkbox enabledButton;
   private final TrashcanButton deleteButton;
   private final Runnable onEdit;

   public ProductionBillListViewWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         ProductionBill productionBill,
         int productionBillIndex,
         Building building,
         Runnable onEdit) {
      super(x, y, width, height, Component.literal("ManageOccupantWidget"));
      this.font = font;
      this.productionBillIndex = productionBillIndex;
      this.productionBill = productionBill;
      this.building = building;
      this.onEdit = onEdit;

      itemDisplay = new ItemDisplayWidget(x + 4, y, productionBill.getDisplayItem());

      deleteButton = new TrashcanButton(x + width - 90, y, b -> {
      }, this::onDeleteConfirmed);

      enabledButton =
            Checkbox.builder(Component.empty(), font)
                  .onValueChange(this::onProductionBillToggled)
                  .tooltip(Tooltip.create(Component.translatable("production_bill.description.enabled")))
                  .pos(x + width - 70, y)
                  .selected(productionBill.isEnabled())
                  .build();
      editButton =
            Button.builder(Component.translatable("gui.misc.button.edit"), this::onPressEdit)
                  .pos(x + width - 50, y)
                  .size(50, 17)
                  .build();

   }

   private void onDeleteConfirmed(TrashcanButton button) {
      ArtisanHouseState state = (ArtisanHouseState) building.getState();
      if (productionBillIndex >= state.getProductionBills(productionBill.getProductionType()).size())
         return;

      state.removeBill(productionBill.getProductionType(), productionBillIndex);
      ClientBuildingStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
   }

   private void onProductionBillToggled(Checkbox checkbox, boolean enabled) {
      // we have to set the state on the actual building instance because the building's state field may no longer be
      // the same
      // instance as the stored instance
      this.productionBill =
            ((ArtisanHouseState) building.getState()).getProductionBills(productionBill.getProductionType())
                  .get(productionBillIndex);
      productionBill.setEnabled(enabled);
      ClientBuildingStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
   }

   @Override
   protected int contentHeight() {
      return height;
   }

   @Override
   protected double scrollRate() {
      return 0;
   }

   @Override
   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

      guiGraphics.drawString(
            font,
            ProductionStrategyType.getComplexDescription(productionBill),
            getX() + 28,
            getY() + 6,
            Colors.MENU_TEXT_DARK,
            false);

      deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
      editButton.render(guiGraphics, mouseX, mouseY, partialTick);
      enabledButton.render(guiGraphics, mouseX, mouseY, partialTick);
      itemDisplay.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(deleteButton, editButton, enabledButton, itemDisplay);
   }

   private void onPressEdit(Button b) {
      onEdit.run();
   }
}
