package com.uncreated.civilized.ui.menu.villager.tabs;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.ui.menu.villager.DialogueBoxWidget;
import com.uncreated.civilized.ui.menu.villager.VillagerConversation;
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
      if (!conversation.hasDialogue())
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
      ResponseOption.SelectedAction result = conversation.respond(optionContext);
      if (result == ResponseOption.SelectedAction.CLOSE_DIALOGUE)
         closeScreen.run();
      else if (result == ResponseOption.SelectedAction.GO_NEXT)
         createDialogueBox();
   }

   @Override
   public void tick() {
      if (dialogueBox != null)
         dialogueBox.tick();
   }

   /** Prevents the empty spaces around the dialogue box being able to receive clicks. */
   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return dialogueBox != null && dialogueBox.isMouseOver(mouseX, mouseY);
   }

   /**
    * A Moving on to the next dialogue replaces the dialogue box while one of its buttons is being clicked. Minecraft
    * then focuses the old box once the click is done, which would let keyboard presses reach its buttons, so it is
    * ignored.
    */
   @Override
   public void setFocused(@Nullable GuiEventListener listener) {
      if (listener != null && listener != dialogueBox)
         listener = null; // todo: not mad about this. is there not another way this can be done? Could we perhaps delay
                          // the replacement by a click? Maybe we don't instantiate a brand new Dialogue box and just
                          // clear it? Might not be possible

      super.setFocused(listener);
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
