package com.uncreated.civilized.ui.menu.building.widgets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.core.building.production.bills.RecipeAllowed;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RecipeAllowedIcon extends AbstractWidget {

   // note that blitSprite omits the full path
   private static final ResourceLocation CHECKMARK =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "icon/checkmark_long");
   private static final ResourceLocation X = ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "icon/x_long");

   private final int iconSize;

   @Getter
   private RecipeAllowed recipeAllowed;

   ResourceLocation imageToDraw;

   public RecipeAllowedIcon(int x, int y, int iconSize) {
      super(x, y, iconSize, iconSize, Component.literal("Recipe Allowed"));
      this.iconSize = iconSize;
   }

   public RecipeAllowedIcon changeStateToAllowed(Tooltip helpTooltip) {
      imageToDraw = null;
      recipeAllowed = RecipeAllowed.ALLOWED;
      this.setTooltip(helpTooltip);
      return this;
   }

   public RecipeAllowedIcon changeStateNotAllowed(Tooltip helpTooltip) {
      imageToDraw = X;
      recipeAllowed = RecipeAllowed.NOT_ALLOWED;
      this.setTooltip(helpTooltip);
      return this;
   }

   public RecipeAllowedIcon changeStateToInvalid() {
      imageToDraw = null;
      recipeAllowed = RecipeAllowed.INVALID_RECIPE;
      this.setTooltip(null);
      return this;
   }

   @Override
   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (imageToDraw == null)
         return;

      guiGraphics.blitSprite(RenderType::guiTextured, imageToDraw, getX(), getY(), iconSize, iconSize);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

   }
}
