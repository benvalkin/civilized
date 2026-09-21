package com.uncreated.civilized.ui.components.buttons.buildingtab;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.function.Function;

import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ILazyLoadTabHost;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class LazyLoadBuildingTabButton extends ImageButton {

   protected static final WidgetSprites SPRITES =
         new WidgetSprites(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "widget/button_building_tab"),
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "widget/button_building_tab"),
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "widget/button_building_tab_selected"));
   private static final int IMAGE_WIDTH = 32;
   private static final int IMAGE_HEIGHT = 32;
   private final ResourceLocation iconTexture;
   private final int imageX;
   private final int imageY;
   protected static final int BUTTON_SPACING = 23;

   public LazyLoadBuildingTabButton(
         ILazyLoadTabHost tabHost,
         int buttonTabIndex,
         ResourceLocation iconTexture,
         Tooltip tooltip,
         Function<ILazyLoadTabHost, ATab> createTab) {
      super(
            tabHost.getFirstTabButtonX(),
            tabHost.getFirstTabButtonY() + buttonTabIndex * BUTTON_SPACING,
            28,
            20,
            SPRITES,
            b -> {
               ATab tab = createTab.apply(tabHost);
               tabHost.changeTab(tab);
            });
      this.iconTexture = iconTexture;
      imageX = tabHost.getFirstTabButtonX() - 2;
      imageY = tabHost.getFirstTabButtonY() - 7 + buttonTabIndex * BUTTON_SPACING;
      this.setTooltip(tooltip);
   }

   @Override
   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      ResourceLocation resourcelocation = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
      guiGraphics.blitSprite(RenderType::guiTextured, resourcelocation, imageX, imageY, IMAGE_WIDTH, IMAGE_HEIGHT);
      guiGraphics.blitSprite(RenderType::guiTextured, iconTexture, imageX, imageY, IMAGE_WIDTH, IMAGE_HEIGHT);
   }
}
