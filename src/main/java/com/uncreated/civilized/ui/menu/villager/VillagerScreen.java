package com.uncreated.civilized.ui.menu.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.VillagerConversation;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.menu.villager.tabs.DialogueTab;
import com.uncreated.civilized.ui.menu.villager.tabs.EquipmentTab;
import com.uncreated.civilized.ui.menu.villager.tabs.FamilyTab;
import com.uncreated.civilized.ui.menu.villager.tabs.SkillsTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;
import com.uncreated.civilized.ui.tabs.TabController;
import com.uncreated.civilized.ui.tabs.TabCoords;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class VillagerScreen extends AbstractContainerScreen<VillagerMenu> implements ITabHost {

   public static final int PANEL_COLOUR = 0xB0000000;
   public static final int UNSELECTED_TAB_COLOUR = 0x70000000;
   public static final int HOVERED_TAB_COLOUR = 0x90000000;
   public static final int DISABLED_TAB_OVERLAY = 0x80000000;

   private static final int SCREEN_MARGIN = 8;
   private static final int BOTTOM_MARGIN = 30;
   private static final int PANEL_GAP = 6;
   private static final int TAB_GAP = 2;
   private static final int PREFERRED_CONTENT_WIDTH = DialogueBoxWidget.PREFERRED_WIDTH;

   private final CivilizedVillager villager;
   private final VillagerConversation conversation;

   private TabController tabController;
   private final List<VillagerTabButton> tabButtons = new ArrayList<>();
   private VillagerInfoCardWidget infoCard;

   @Getter
   private TabCoords tabCoords;

   public VillagerScreen(VillagerMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.villager = menu.getVillager();
      // recompute the opening dialogue on the client
      this.conversation = VillagerConversation.openingDialogue(villager, playerInventory.player, menu.getHand());
      this.imageHeight = VillagerMenu.CONTENT_HEIGHT;
      // standard inventory label height
      this.inventoryLabelY = this.imageHeight - 94;
   }

   @Override
   protected void init() {
      // centres the container area, which is moved next to the info card below
      super.init();
      int panelBottom = height - BOTTOM_MARGIN;

      infoCard = new VillagerInfoCardWidget(font, villager, SCREEN_MARGIN, panelBottom);
      addRenderableWidget(infoCard);

      // the leftmost possible X coordinate the content panel can start (starts at the end of the info card)
      int contentLeftPosLimit = infoCard.getRight() + PANEL_GAP;
      imageWidth = Math.min(PREFERRED_CONTENT_WIDTH, width - SCREEN_MARGIN - contentLeftPosLimit);
      // the content's left pos starts at the end the info card (plus a margin), and its width is set to
      // whatever value would make it center aligned on the main screen.
      // This makes it nicely center aligned in the main screen whilst also fixed to the end of the info card.
      leftPos = Math.max(contentLeftPosLimit, (width - imageWidth) / 2);
      topPos = panelBottom - imageHeight;
      tabCoords = new TabCoords(leftPos, topPos, imageWidth, imageHeight);

      tabController = new TabController(this::createDialogueTab, this::addWidget, this::removeWidget);
      createTabButtons(infoCard.getX(), infoCard.getY() - VillagerTabButton.SIZE);

      onTabOpened(tabController.changeToDefaultTabIfNotSet(this));
   }

   private ATab createDialogueTab(ITabHost tabHost) {
      return new DialogueTab(tabCoords, font, conversation, this::onClose);
   }

   private void createTabButtons(int x, int y) {
      // the old buttons were removed from the screen when it was resized
      tabButtons.clear();
      addTabButton(x, y, new ItemStack(Items.WRITABLE_BOOK), "talk", DialogueTab.class, () -> createDialogueTab(this));
      x += VillagerTabButton.SIZE + TAB_GAP;
      addTabButton(
            x,
            y,
            new ItemStack(Items.EXPERIENCE_BOTTLE),
            "skills",
            SkillsTab.class,
            () -> new SkillsTab(tabCoords, font));
      x += VillagerTabButton.SIZE + TAB_GAP;
      addTabButton(
            x,
            y,
            new ItemStack(Items.IRON_CHESTPLATE),
            "equipment",
            EquipmentTab.class,
            () -> new EquipmentTab(tabCoords, font));
      x += VillagerTabButton.SIZE + TAB_GAP;
      addTabButton(x, y, new ItemStack(Items.RED_BED), "family", FamilyTab.class, () -> new FamilyTab(tabCoords, font));
   }

   private void addTabButton(
         int x,
         int y,
         ItemStack icon,
         String key,
         Class<? extends ATab> tabType,
         Supplier<ATab> createTab) {
      Component name = Component.translatable("menu.villager.tab." + key);
      VillagerTabButton button = new VillagerTabButton(x, y, icon, name, tabType, () -> changeTab(createTab.get()));
      tabButtons.add(button);
      addRenderableWidget(button);
   }

   @Override
   public ATab changeTab(ATab newTab) {
      tabController.changeTab(newTab);
      onTabOpened(newTab);
      return newTab;
   }

   private void onTabOpened(ATab openTab) {
      menu.setPlayerInventoryVisible(openTab.showsPlayerInventory());
      for (VillagerTabButton button : tabButtons)
         button.refreshSelected(openTab);
   }

   public @Nullable ATab getCurrentTab() {
      return tabController.getCurrentTab();
   }

   @Override
   public int getFirstTabButtonX() {
      return infoCard.getX();
   }

   @Override
   public int getFirstTabButtonY() {
      return infoCard.getY() - VillagerTabButton.SIZE;
   }

   @Override
   protected void containerTick() {
      ATab currentTab = getCurrentTab();
      if (currentTab != null)
         currentTab.tick();
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBg(graphics, partialTick, mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      ATab currentTab = getCurrentTab();
      if (currentTab != null) // this screen supports tabs can have their own background
         currentTab.renderBackground(graphics, mouseX, mouseY, partialTick);

      for (Slot slot : menu.slots) {
         if (slot.isActive()) // conditional inventory slot rendering
            SlotFrameRenderer.render(graphics, leftPos + slot.x, topPos + slot.y);
      }
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {

      if (menu.isPlayerInventoryVisible())
         graphics.drawString(
               font,
               playerInventoryTitle,
               inventoryLabelX,
               inventoryLabelY,
               Colors.MENU_TEXT_VILLAGER_DIALOGUE,
               false);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);

      if (tabController != null)
         tabController.renderCurrentTabContents(graphics, mouseX, mouseY, partialTick);

      renderTooltip(graphics, mouseX, mouseY);
   }

   /**
    * Disallows clicks that occur over info card/tab buttons. It is in place primarily to prevent mouse carried items
    * getting dropped on the ground if you click them over the info card.
    */
   @Override
   protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
      boolean isOverCardOrTabs =
            mouseX >= infoCard.getX() && mouseX < infoCard.getRight() && mouseY >= getFirstTabButtonY()
                  && mouseY < infoCard.getBottom();
      return !isOverCardOrTabs && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
   }

   /**
    * This method is overridden for a funny reason. See {@code ABuildingMenuScreen.getChildAt} for more details.
    */
   @Override
   public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
      if (isOverActiveSlot(mouseX, mouseY))
         return Optional.empty();

      return super.getChildAt(mouseX, mouseY);
   }

   private boolean isOverActiveSlot(double mouseX, double mouseY) {
      for (Slot slot : menu.slots) {
         if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY))
            return true;
      }

      return false;
   }
}
