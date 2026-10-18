package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.ui.components.widget.CarriedSlotIconWidget;
import com.uncreated.civilized.ui.components.widget.EyeDropperSlotWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class EditLoadoutTab extends ABuildingScreenTab {

   private static final int ITEMS_PER_SLOT = 3;
   private static final int NAME_Y = 11;
   private static final int ROWS_TOP = 32;
   private static final int ROW_SPACING = 19;
   private static final int SLOT_SPACING = 18;
   private static final int ICON_RIGHT_MARGIN = CarriedSlotIconWidget.SIZE + 2;
   private static final int FILTER_SET_GAP = 0;

   private static final int NAME_BOX_WIDTH = 128;
   private static final int NAME_MAX_LENGTH = 32;
   private static final int BUTTON_WIDTH = 60;
   private static final int BUTTON_HEIGHT = 18;

   /** The loadout being edited, or null when creating a new one. */
   @Nullable
   private final Loadout loadout;

   private final ItemStack[][] items = new ItemStack[Loadout.SUPPORTED_SLOTS.size()][ITEMS_PER_SLOT];

   private final List<CarriedSlotIconWidget> slotIcons = new ArrayList<>();
   private final List<EyeDropperSlotWidget> itemSlots = new ArrayList<>();
   private final EditBox nameBox;
   private final Button cancelButton;
   private final Button saveButton;

   public EditLoadoutTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      this(tabHost, font, context, null);
   }

   public EditLoadoutTab(ITabHost tabHost, Font font, BuildingScreenContext context, @Nullable Loadout loadout) {
      super(
            tabHost,
            font,
            Component.translatable(
                  loadout == null ? "menu.building.military.loadout.create.tab.heading"
                        : "menu.building.military.loadout.edit.tab.heading"),
            context);
      this.loadout = loadout;

      for (ItemStack[] slotItems : items)
         Arrays.fill(slotItems, ItemStack.EMPTY);
      if (loadout != null)
         copyItemsFrom(loadout);

      AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) tabHost;
      createSlotRows(screen);

      int nameBoxX = getX() + font.width(Component.translatable("menu.building.military.loadout.edit.name")) + 4;
      nameBox =
            new EditBox(
                  font,
                  nameBoxX,
                  getY() + NAME_Y,
                  NAME_BOX_WIDTH,
                  16,
                  Component.translatable("menu.building.military.loadout.edit.name"));
      nameBox.setMaxLength(NAME_MAX_LENGTH);
      nameBox.setHint(Component.translatable("menu.building.military.loadout.edit.name.hint"));
      if (loadout != null)
         nameBox.setValue(loadout.displayName());

      cancelButton =
            Button.builder(Component.translatable("gui.misc.button.cancel"), button -> goBack(tabHost))
                  .pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT * 2 - 4)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      saveButton =
            Button.builder(Component.translatable("gui.misc.button.save"), button -> saveLoadout(tabHost))
                  .pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();
   }

   private void createSlotRows(AbstractContainerScreen<?> screen) {
      int setWidth = ICON_RIGHT_MARGIN + SLOT_SPACING * ITEMS_PER_SLOT + FILTER_SET_GAP;

      for (int slotIndex = 0; slotIndex < Loadout.SUPPORTED_SLOTS.size(); slotIndex++) {
         CarriedEquipmentSlot slot = Loadout.SUPPORTED_SLOTS.get(slotIndex);
         GridCell cell = gridCellFor(slot);
         int setX = getX() + 1 + cell.column() * setWidth;
         int setY = getY() + ROWS_TOP + cell.row() * ROW_SPACING;

         // centred against the item slots' frames
         int iconInset = (SLOT_SPACING - CarriedSlotIconWidget.SIZE) / 2;
         slotIcons.add(new CarriedSlotIconWidget(setX, setY + iconInset, iconFor(slot), tooltipFor(slot)));

         for (int itemIndex = 0; itemIndex < ITEMS_PER_SLOT; itemIndex++) {
            int slotItems = slotIndex;
            int item = itemIndex;
            itemSlots.add(
                  new EyeDropperSlotWidget(
                        screen,
                        setX + ICON_RIGHT_MARGIN + itemIndex * SLOT_SPACING,
                        setY,
                        () -> items[slotItems][item],
                        stack -> isItemAllowed(slot, stack),
                        stack -> items[slotItems][item] = stack));
         }
      }
   }

   private record GridCell(int column, int row) {
   }

   private static GridCell gridCellFor(CarriedEquipmentSlot slot) {
      return switch (slot) {
      case PRIMARY_WEAPON -> new GridCell(0, 0);
      case SECONDARY_WEAPON -> new GridCell(1, 0);
      case OFFHAND_WEAPON -> new GridCell(0, 1);
      case HEAD -> new GridCell(2, 0);
      case CHEST -> new GridCell(3, 0);
      case LEGS -> new GridCell(2, 1);
      case FEET -> new GridCell(3, 1);
      case TOOL -> throw new IllegalArgumentException("Loadouts don't have a tool slot");
      };
   }

   private void copyItemsFrom(Loadout loadout) {
      for (int slotIndex = 0; slotIndex < Loadout.SUPPORTED_SLOTS.size(); slotIndex++) {
         List<ItemStack> allowed =
               loadout.getAllowedItems(Loadout.SUPPORTED_SLOTS.get(slotIndex)).map(ItemFilter::items).orElse(List.of());
         for (int itemIndex = 0; itemIndex < ITEMS_PER_SLOT && itemIndex < allowed.size(); itemIndex++)
            items[slotIndex][itemIndex] = allowed.get(itemIndex).copy();
      }
   }

   private static @Nullable EquipmentSlot armourSlot(CarriedEquipmentSlot slot) {
      return switch (slot) {
      case HEAD -> EquipmentSlot.HEAD;
      case CHEST -> EquipmentSlot.CHEST;
      case LEGS -> EquipmentSlot.LEGS;
      case FEET -> EquipmentSlot.FEET;
      default -> null;
      };
   }

   private static boolean isItemAllowed(CarriedEquipmentSlot slot, ItemStack stack) {
      EquipmentSlot armourSlot = armourSlot(slot);
      if (armourSlot == null)
         return true;

      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.getEquipmentSlotForItem(stack) == armourSlot;
   }

   private static ResourceLocation iconFor(CarriedEquipmentSlot slot) {
      String sprite = switch (slot) {
      case PRIMARY_WEAPON -> "sword";
      case SECONDARY_WEAPON -> "axe";
      case OFFHAND_WEAPON -> "shield";
      case HEAD -> "helmet";
      case CHEST -> "chestplate";
      case LEGS -> "leggings";
      case FEET -> "boots";
      case TOOL -> "pickaxe";
      };
      return ResourceLocation.withDefaultNamespace("container/slot/" + sprite);
   }

   private static Component tooltipFor(CarriedEquipmentSlot slot) {
      return Component.translatable("menu.building.military.loadout.edit.slot." + slot.name().toLowerCase())
            .append("\n")
            .append(Component.translatable("menu.building.military.loadout.edit.slot.preference_help", ITEMS_PER_SLOT));
   }

   private void goBack(ITabHost tabHost) {
      tabHost.changeTab(new ManageLoadoutsTab(tabHost, font, context));
   }

   private void saveLoadout(ITabHost tabHost) {

      goBack(tabHost);
   }

   @Override
   public boolean showsPlayerInventory() {
      return true;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      slotIcons.forEach(icon -> icon.render(graphics, mouseX, mouseY, partialTicks));
      itemSlots.forEach(slot -> slot.render(graphics, mouseX, mouseY, partialTicks));

      graphics.drawString(
            font,
            Component.translatable("menu.building.military.loadout.edit.name"),
            getX(),
            nameBox.getY() + (nameBox.getHeight() - font.lineHeight) / 2 + 1,
            Colors.MENU_TEXT_DARK,
            false);
      nameBox.render(graphics, mouseX, mouseY, partialTicks);

      // save button is disabled until a name is specified
      saveButton.active = !nameBox.getValue().isBlank();
      cancelButton.render(graphics, mouseX, mouseY, partialTicks);
      saveButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>(itemSlots);
      children.add(nameBox);
      children.add(cancelButton);
      children.add(saveButton);
      return children;
   }
}
