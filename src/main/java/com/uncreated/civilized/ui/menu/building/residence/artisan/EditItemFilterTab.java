package com.uncreated.civilized.ui.menu.building.residence.artisan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.ItemFilterMode;
import com.uncreated.civilized.core.building.production.bills.RecipeSlotType;
import com.uncreated.civilized.ui.components.widget.EyeDropperSlotWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EditItemFilterTab extends ABuildingScreenTab {

   private static final int DESCRIPTION_Y = 26;
   private static final int SLOT_SPACING = 18;
   private static final int MODE_BUTTON_X = 8;
   private static final int MODE_BUTTON_Y = 38;
   private static final int MODE_BUTTON_WIDTH = 100;

   private final RecipeSlotType recipeSlot;

   /** The filter items chosen so far. */
   private final ItemStack[] items = new ItemStack[ItemFilter.MAX_ITEMS];
   private ItemFilterMode mode;

   private final List<EyeDropperSlotWidget> itemSlots = new ArrayList<>();
   private final CycleButton<ItemFilterMode> modeButton;
   private final Button done;
   private final Button cancel;

   public EditItemFilterTab(
         ITabHost tabHost,
         Font font,
         BuildingScreenContext context,
         AbstractContainerScreen<?> screen,
         RecipeSlotType recipeSlot,
         ItemFilter filter,
         Consumer<ItemFilter> onDone,
         Runnable onCancel) {
      super(tabHost, font, recipeSlot.name(), context);
      this.recipeSlot = recipeSlot;
      this.mode = filter.mode();

      Arrays.fill(items, ItemStack.EMPTY);
      for (int i = 0; i < items.length && i < filter.items().size(); i++)
         items[i] = filter.items().get(i).copy();

      modeButton =
            CycleButton.builder(EditItemFilterTab::modeName)
                  .withValues(ItemFilterMode.values())
                  .withInitialValue(mode)
                  .create(
                        getX() + MODE_BUTTON_X,
                        getY() + MODE_BUTTON_Y,
                        MODE_BUTTON_WIDTH,
                        18,
                        Component.translatable("item_filter.mode.heading"),
                        (button, newMode) -> mode = newMode);

      int firstSlotX = modeButton.getRight() + 6;
      int slotY = modeButton.getY() + 1;

      for (int i = 0; i < items.length; i++) {
         int itemSlot = i;
         itemSlots.add(
               new EyeDropperSlotWidget(
                     screen,
                     firstSlotX + i * SLOT_SPACING,
                     slotY,
                     () -> items[itemSlot],
                     this::isItemAllowed,
                     stack -> items[itemSlot] = stack));
      }

      done =
            Button.builder(Component.translatable("gui.misc.button.done"), button -> onDone.accept(toItemFilter()))
                  .pos(getRight() - 60, getBottom() - 18)
                  .size(60, 18)
                  .build();

      cancel =
            Button.builder(Component.translatable("gui.misc.button.cancel"), button -> onCancel.run())
                  .pos(getRight() - 60, getBottom() - 40)
                  .size(60, 18)
                  .build();
   }

   private static Component modeName(ItemFilterMode mode) {
      return Component.translatable("item_filter.mode." + mode.name().toLowerCase());
   }

   private static Component modeDescription(ItemFilterMode mode, RecipeSlotType slot) {
      return Component.translatable("item_filter.mode.description." + mode.name().toLowerCase(), slot.name());
   }

   private boolean isItemAllowed(ItemStack stack) {
      Level level = Minecraft.getInstance().level;

      return level != null && recipeSlot.isItemAllowed(stack, level);
   }

   private ItemFilter toItemFilter() {
      // get rid of empty items
      List<ItemStack> nonEmptyItems =
            Arrays.stream(items).filter(item -> !item.isEmpty()).map(ItemStack::copy).toList();
      return new ItemFilter(mode, new ArrayList<>(nonEmptyItems));
   }

   @Override
   public boolean showsPlayerInventory() {
      return true;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

      graphics.drawWordWrap(
            font,
            Component.translatable("menu.building.residence.production_bills.edit_item_filter.description"),
            getX(),
            getY(),
            width,
            Colors.MENU_TEXT_DARK,
            false);

      graphics.drawWordWrap(
            font,
            modeDescription(mode, recipeSlot),
            modeButton.getRight() + 6,
            modeButton.getY() - 11,
            width,
            Colors.MENU_TEXT_DARK,
            false);

      itemSlots.forEach(slot -> slot.render(graphics, mouseX, mouseY, partialTicks));
      modeButton.render(graphics, mouseX, mouseY, partialTicks);

      cancel.render(graphics, mouseX, mouseY, partialTicks);
      done.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>(itemSlots);
      children.add(modeButton);
      children.add(cancel);
      children.add(done);
      return children;
   }
}
