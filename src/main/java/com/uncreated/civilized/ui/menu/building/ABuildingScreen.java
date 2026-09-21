package com.uncreated.civilized.ui.menu.building;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.inn.InnBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.ResidenceBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.BakeryBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.BlacksmithBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.ButcheryBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.CraftsmanHouseBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.MasonBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.WorksiteBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.AnimalFarmBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.CropFarmBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.grove.GroveBuildingScreen;
import com.uncreated.civilized.ui.tabs.AScreenWithTabs;
import com.uncreated.civilized.ui.tabs.TabCoords;
import com.uncreated.civilized.ui.tabs.ITabHost;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class ABuildingScreen extends AScreenWithTabs {
   private static final ResourceLocation MENU_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/building_menu.png");

   private static final int CONTENT_MARGIN_X = 25;
   private static final int CONTENT_MARGIN_Y = 20;

   @Getter
   protected final BuildingScreenContext context;

   @Getter
   protected TabCoords tabCoords;

   public ABuildingScreen(BuildingScreenContext context, Component title) {
      super(title, 340, 200);
      this.context = context;
   }

   protected abstract List<Button> createTabButtons(ITabHost tabHost);

   protected void init() {
      super.init();

      tabCoords =
            new TabCoords(
                  leftPos + CONTENT_MARGIN_X,
                  topPos + CONTENT_MARGIN_Y,
                  imageWidth - CONTENT_MARGIN_X * 2,
                  imageHeight - CONTENT_MARGIN_Y * 2);

      createTabButtons(this).forEach(this::addRenderableWidget);

      changeToDefaultTabIfNotSet();
   }

   @Override
   public int getFirstTabButtonX() {
      return leftPos - 12;
   }

   @Override
   public int getFirstTabButtonY() {
      return topPos + 14;
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float mouseX, int mouseY, int partialTicks) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      graphics
            .blit(RenderType::guiTextured, MENU_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 384, 384);
   }
}
