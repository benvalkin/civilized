package com.uncreated.civilized.ui.components.widget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ArrowSelectorWidget<T> extends AbstractWidget {

   public static final int ARROW_WIDTH = 14;

   private static final int LABEL_PADDING = 2;
   private static final String ELLIPSIS = "...";
   /** Same sprites as vanilla's buttons to make the widget look like button. */
   private static final WidgetSprites ARROW_SPRITES =
         new WidgetSprites(
               ResourceLocation.withDefaultNamespace("widget/button"),
               ResourceLocation.withDefaultNamespace("widget/button_disabled"),
               ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

   private final Font font;
   @Getter
   private final List<T> values;
   private final Function<T, Component> nameOf;
   private final Consumer<T> onChanged;
   private int selectedIndex;

   /**
    * @param onChanged
    *           called whenever the player picks a different value.
    */
   public ArrowSelectorWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         List<T> values,
         int selectedIndex,
         Function<T, Component> nameOf,
         Consumer<T> onChanged) {
      super(x, y, width, height, Component.empty());
      if (values.isEmpty())
         throw new IllegalArgumentException("Arrow selector needs at least one value to show.");

      this.font = font;
      this.values = List.copyOf(values);
      this.nameOf = nameOf;
      this.onChanged = onChanged;
      forceSetSelectedIndex(selectedIndex);
   }

   public T getSelected() {
      return values.get(selectedIndex);
   }

   /** Force set the selected index without calling {@code onChanged}. */
   public void forceSetSelectedIndex(int index) {
      // this method does not update the listener
      this.selectedIndex = Math.floorMod(index, values.size());
      setMessage(nameOf.apply(getSelected()));
      setTooltip(
            Tooltip.create(Component.translatable("gui.misc.selector.position", selectedIndex + 1, values.size())));
   }

   private void step(int direction) {
      // there's nothing to change to
      if (values.size() == 1)
         return;

      forceSetSelectedIndex(selectedIndex + direction);
      onChanged.accept(getSelected());
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      step(isOverPreviousArrow(mouseX) ? -1 : 1);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (!active || !visible || scrollY == 0)
         return false;

      // scrolling up goes back up the list
      step(scrollY > 0 ? -1 : 1);
      return true;
   }

   private boolean isOverPreviousArrow(double mouseX) {
      return mouseX < getX() + ARROW_WIDTH;
   }

   private boolean isOverNextArrow(double mouseX) {
      return mouseX >= getRight() - ARROW_WIDTH;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      boolean canStep = active && values.size() > 1;
      renderArrow(graphics, getX(), "<", canStep, isHovered() && isOverPreviousArrow(mouseX));
      renderArrow(graphics, getRight() - ARROW_WIDTH, ">", canStep, isHovered() && isOverNextArrow(mouseX));

      int labelLeft = getX() + ARROW_WIDTH + LABEL_PADDING;
      int labelWidth = width - (ARROW_WIDTH + LABEL_PADDING) * 2;
      String label = fitToWidth(getMessage().getString(), labelWidth);
      graphics.drawString(
            font,
            label,
            labelLeft + (labelWidth - font.width(label)) / 2,
            getY() + (height - font.lineHeight) / 2 + 1,
            Colors.MENU_TEXT_DARK,
            false);
   }

   private void renderArrow(GuiGraphics graphics, int x, String arrow, boolean enabled, boolean hovered) {
      graphics.blitSprite(RenderType::guiTextured, ARROW_SPRITES.get(enabled, hovered), x, getY(), ARROW_WIDTH, height);
      graphics.drawCenteredString(
            font,
            arrow,
            x + ARROW_WIDTH / 2,
            getY() + (height - font.lineHeight) / 2 + 1,
            enabled ? 0xFFFFFF : 0xA0A0A0);
   }

   /** Cuts the label short with an ellipsis when it's too long to fit between the arrows. */
   private String fitToWidth(String label, int maxWidth) {
      if (font.width(label) <= maxWidth)
         return label;

      return font.plainSubstrByWidth(label, maxWidth - font.width(ELLIPSIS)) + ELLIPSIS;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      narrationElementOutput.add(NarratedElementType.TITLE, getMessage());
   }
}
