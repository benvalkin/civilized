package com.uncreated.civilized.ui.menu.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.ui.components.buttons.ModernButton;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class DialogueBoxWidget extends AbstractContainerWidget {

   public static final int PREFERRED_WIDTH = 320;

   private static final int PADDING = 8;
   private static final int SPEECH_GAP = 6;
   private static final int RESPONSE_BUTTON_HEIGHT = 20;
   private static final int RESPONSE_BUTTON_GAP = 2;

   private final Font font;
   private final IVillageDialogue dialogue;
   private final List<Button> responseButtons = new ArrayList<>();

   private int staggeredDialogueCurrentIndex = 0;
   private int timeUntilNextStaggeredChar = 5;
   private boolean staggerDone = false;

   /**
    * @param bottom
    *           the lowest Y coordinate of the card on the screen. The card is as tall as its contents.
    * @param onResponse
    *           called with the response the player picked
    */
   public DialogueBoxWidget(
         Font font,
         IVillageDialogue dialogue,
         DialogueContext context,
         int x,
         int bottom,
         int width,
         Consumer<ResponseOptionContext> onResponse) {
      super(x, 0, width, 0, Component.empty());
      this.font = font;
      this.dialogue = dialogue;

      List<ResponseOptionContext> visibleResponses = findVisibleResponses(dialogue, context);

      // the size of the widget is set according to the entire speech text up front so that the box doesn't resize while
      // the text is typed out.
      // including the quotes drawn around it, which can push a word onto another line
      Component fullSpeech = Component.literal("\"" + dialogue.getVillagerSpeech().getString() + "\"");
      int speechHeight = font.split(fullSpeech, width - PADDING * 2).size() * font.lineHeight;
      int responsesHeight = visibleResponses.size() * (RESPONSE_BUTTON_HEIGHT + RESPONSE_BUTTON_GAP);
      setHeight(PADDING + speechHeight + SPEECH_GAP + responsesHeight + PADDING);
      setY(bottom - height);

      int buttonY = getY() + PADDING + speechHeight + SPEECH_GAP;
      for (ResponseOptionContext response : visibleResponses) {
         responseButtons.add(createResponseButton(response, buttonY, onResponse));
         buttonY += RESPONSE_BUTTON_HEIGHT + RESPONSE_BUTTON_GAP;
      }
   }

   private static List<ResponseOptionContext> findVisibleResponses(IVillageDialogue dialogue, DialogueContext context) {
      List<ResponseOptionContext> visible = new ArrayList<>();
      for (ResponseOption responseOption : dialogue.getResponseOptions()) {
         ResponseOptionContext optionContext = new ResponseOptionContext(responseOption, context);
         if (responseOption.getVisibleCheck().isOptionVisible(optionContext))
            visible.add(optionContext);
      }
      return visible;
   }

   private Button createResponseButton(
         ResponseOptionContext optionContext,
         int y,
         Consumer<ResponseOptionContext> onResponse) {
      ResponseOption responseOption = optionContext.getSelectedOption();
      Button button =
            new ModernButton(
                  Button.builder(responseOption.getPlayerSpeech(), b -> onResponse.accept(optionContext))
                        .pos(getX() + PADDING, y)
                        .size(width - PADDING * 2, RESPONSE_BUTTON_HEIGHT));

      ResponseOption.EnabledCheckResult enabledResult = responseOption.getEnabledCheck().isOptionEnabled(optionContext);
      button.active = enabledResult.isEnabled();

      // if there's a tooltip displayed by isOptionEnabled check, show it instead of the option's preset tooltip
      if (enabledResult.getTooltip() != null)
         button.setTooltip(Tooltip.create(enabledResult.getTooltip()));
      else if (responseOption.getTooltip() != null)
         button.setTooltip(Tooltip.create(responseOption.getTooltip()));

      // shown once the villager has finished speaking
      button.visible = false;
      return button;
   }

   /** Types out the speech, pausing for punctuation, and shows the responses once it's done. */
   public void tick() {

      if (timeUntilNextStaggeredChar == 0) {

         String currentStaggeredText = "\"";
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
            currentStaggeredText += "";
            timeUntilNextStaggeredChar = 15; // when stagger is done, wait a bit before showing response options
         }

         return;
      }

      timeUntilNextStaggeredChar--;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(getX(), getY(), getRight(), getBottom(), VillagerScreen.PANEL_COLOUR);

      Component speech =
            Component.literal(
                  "\"" + dialogue.getVillagerSpeech().getString(staggeredDialogueCurrentIndex)
                        + (staggerDone ? "\"" : ""));
      int y = getY() + PADDING;
      for (FormattedCharSequence line : font.split(speech, width - PADDING * 2)) {
         graphics.drawString(font, line, getX() + PADDING, y, Colors.MENU_TEXT_VILLAGER_DIALOGUE, true);
         y += font.lineHeight;
      }

      responseButtons.forEach(button -> button.render(graphics, mouseX, mouseY, partialTick));
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return responseButtons;
   }

   @Override
   protected int contentHeight() {
      return height;
   }

   @Override
   protected double scrollRate() {
      return 0;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }
}
