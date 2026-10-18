package com.uncreated.civilized.ui.menu.building.military.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class LoadoutRowWidget extends AbstractContainerWidget {

   /** Tall enough for the name, with a row of item slots underneath. */
   public static final int HEIGHT = 34;

   private static final int SLOTS_TOP = 12;
   /** Item slots are 16 pixels, with a 1 pixel frame on every side. */
   private static final int SLOT_SPACING = 18;
   private static final int BUTTON_WIDTH = 40;
   private static final int BUTTON_HEIGHT = 16;

   private final Font font;
   private final Loadout loadout;
   private final List<ItemDisplayWidget> previewSlots = new ArrayList<>();
   private final Button editButton;

   public LoadoutRowWidget(int x, int y, int width, int height, Font font, Loadout loadout, Consumer<Loadout> onEdit) {
      super(x, y, width, height, Component.literal(loadout.displayName()));
      this.font = font;
      this.loadout = loadout;

      int iconNumber = 0;
      for (int i = 0; i < Loadout.SUPPORTED_SLOTS.size(); i++) {
         CarriedEquipmentSlot slot = Loadout.SUPPORTED_SLOTS.get(i);
         ItemFilter allowedItems = loadout.loadoutItems().getFilter(i);
         if (allowedItems.items().isEmpty())
            continue;

         ItemStack first = allowedItems.items().getFirst();
         previewSlots.add(new ItemDisplayWidget(x + 1 + iconNumber * SLOT_SPACING, y + SLOTS_TOP + 1, first));
         iconNumber++;
      }

      editButton =
            Button.builder(Component.translatable("gui.misc.button.edit"), button -> onEdit.accept(loadout))
                  .pos(x + width - BUTTON_WIDTH, y + (height - BUTTON_HEIGHT) / 2)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.drawString(font, loadout.displayName(), getX(), getY(), Colors.MENU_TEXT_DARK, false);

      for (ItemDisplayWidget previewSlot : previewSlots) {
         previewSlot.render(graphics, mouseX, mouseY, partialTick);
      }

      editButton.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(editButton);
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
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }
}
