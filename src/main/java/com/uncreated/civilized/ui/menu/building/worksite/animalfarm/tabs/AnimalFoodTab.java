package com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.core.building.state.animalfarm.AnimalFarmState;
import com.uncreated.civilized.networking.packets.SetEyeDropperSlotItem;
import com.uncreated.civilized.ui.components.widget.EyeDropperSlotWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class AnimalFoodTab extends ABuildingScreenTab {

   private static final int DESCRIPTION_Y = 12;
   private static final int SLOTS_Y = 38;
   private static final int SLOT_SPACING = 36;

   private final List<EyeDropperSlotWidget> foodSlots = new ArrayList<>();

   public AnimalFoodTab(
         ITabHost tabHost,
         Font font,
         BuildingScreenContext context,
         AbstractContainerScreen<?> screen) {
      super(
            tabHost,
            font,
            Component.translatable("menu.building.worksite.animal_farm.allowed_animal_food.heading"),
            context);

      int x = tabHost.getTabCoords().contentLeftPos();
      int y = tabHost.getTabCoords().contentTopPos();
      int slotsWidth = EyeDropperSlotWidget.SIZE + (AnimalFarmState.NUMBER_OF_FOOD_SLOTS - 1) * SLOT_SPACING;
      int firstSlotX = x + (width - slotsWidth) / 2;

      for (int i = 0; i < AnimalFarmState.NUMBER_OF_FOOD_SLOTS; i++) {
         int foodSlot = i;
         foodSlots.add(
               new EyeDropperSlotWidget(
                     screen,
                     firstSlotX + i * SLOT_SPACING,
                     y + SLOTS_Y,
                     () -> animalFarmState().getFoodSlot(foodSlot),
                     stack -> animalFarmState().isCorrectAnimalFood(stack),
                     stack -> PacketDistributor.sendToServer(
                           new SetEyeDropperSlotItem(context.building().getBuildingId(), foodSlot, stack))));
      }
   }

   private AnimalFarmState animalFarmState() {
      return (AnimalFarmState) context.building().getState();
   }

   @Override
   public boolean showsPlayerInventory() {
      return true;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      graphics.drawWordWrap(
            font,
            Component.translatable("menu.building.worksite.animal_farm.allowed_animal_food.description"),
            getX(),
            getY() + DESCRIPTION_Y,
            width,
            Colors.MENU_TEXT_DARK,
            false);

      foodSlots.forEach(slot -> slot.render(graphics, mouseX, mouseY, partialTicks));
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return foodSlots;
   }
}
