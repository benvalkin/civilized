package com.uncreated.civilized.ui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a vanilla inventory slot background square. Saves GUIs from needing inventory slots painted into their
 * background texture. It uses the vanilla single slot sprite (container/slot) so resource packs will be able to change
 * how it looks.
 */
public final class SlotFrameRenderer {

   private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
   private static final int SLOT_SIZE = 18;

   private SlotFrameRenderer() {
   }

   /**
    * @param itemX
    *           the coordinate of the {@link net.minecraft.world.inventory.Slot}. The actual slot frame will be drawn 1
    *           pixel to the left of this value (the square is one pixel larger on every side).
    * @param itemY
    *           the coordinate of the {@link net.minecraft.world.inventory.Slot}. The actual slot frame will be drawn 1
    *           pixel above of this value (the square is one pixel larger on every side).
    **/
   public static void render(GuiGraphics graphics, int itemX, int itemY) {
      graphics.blitSprite(RenderType::guiTextured, SLOT_SPRITE, itemX - 1, itemY - 1, SLOT_SIZE, SLOT_SIZE);
   }
}
