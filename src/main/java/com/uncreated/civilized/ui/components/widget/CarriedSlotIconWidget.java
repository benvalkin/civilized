package com.uncreated.civilized.ui.components.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CarriedSlotIconWidget extends AbstractWidget {

   public static final int SIZE = 16;

   private final ResourceLocation sprite;

   public CarriedSlotIconWidget(int x, int y, ResourceLocation sprite, Component tooltip) {
      super(x, y, SIZE, SIZE, tooltip);
      this.sprite = sprite;
      setTooltip(Tooltip.create(tooltip));
      active = false; // never takes clicks, but still shows its tooltip
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.blitSprite(RenderType::guiTextured, sprite, getX(), getY(), SIZE, SIZE);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
