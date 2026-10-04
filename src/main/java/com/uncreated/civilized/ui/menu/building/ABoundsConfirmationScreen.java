package com.uncreated.civilized.ui.menu.building;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
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

/**
 * Shows the requirements for bounds a player dragged out, which the server has already checked, and lets them confirm
 * the bounds once every requirement is met. Used when establishing a building and when redrawing one.
 */
public abstract class ABoundsConfirmationScreen extends Screen {
   private static final ResourceLocation BACKGROUND_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/building_deed.png");

   private static final int IMAGE_WIDTH = 256;
   private static final int IMAGE_HEIGHT = 256;
   private static final int CONTENT_MARGIN_X = 60;
   private static final int CONTENT_MARGIN_Y = 15;

   private final List<IBuildingRequirementResult> requirements;

   private BuildingRequirementsView requirementsView;
   private Button confirm;

   /**
    * @param requirements
    *           the requirements the server checked before telling the client to open this screen
    */
   protected ABoundsConfirmationScreen(Component title, List<? extends IBuildingRequirementResult> requirements) {
      super(title);
      this.requirements = List.copyOf(requirements);
   }

   /** Sends the confirmed bounds to the server, which checks everything again. The screen is already closed. */
   protected abstract void onConfirm();

   /**
    * Shown when the player closes the screen without confirming. The dragged bounds are kept, so this tells them how to
    * bring the screen back.
    */
   protected abstract Component closedWithoutConfirmingMessage();

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   protected void init() {
      super.init();
      int leftPos = (width - IMAGE_WIDTH) / 2 + CONTENT_MARGIN_X;
      int topPos = (height - IMAGE_HEIGHT) / 2 + CONTENT_MARGIN_Y;
      int contentWidth = IMAGE_WIDTH - CONTENT_MARGIN_X * 2;
      int contentHeight = IMAGE_HEIGHT - 85;

      final int marginXTitle = 10;
      ImprovedMultiLineTextWidget titleText =
            new ImprovedMultiLineTextWidget(leftPos + marginXTitle, topPos + 10, getTitle(), font);
      titleText.setCentered(true);
      titleText.setMaxWidth(contentWidth - marginXTitle);
      titleText.setColor(Colors.MENU_TEXT_DARK);
      titleText.dropShadows(false);

      final int buttonMargin = 2;
      final int buttonHeight = 20;
      Button cancel =
            Button.builder(Component.translatable("gui.misc.button.cancel"), button -> onClose())
                  .pos(leftPos + buttonMargin, topPos + contentHeight - buttonMargin)
                  .size(contentWidth / 2 - buttonMargin * 2, buttonHeight)
                  .build();

      confirm =
            Button.builder(Component.translatable("gui.misc.button.confirm"), button -> confirm())
                  .pos(leftPos + contentWidth / 2 + buttonMargin, topPos + contentHeight - buttonMargin)
                  .size(contentWidth / 2 - buttonMargin * 2, buttonHeight)
                  .build();

      requirementsView =
            new BuildingRequirementsView(leftPos, topPos + 35, contentWidth, contentHeight - buttonMargin - 5, font);
      requirementsView.setResults(requirements);
      requirementsView.updateConfirmButton(confirm);

      addRenderableOnly(titleText);
      addRenderableWidget(cancel);
      addRenderableWidget(confirm);
      addRenderableWidget(requirementsView);
   }

   private void confirm() {
      // closed without onClose(), so that the "closed without confirming" message isn't shown
      Minecraft.getInstance().setScreen(null);
      onConfirm();
   }

   @Override
   public void onClose() {
      super.onClose();

      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null)
         player.displayClientMessage(closedWithoutConfirmingMessage(), true);
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.blit(
            RenderType::guiTextured,
            BACKGROUND_TEXTURE,
            (width - IMAGE_WIDTH) / 2,
            (height - IMAGE_HEIGHT) / 2,
            0.0F,
            0.0F,
            IMAGE_WIDTH,
            IMAGE_HEIGHT,
            256,
            256);
   }
}
