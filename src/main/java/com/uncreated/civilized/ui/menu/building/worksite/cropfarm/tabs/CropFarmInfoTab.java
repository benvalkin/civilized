package com.uncreated.civilized.ui.menu.building.worksite.cropfarm.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.uncreated.civilized.core.building.state.CropFarmState;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CropFarmInfoTab extends ABuildingScreenTab {

   private final ArrayList<ItemDisplayWidget> itemDisplayWidgets;
   private List<VillagerInfo> workers;
   private final Button chooseCrops;

   public CropFarmInfoTab(
         ITabHost tabHost,
         Font font,
         BuildingScreenContext context,
         Function<ITabHost, ATab> createCropsTab) {
      super(
            tabHost,
            font,
            Component.translatable("menu.building.residence.info.tab.heading"),
            context);
      this.workers = createOccupantsList();

      itemDisplayWidgets = new ArrayList<>();

      chooseCrops =
            Button.builder(
                  Component.translatable("menu.building.worksite.crop_farm.edit_allowed_crops"),
                  // notice how if the button's 'x' is set to the parent's width, the button seems to render right to
                  // left...
                  // is this a feature??
                  button -> tabHost.changeTab(createCropsTab.apply(tabHost))).pos(getX(), getBottom() - 18).size(80, 18).build();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      graphics.drawString(
            font,
            Component.translatable(
                  "menu.building.worksite.workers.heading",
                  workers.size(),
                  ManageWorkersTab.MAX_ASSIGNED_WORKERS),
            getX(),
            getY() + 20,
            Colors.MENU_TEXT_DARK,
            false);

      for (int i = 0; i < workers.size(); i++) {

         VillagerInfo occupant = workers.get(i);
         graphics.drawString(
               font,
               Component.literal(occupant.getFullName()),
               getX() + 8,
               getY() + 35 + i * 10,
               Colors.MENU_TEXT_DARK,
               false);
      }

      chooseCrops.render(graphics, mouseX, mouseY, partialTicks);

      graphics.drawString(
            font,
            Component.translatable("menu.building.worksite.crop_farm.allowed_crops.heading"),
            getX(),
            getBottom() - 70,
            Colors.MENU_TEXT_DARK,
            false);

      itemDisplayWidgets.forEach(i -> i.render(graphics, mouseX, mouseY, partialTicks));
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(chooseCrops);
   }

   private List<VillagerInfo> createOccupantsList() {
      return BuildingUtil.getAssignedWorkers(context.building(), ClientVillagerStore.INSTANCE);
   }

   @Override
   public void refresh() {
      this.workers = createOccupantsList();

      CropFarmState cropFarmState = (CropFarmState) context.building().getState();

      itemDisplayWidgets.clear();
      for (int i = 0; i < CropFarmState.NUMBER_OF_CROP_SLOTS; i++) {
         ItemStack cropSlot = cropFarmState.getCropSlot(i);
         itemDisplayWidgets.add(new ItemDisplayWidget(getX() + i * 18, getBottom() - 60, cropSlot));
      }
   }
}
