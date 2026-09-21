package com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.state.animalfarm.AnimalFarmState;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class AnimalFarmInfoTab extends ABuildingScreenTab {

   private final ArrayList<ItemDisplayWidget> itemDisplayWidgets;
   private final Button chooseFood;
   private List<VillagerInfo> workers;

   public AnimalFarmInfoTab(
         ITabHost tabHost,
         Font font,
         BuildingScreenContext context,
         ABuildingMenuScreen screen) {
      super(tabHost, font, Component.translatable("menu.building.residence.info.tab.heading"), context);
      this.workers = createOccupantsList();

      itemDisplayWidgets = new ArrayList<>();

      chooseFood =
            Button.builder(
                  Component.translatable("menu.building.worksite.animal_farm.edit_allowed_animal_food"),
                  openChooseFoodTab(tabHost, context, screen)).pos(getX(), getBottom() - 18).size(80, 18).build();
   }

   private Button.@NotNull OnPress openChooseFoodTab(
         ITabHost tabHost,
         BuildingScreenContext context,
         ABuildingMenuScreen screen) {
      return button -> tabHost.changeTab(tabHost.changeTab(new AnimalFoodTab(tabHost, this.font, context, screen)));
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
            getBottom() - 70,
            Colors.MENU_TEXT_DARK,
            false);

      itemDisplayWidgets.forEach(i -> i.render(graphics, mouseX, mouseY, partialTicks));

      chooseFood.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(chooseFood);
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
         itemDisplayWidgets.add(new ItemDisplayWidget(getX() + i * 18, getBottom() - 60, foodSlot));
      }
   }
}
