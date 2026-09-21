package com.uncreated.civilized.ui.menu.building;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;
import com.uncreated.civilized.ui.tabs.TabController;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The tabbed screen of a building. It is backed by {@link BuildingMenu} so that the player's inventory is available for
 * certain actions (e.g. Editing Recipe tabs). Switching between them never opens or closes a menu.
 */
public abstract class ABuildingMenuScreen extends AbstractContainerScreen<BuildingMenu> implements ITabHost {

   private static final ResourceLocation MENU_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/building_menu.png");

   private static final int CONTENT_MARGIN_X = 25;
   private static final int CONTENT_MARGIN_Y = 20;

   @Getter
   protected final BuildingScreenContext context;

   private TabController tabController;

   protected int contentLeftPos;
   protected int contentTopPos;
   protected int contentWidth;
   protected int contentHeight;

   public ABuildingMenuScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.context = menu.getContext();
      this.imageWidth = 340;
      this.imageHeight = 200;
      this.titleLabelX = 30;
      this.titleLabelY = 2;
      this.inventoryLabelX = 90;
      this.inventoryLabelY = this.imageHeight - 125;
   }

   protected abstract List<ATab> createTabs(int contentLeftPos, int contentTopPos, int tabWidth, int tabHeight);

   protected abstract List<Button> createTabButtons();

   @Override
   protected void init() {
      super.init();

      contentLeftPos = leftPos + CONTENT_MARGIN_X;
      contentTopPos = topPos + CONTENT_MARGIN_Y;
      contentWidth = imageWidth - CONTENT_MARGIN_X * 2;
      contentHeight = imageHeight - CONTENT_MARGIN_Y;

      tabController =
            new TabController(
                  createTabs(contentLeftPos, contentTopPos, contentWidth, contentHeight),
                  this::addWidget,
                  this::removeWidget);

      createTabButtons().forEach(this::addRenderableWidget);

      ATab openTab = tabController.changeToDefaultTabIfNotSet();
      menu.setPlayerInventoryVisible(openTab.showsPlayerInventory());
   }

   @Override
   public ATab changeTab(int newTabIndex) {
      ATab newTab = tabController.changeTab(newTabIndex);
      menu.setPlayerInventoryVisible(newTab.showsPlayerInventory());
      return newTab;
   }

   @Override
   public int getFirstTabButtonX() {
      return leftPos - 12;
   }

   @Override
   public int getFirstTabButtonY() {
      return topPos + 14;
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(
            RenderType::guiTextured,
            MENU_TEXTURE,
            leftPos,
            topPos,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            384,
            384);

      // the background texture has no slots painted on it, since the inventory only shows on some tabs
      for (Slot slot : menu.slots) {
         if (slot.isActive())
            SlotFrameRenderer.render(graphics, leftPos + slot.x, topPos + slot.y);
      }
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(font, title, titleLabelX, titleLabelY, Colors.MENU_TEXT_DARK, false);

      // the inventory heading would otherwise sit on its own above nothing
      if (menu.isPlayerInventoryVisible())
         graphics
               .drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Colors.MENU_TEXT_DARK, false);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);

      if (tabController != null)
         tabController.renderCurrentTabContents(graphics, mouseX, mouseY, partialTick);

      // the tooltip goes last so that it draws over the tab's contents
      renderTooltip(graphics, mouseX, mouseY);
   }

   /**
    * This override is in place for two reasons: 1) because {@link AbstractContainerScreen}'s implementation of
    * {@code mouseClicked} returns early if ANY other {@link GuiEventListener}, including the tab widget itself, which
    * ends up preventing picking up items from slots 2) because we can disable the player's inventory using
    * {@code  TogglableSlots}, we need to checking if we are hovering over a disabled inventory slot so that other
    * {@link GuiEventListener} covering it can still run. Without it, a slot may handle another
    * {@link GuiEventListener}'s {@code mouseClicked} method despite being disabled.
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

   public void refresh() {
      if (tabController != null)
         tabController.refresh();
   }
}
