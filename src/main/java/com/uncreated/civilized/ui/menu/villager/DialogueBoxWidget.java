package com.uncreated.civilized.ui.menu.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.uncreated.civilized.core.dialogue.DialogueChoice;
import com.uncreated.civilized.core.dialogue.DialogueSession;
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
   private final Component villagerSpeech;
   private final Supplier<List<DialogueSession.DisplayedChoice>> choices;
   private final Consumer<DialogueChoice> onChoice;
   private final int bottom;
   private final int speechHeight;
   private final List<Button> responseButtons = new ArrayList<>();

   private int staggeredDialogueCurrentIndex = 0;
   private int timeUntilNextStaggeredChar = 4;
   private boolean staggerDone = false;
   private boolean responsesShown = false;

   /**
    * @param choices
    *           the choices to show once the speech is typed out, which are fetched again at that point in case
    *           something has changed in the meantime
    * @param bottom
    *           the lowest Y coordinate of the card on the screen. The card is as tall as its contents.
    * @param onChoice
    *           called with the choice the player picked
    */
   public DialogueBoxWidget(
         Font font,
         Component villagerSpeech,
         Supplier<List<DialogueSession.DisplayedChoice>> choices,
         int x,
         int bottom,
         int width,
         Consumer<DialogueChoice> onChoice) {
      super(x, 0, width, 0, Component.empty());
      this.font = font;
      this.villagerSpeech = villagerSpeech;
      this.choices = choices;
      this.onChoice = onChoice;
      this.bottom = bottom;

      // the size of the widget is set according to the entire speech text up front so that the box doesn't resize while
      // the text is typed out.
      // including the quotes drawn around it, which can push a word onto another line
      Component fullSpeech = Component.literal("\"" + villagerSpeech.getString() + "\"");
      speechHeight = font.split(fullSpeech, width - PADDING * 2).size() * font.lineHeight;
      displayChoices(false);
   }

   private void displayChoices(boolean visible) {
      List<DialogueSession.DisplayedChoice> shown = choices.get();
      int responsesHeight = shown.size() * (RESPONSE_BUTTON_HEIGHT + RESPONSE_BUTTON_GAP);
      setHeight(PADDING + speechHeight + SPEECH_GAP + responsesHeight + PADDING);
      setY(bottom - height);

      responseButtons.clear();
      int buttonY = getY() + PADDING + speechHeight + SPEECH_GAP;
      for (DialogueSession.DisplayedChoice choice : shown) {
         Button button = createResponseButton(choice, buttonY);
         button.visible = visible;
         responseButtons.add(button);
         buttonY += RESPONSE_BUTTON_HEIGHT + RESPONSE_BUTTON_GAP;
      }
   }

   private Button createResponseButton(DialogueSession.DisplayedChoice choice, int y) {
      Button button =
            new ModernButton(
                  Button.builder(choice.text(), b -> onChoice.accept(choice.choice()))
                        .pos(getX() + PADDING, y)
                        .size(width - PADDING * 2, RESPONSE_BUTTON_HEIGHT));
      button.active = choice.state().isEnabled();
      if (choice.state().tooltip() != null)
         button.setTooltip(Tooltip.create(choice.state().tooltip()));
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

            currentStaggeredText = villagerSpeech.getString(staggeredDialogueCurrentIndex);

            nextCharToRender = currentStaggeredText.charAt(currentStaggeredText.length() - 1);

            if (",;-!.?".indexOf(nextCharToRender) != -1)
               break;
         }

         timeUntilNextStaggeredChar = switch (nextCharToRender) {
         case ',', ';' -> 5;
         case '-', '!', '.', '?' -> 10;
         default -> 0;
         };

         if (staggerDone && !responsesShown) {
            responsesShown = true;
            // fetched again, as a previous choice may have changed something on the server in the meantime
            displayChoices(true);
         }

         if (!staggerDone && currentStaggeredText.length() >= villagerSpeech.getString().length()) {
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
                  "\"" + villagerSpeech.getString(staggeredDialogueCurrentIndex)
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
