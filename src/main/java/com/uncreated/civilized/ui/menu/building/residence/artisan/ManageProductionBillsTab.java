package com.uncreated.civilized.ui.menu.building.residence.artisan;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;
import com.uncreated.civilized.networking.packets.RequestEditRecipeProductionScreen;
import com.uncreated.civilized.ui.components.IListViewBuilder;
import com.uncreated.civilized.ui.components.ScrollListView;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.widgets.ProductionBillListViewWidget;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManageProductionBillsTab extends ABuildingScreenTab {

   private final Button addProductionBill;
   private final ProductionType productionType;
   @Nullable
   private ScrollListView<ProductionBill, ProductionBillListViewWidget> scrollView;

   public ManageProductionBillsTab(
         ITabHost tabHost,
         Font font,
         BuildingScreenContext context,
         ProductionType productionType) {
      super(tabHost, font, productionType.heading(), context);
      this.productionType = productionType;

      addProductionBill =
            Button.builder(
                  Component.translatable("menu.building.residence.production_bills.add_bill.description"),
                  this::onAddBill)
                  .pos(getX() + width - 80, getBottom() - 20)
                  .size(80, 20)
                  .tooltip(
                        Tooltip.create(
                              Component.translatable("menu.building.residence.production_bills.add_bill.tooltip")))
                  .build();

      ArtisanHouseState state = (ArtisanHouseState) context.building().getState();
      if (state.tryLoadDefaultProductionBills())
         ClientBuildingStore.INSTANCE.replicateChange(context.building(), StoreOperation.UPDATE);

      refresh();
   }

   private void onAddBill(Button button) {
      ArtisanHouseState state = (ArtisanHouseState) context.building().getState();
      int newIndex = state.getProductionBills(productionType).size();

      PacketDistributor.sendToServer(
            new RequestEditRecipeProductionScreen(
                  context.building().getBuildingId(),
                  9,
                  productionType,
                  newIndex,
                  true));
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      if (scrollView != null)
         scrollView.render(graphics, mouseX, mouseY, partialTicks);
      else
         graphics.drawWordWrap(
               font,
               Component.translatable("menu.building.residence.production_bills.heading.empty"),
               getX(),
               getY() + 15,
               width,
               Colors.MENU_TEXT_DARK,
               false);

      addProductionBill.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = Lists.newArrayList();
      children.add(addProductionBill);
      children.add(scrollView);
      children.addAll(scrollView.children());
      return children;
   }

   private ScrollListView<ProductionBill, ProductionBillListViewWidget> createScrollView(
         List<ProductionBill> productionBills) {
      final int elementHeight = 22;

      return new ScrollListView<>(getX(), getY() + 20, width, height - 45, elementHeight, new IListViewBuilder<>() {

         @Override
         public List<ProductionBill> provideModelData() {
            return productionBills;
         }

         @Override
         public ProductionBillListViewWidget buildElementWidgetFromModel(
               int elementIndex,
               ProductionBill productionBill,
               int elementX,
               int elementY,
               int elementWidth,
               int elementHeight,
               int elementSpacing) {
            return new ProductionBillListViewWidget(
                  elementX,
                  elementY,
                  elementWidth,
                  elementHeight,
                  font,
                  productionBill,
                  elementIndex,
                  context.building());
         }
      });
   }

   @Override
   public void refresh() {

      ArtisanHouseState state = (ArtisanHouseState) context.building().getState();
      scrollView = createScrollView(state.getProductionBills(productionType));
   }
}
