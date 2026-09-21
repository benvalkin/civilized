package com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.core.building.state.animalfarm.AnimalFarmState;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class AnimalFarmInfoTab extends ABuildingScreenTab {

   private final ArrayList<ItemDisplayWidget> itemDisplayWidgets;
   private List<VillagerInfo> workers;

   public AnimalFarmInfoTab(int index, int x, int y, int width, int height, Font font, BuildingScreenContext context) {
      super(
            index,
            x,
            y,
            width,
            height,
            font,
            Component.translatable("menu.building.residence.info.tab.heading"),
            context);
      this.workers = createOccupantsList();

      itemDisplayWidgets = new ArrayList<>();
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

      graphics.drawString(
            font,
            Component.translatable("menu.building.worksite.animal_farm.allowed_animal_food.heading"),
            getX(),
            getHeight() - 70,
            Colors.MENU_TEXT_DARK,
            false);

      itemDisplayWidgets.forEach(i -> i.render(graphics, mouseX, mouseY, partialTicks));

   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of();
   }

   private List<VillagerInfo> createOccupantsList() {
      return BuildingUtil.getAssignedWorkers(context.building(), ClientVillagerStore.INSTANCE);
   }

   @Override
   public void refresh() {
      this.workers = createOccupantsList();

      AnimalFarmState animalFarmState = (AnimalFarmState) context.building().getState();

      itemDisplayWidgets.clear();
      for (int i = 0; i < AnimalFarmState.NUMBER_OF_FOOD_SLOTS; i++) {
         ItemStack foodSlot = animalFarmState.getFoodSlot(i);
         itemDisplayWidgets.add(new ItemDisplayWidget(getX() + i * 18, getHeight() - 60, foodSlot));
      }
   }
}
