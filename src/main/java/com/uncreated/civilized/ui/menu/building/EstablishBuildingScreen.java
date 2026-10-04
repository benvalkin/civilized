package com.uncreated.civilized.ui.menu.building;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.item.BuildingDeedItem;
import com.uncreated.civilized.networking.packets.CreateNewBuilding;
import com.uncreated.civilized.ui.components.multiline.ImprovedMultiLineTextWidget;
import com.uncreated.civilized.ui.menu.building.widgets.BuildingRequirementsView;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen that shows when placing and upgrading buildings.
 */
public class EstablishBuildingScreen extends Screen {
   private static final ResourceLocation BACKGROUND_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/building_deed.png");

   private final int imageWidth;
   private final int imageHeight;
   @org.jetbrains.annotations.NotNull
   private int leftPos;
   private int topPos;
   private int contentWidth;
   private int contentHeight;
   private int contentMarginX = 60;
   private int contentMarginY = 15;
   private int titleX;
   private int titleY;

   private ImprovedMultiLineTextWidget titleText;
   private Button cancel;
   private Button confirm;

   private final BuildingType buildingType;
   private final BuildingBounds bounds;
   private final List<IBuildingRequirementResult> requirements;
   private BuildingRequirementsView requirementsView;

   /**
    * @param requirements
    *           the requirements the server checked before telling the client to open this screen
    */
   public EstablishBuildingScreen(
         BuildingType buildingType,
         BuildingBounds bounds,
         List<? extends IBuildingRequirementResult> requirements) {
      super(Component.translatable("menu.building.management.create.heading", buildingType.translationDark()));
      this.buildingType = buildingType;
      this.bounds = bounds;
      this.requirements = List.copyOf(requirements);
      imageWidth = 256;
      imageHeight = 256;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      this.leftPos = (width - this.imageWidth) / 2 + contentMarginX;
      this.topPos = (height - this.imageHeight) / 2 + contentMarginY;
      this.contentWidth = imageWidth - contentMarginX * 2;
      this.contentHeight = imageHeight - 85;
      this.titleX = leftPos;
      this.titleY = topPos;

      final int marginXTitle = 10;
      titleText = new ImprovedMultiLineTextWidget(leftPos + marginXTitle, topPos + 10, getTitle(), font);
      titleText.setCentered(true);
      titleText.setMaxWidth(contentWidth - marginXTitle);
      titleText.setColor(Colors.MENU_TEXT_DARK);
      titleText.dropShadows(false);

      final int buttonMargin = 2;
      final int buttonHeight = 20;
      cancel =
            Button.builder(Component.translatable("gui.misc.button.cancel"), this::onPressCancel)
                  .pos(leftPos + buttonMargin, topPos + contentHeight - buttonMargin)
                  .size(contentWidth / 2 - buttonMargin * 2, buttonHeight)
                  .build();

      confirm =
            Button.builder(Component.translatable("gui.misc.button.confirm"), this::onPressConfirm)
                  .pos(leftPos + contentWidth / 2 + buttonMargin, topPos + contentHeight - buttonMargin)
                  .size(contentWidth / 2 - buttonMargin * 2, buttonHeight)
                  .build();

      // the heading sits where it always has, with the list below it
      requirementsView =
            new BuildingRequirementsView(leftPos, topPos + 35, contentWidth, contentHeight - buttonMargin - 5, font);
      requirementsView.setResults(requirements);
      requirementsView.updateConfirmButton(confirm);

      addRenderableOnly(titleText);
      addRenderableWidget(cancel);
      addRenderableWidget(confirm);
      addRenderableWidget(requirementsView);
   }

   private void onPressCancel(Button button) {
      onClose();
   }

   /**
    * Closing without confirming (by pressing Escape) keeps the dragged bounds, so it's useful to explain to the player
    * that they have a way to bring up the screen again.
    */
   @Override
   public void onClose() {
      super.onClose();

      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null)
         player.displayClientMessage(
               Component.translatable("message.building.placement.help.placed_destination")
                     .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
               true);
   }

   private void onPressConfirm(Button button) {

      Minecraft.getInstance().setScreen(null);

      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null)
         return;

      // the server checks the placement and requirements again and uses up the deed, if the building is established
      ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (itemInHand.getItem() instanceof BuildingDeedItem buildingDeed
            && buildingDeed.getBuildingType() == buildingType)
         PacketDistributor.sendToServer(new CreateNewBuilding(buildingType, bounds));

      BuildingBoundsDragTool.resetDragging();
   }

   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      graphics.blit(
            RenderType::guiTextured,
            BACKGROUND_TEXTURE,
            i,
            j,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            256,
            256);
   }
}
