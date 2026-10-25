package com.uncreated.civilized.ui.menu.villager.tabs;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.VillagerConversation;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.ui.menu.villager.DialogueBoxWidget;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class DialogueTab extends ATab {

   private final VillagerConversation conversation;
   private final Runnable closeScreen;
   private final int bottom;

   @Nullable
   private DialogueBoxWidget dialogueBox;
   private boolean showNextDialogue;

   public DialogueTab(TabCoords tabCoords, Font font, VillagerConversation conversation, Runnable closeScreen) {
      super(tabCoords, font);
      this.conversation = conversation;
      this.closeScreen = closeScreen;
      this.bottom = tabCoords.contentTopPos() + tabCoords.tabHeight();
   }

   @Override
   protected void init() {
      createDialogueBox();
   }

   private void createDialogueBox() {
      dialogueBox = null;
      if (!conversation.hasAnySpeech())
         return;

      dialogueBox =
            new DialogueBoxWidget(
                  font,
                  conversation.getDialogue(),
                  conversation.getContext(),
                  getX(),
                  bottom,
                  width,
                  this::onResponseOptionPressed);
   }

   private void onResponseOptionPressed(ResponseOptionContext optionContext) {
      // the old box is still showing until the next tick, and its responses no longer apply
      if (showNextDialogue)
         return;

      ResponseOption.SelectedAction result = conversation.respond(optionContext);
      if (result == ResponseOption.SelectedAction.CLOSE_DIALOGUE)
         closeScreen.run();
      else if (result == ResponseOption.SelectedAction.GO_NEXT)
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
