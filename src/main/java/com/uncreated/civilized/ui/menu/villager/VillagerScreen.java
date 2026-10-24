package com.uncreated.civilized.ui.menu.villager;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.Dialogue;
import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.networking.packets.VillagerDialogueScreenToggled;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class VillagerScreen extends Screen {

   // todo: placeholder look until there are textures for these panels
   public static final int PANEL_COLOUR = 0xB0000000;
   public static final int UNSELECTED_TAB_COLOUR = 0x70000000;
   public static final int HOVERED_TAB_COLOUR = 0x90000000;
   public static final int DISABLED_TAB_OVERLAY = 0x80000000;

   private static final int SCREEN_MARGIN = 8;
   private static final int BOTTOM_MARGIN = 30;
   private static final int PANEL_GAP = 6;
   private static final int TAB_GAP = 2;

   private final CivilizedVillager villager;
   @Nullable
   private IVillageDialogue dialogue;
   @Nullable
   private final DialogueContext context;

   @Nullable
   private DialogueBoxWidget dialogueBox;

   public VillagerScreen(
         CivilizedVillager villager,
         @Nullable IVillageDialogue dialogue,
         @Nullable DialogueContext context) {
      super(Component.literal(villager.getInfo().getFullName()));
      this.villager = villager;
      this.dialogue = dialogue;
      this.context = context;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   protected void init() {
      super.init();
      int panelBottom = height - BOTTOM_MARGIN;

      VillagerInfoCardWidget infoCard = new VillagerInfoCardWidget(font, villager, SCREEN_MARGIN, panelBottom);
      addRenderableWidget(infoCard);

      createTabs(infoCard.getX(), infoCard.getY() - VillagerTabButton.SIZE);

      if (dialogue != null && context != null) {

         // the furthest possible X coordinate that the dialogue box can start at
         int dialogueLeftPosLimit = infoCard.getRight() + PANEL_GAP;
         int dialogueWidth = Math.min(DialogueBoxWidget.PREFERRED_WIDTH, width - SCREEN_MARGIN - dialogueLeftPosLimit);
         // the dialogue box's left pos starts at the end the info card (plus a margin), and its width is set to
         // whatever
         // value would make it center aligned on the main screen.
         // This makes it nicely center aligned in the main screen whilst also fixed to the end of the info card.
         int dialogueX = Math.max(dialogueLeftPosLimit, (width - dialogueWidth) / 2);
         dialogueBox =
               new DialogueBoxWidget(
                     font,
                     dialogue,
                     context,
                     dialogueX,
                     panelBottom,
                     dialogueWidth,
                     this::onResponseOptionPressed);
         addRenderableWidget(dialogueBox);
      }

      clientNotifyServerScreenOpen(villager);
   }

   private void createTabs(int x, int y) {
      addTab(x, y, new ItemStack(Items.WRITABLE_BOOK), "talk", true, true);
      x += VillagerTabButton.SIZE + TAB_GAP;
      addTab(x, y, new ItemStack(Items.EXPERIENCE_BOTTLE), "skills", false, false);
      x += VillagerTabButton.SIZE + TAB_GAP;
      addTab(x, y, new ItemStack(Items.IRON_CHESTPLATE), "equipment", false, false);
      x += VillagerTabButton.SIZE + TAB_GAP;
      addTab(x, y, new ItemStack(Items.RED_BED), "family", false, false);
   }

   private void addTab(int x, int y, ItemStack icon, String key, boolean selected, boolean available) {
      Component name = Component.translatable("menu.villager.tab." + key);
      VillagerTabButton tab = new VillagerTabButton(x, y, icon, name, selected, () -> {
      });
      tab.active = available;
      if (!available)
         tab.setTooltip(Tooltip.create(name.copy().append(Component.translatable("menu.villager.tab.coming_soon"))));
      addRenderableWidget(tab);
   }

   private void onResponseOptionPressed(ResponseOptionContext optionContext) {
      ResponseOption.SelectedAction result =
            optionContext.getSelectedOption().getOnPress().onOptionSelected(optionContext);

      if (dialogue == null)
         return;

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

   @Override
   public void tick() {

      if (dialogueBox != null)
         dialogueBox.tick();
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
