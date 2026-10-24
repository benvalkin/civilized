package com.uncreated.civilized.ui.menu.villager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;


public class VillagerTabButton extends AbstractButton {

   public static final int SIZE = 22;

   private final ItemStack icon;
   private final boolean selected;
   private final Runnable onPress;

   public VillagerTabButton(int x, int y, ItemStack icon, Component name, boolean selected, Runnable onPress) {
      super(x, y, SIZE, SIZE, name);
      this.icon = icon;
      this.selected = selected;
      this.onPress = onPress;
      setTooltip(Tooltip.create(name));
   }

   @Override
   public void onPress() {
      onPress.run();
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int colour;
      if (selected)
         colour = VillagerScreen.PANEL_COLOUR;
      else if (isHoveredOrFocused() && active)
         colour = VillagerScreen.HOVERED_TAB_COLOUR;
      else
         colour = VillagerScreen.UNSELECTED_TAB_COLOUR;

      // the selected tab meets the card below it, so the two read as one panel, while the others stop just short of it.
      // Overlapping the card instead would draw a darker line, since the colours are translucent.
      int bottom = selected ? getBottom() : getBottom() - 1;
      graphics.fill(getX(), getY(), getRight(), bottom, colour);

      int iconX = getX() + (SIZE - 16) / 2;
      int iconY = getY() + (SIZE - 16) / 2;
      graphics.renderFakeItem(icon, iconX, iconY);
      if (!active) {
         // items are drawn in front of flat fills, so the overlay is moved in front of the item
         graphics.pose().pushPose();
         graphics.pose().translate(0, 0, 200);
         graphics.fill(iconX, iconY, iconX + 16, iconY + 16, VillagerScreen.DISABLED_TAB_OVERLAY);
         graphics.pose().popPose();
      }
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
