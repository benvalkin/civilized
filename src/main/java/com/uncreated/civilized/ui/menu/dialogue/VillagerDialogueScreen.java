package com.uncreated.civilized.ui.menu.dialogue;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.dialogue.Dialogue;
import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.networking.packets.VillagerDialogueScreenToggled;
import com.uncreated.civilized.ui.StringRenderHelper;
import com.uncreated.civilized.ui.components.buttons.ModernButton;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class VillagerDialogueScreen extends Screen {
   private static final ResourceLocation BACKGROUND_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/villager_dialogue.png");

   private final int imageWidth;
   private final int imageHeight;
   private int leftPos;
   private int topPos;
   private int contentWidth;
   private int contentHeight;
   private int contentMarginX = 120;
   private int contentMarginY = 150;
   private int titleX;
   private int titleY;

   private List<Button> responseButtons;

   private final CivilizedVillager villager;
   private IVillageDialogue dialogue;
   private final DialogueContext context;

   MutableComponent villagerName;

   public VillagerDialogueScreen(
         CivilizedVillager villager,
         @NotNull IVillageDialogue dialogue,
         DialogueContext context) {
      super(Component.translatable(villager.getInfo().getFullName()));
      this.villager = villager;
      this.dialogue = dialogue;
      this.context = context;
      imageWidth = 512;
      imageHeight = 512;
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
      this.contentHeight = imageHeight - 125;
      this.titleX = leftPos;
      this.titleY = topPos;

      final int buttonStartY = 100;
      final int buttonMarginX = 20;
      final int buttonHeight = 15;

      villagerName = Component.literal(villager.getInfo().getFullName()).withStyle(ChatFormatting.UNDERLINE);

      responseButtons = new ArrayList<>();
      int currentButtonHeight = topPos + buttonStartY;
      for (ResponseOption responseOption : dialogue.getResponseOptions()) {
         ResponseOptionContext optionContext = new ResponseOptionContext(responseOption, context);

         boolean shouldShow = responseOption.getVisibleCheck().isOptionVisible(optionContext);
         if (!shouldShow)
            continue;

         Button button =
               new ModernButton(
                     Button.builder(responseOption.getPlayerSpeech(), b -> onResponseOptionPressed(b, optionContext))
                           .pos(leftPos + buttonMarginX, currentButtonHeight)
                           .width(contentWidth - buttonMarginX * 2));

         currentButtonHeight += button.getHeight();

         ResponseOption.EnabledCheckResult enabledResult =
               responseOption.getEnabledCheck().isOptionEnabled(optionContext);
         button.active = enabledResult.isEnabled();

         // tooltip from option enabled check takes precedence over option's preset tooltip
         if (enabledResult.getTooltip() != null)
            button.setTooltip(Tooltip.create(enabledResult.getTooltip()));
         else if (responseOption.getTooltip() != null)
            button.setTooltip(Tooltip.create(responseOption.getTooltip()));

         responseButtons.add(button);
         addRenderableWidget(button);
      }

      staggeredDialogueCurrentIndex = 0;
      timeUntilNextStaggeredChar = 5;
      staggerDone = false;
      responseButtons.forEach(b -> b.visible = false);

      clientNotifyServerScreenOpen(villager);
   }

   private void onResponseOptionPressed(Button button, ResponseOptionContext optionContext) {
      ResponseOption.SelectedAction result =
            optionContext.getSelectedOption().getOnPress().onOptionSelected(optionContext);
      if (result == ResponseOption.SelectedAction.DO_NOTHING)
         return;

      dialogue.getOnEnded().accept(context);
      if (result == ResponseOption.SelectedAction.CLOSE_DIALOGUE) {
         Minecraft.getInstance().setScreen(null);
         PacketDistributor.sendToServer(new VillagerDialogueScreenToggled(villager.getUUID(), false));
      } else if (result == ResponseOption.SelectedAction.GO_NEXT) {
         @Nullable
         Dialogue next = null;
         // response option's dialogue takes precedence over pages
         if (optionContext.getSelectedOption().getNextDialogue() != null)
            next = optionContext.getSelectedOption().getNextDialogue();
         else if (dialogue.hasNextPage())
            next = dialogue.getNextPage();

         // check availability and fallbacks
         while (next != null && !next.isAvailableToPlayer(context)) {
            next = next.getFallback();
         }

         // finally, if there is still dialogue to display, close the screen
         if (next == null) {
            Minecraft.getInstance().setScreen(null);
            return;
         }

         dialogue = next;
         rebuildWidgets();
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      // this.renderBackground(graphics, mouseX, mouseY, partialTicks);
      responseButtons.forEach(b -> b.render(graphics, mouseX, mouseY, partialTicks));
      this.renderLabels(graphics, mouseX, mouseY, partialTicks);
   }

   private int staggeredDialogueCurrentIndex;
   private int timeUntilNextStaggeredChar;
   private boolean staggerDone;

   @Override
   public void tick() {

      if (timeUntilNextStaggeredChar == 0) {

         String currentStaggeredText = "";
         char nextCharToRender = ' ';

         int charsPerStagger = 2;
         for (int i = 0; i < charsPerStagger; i++) {

            staggeredDialogueCurrentIndex++;

            currentStaggeredText = dialogue.getVillagerSpeech().getString(staggeredDialogueCurrentIndex);

            nextCharToRender = currentStaggeredText.charAt(currentStaggeredText.length() - 1);

            if (",;-!.?".indexOf(nextCharToRender) != -1)
               break;
         }

         timeUntilNextStaggeredChar = switch (nextCharToRender) {
         case ',', ';' -> 5;
         case '-', '!', '.', '?' -> 10;
         default -> 0;
         };

         if (staggerDone) {
            responseButtons.forEach(b -> b.visible = true);
         }

         if (!staggerDone && currentStaggeredText.length() >= dialogue.getVillagerSpeech().getString().length()) {
            staggerDone = true;
            timeUntilNextStaggeredChar = 15; // when stagger is done, wait a bit before showing response options
         }

         return;
      }

      timeUntilNextStaggeredChar--;
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      StringRenderHelper.drawCenterAlignedWordWrap(
            graphics,
            this.font,
            villagerName,
            leftPos + contentWidth / 2,
            topPos,
            contentWidth,
            Colors.MENU_TEXT_VILLAGER_DIALOGUE,
            true);

      int villagerSpeechHeight =
            StringRenderHelper.drawLeftAlignedWordWrap(
                  graphics,
                  this.font,
                  Component.literal(dialogue.getVillagerSpeech().getString(staggeredDialogueCurrentIndex)),
                  leftPos,
                  topPos + 30,
                  contentWidth,
                  Colors.MENU_TEXT_VILLAGER_DIALOGUE,
                  true);
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
            512,
            512);
   }

   @Override
   public void onClose() {
      super.onClose();
      clientNotifyServerScreenClosed(villager);
   }

   private void clientNotifyServerScreenOpen(CivilizedVillager villager) {
      PacketDistributor.sendToServer(new VillagerDialogueScreenToggled(villager.getUUID(), true));
   }

   private void clientNotifyServerScreenClosed(CivilizedVillager villager) {
      PacketDistributor.sendToServer(new VillagerDialogueScreenToggled(villager.getUUID(), false));
   }
}
