package com.uncreated.civilized.ui.menu.villager.tabs;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.DialogueChoice;
import com.uncreated.civilized.core.dialogue.DialogueSession;
import com.uncreated.civilized.ui.menu.villager.DialogueBoxWidget;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class DialogueTab extends ATab {

   private final @Nullable DialogueSession session;
   private final Runnable closeScreen;
   private final int bottom;

   @Nullable
   private DialogueBoxWidget dialogueBox;
   private boolean showNextDialogue;

   public DialogueTab(TabCoords tabCoords, Font font, @Nullable DialogueSession session, Runnable closeScreen) {
      super(tabCoords, font);
      this.session = session;
      this.closeScreen = closeScreen;
      this.bottom = tabCoords.contentTopPos() + tabCoords.tabHeight();
   }

   @Override
   protected void init() {
      createDialogueBox();
   }

   private void createDialogueBox() {
      dialogueBox = null;
      if (session == null)
         return;

      dialogueBox = new DialogueBoxWidget(font, session.speech(), session::choicesForCurrentPage, getX(), bottom, width, this::onChoice);
   }

   private void onChoice(DialogueChoice choice) {
      // the old box is still showing until the next tick, and its responses no longer apply
      if (showNextDialogue || session == null)
         return;

      if (!session.choose(choice))
         closeScreen.run();
      else
         // showing the next dialogue is done in the next tick to avoid replacing the box while a button's onClick
         // method is busy running
         showNextDialogue = true;
   }

   @Override
   public void tick() {
      if (showNextDialogue) {
         showNextDialogue = false;
         // remove focus that keyboard presses don't reach the old box's buttons
         setFocused(null);
         createDialogueBox();
      }

      if (dialogueBox != null)
         dialogueBox.tick();
   }

   /** Prevents the empty spaces around the dialogue box being able to receive clicks. */
   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return dialogueBox != null && dialogueBox.isMouseOver(mouseX, mouseY);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (dialogueBox != null)
         dialogueBox.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return dialogueBox == null ? List.of() : List.of(dialogueBox);
   }
}
