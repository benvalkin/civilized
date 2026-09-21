package com.uncreated.civilized.ui.menu.building;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.TabCoords;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ILazyLoadTabHost;
import com.uncreated.civilized.ui.tabs.LazyLoadTabController;

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
public abstract class ABuildingMenuScreen extends AbstractContainerScreen<BuildingMenu> implements ILazyLoadTabHost {

   private static final ResourceLocation MENU_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/building_menu.png");

   private static final int CONTENT_MARGIN_X = 25;
   private static final int CONTENT_MARGIN_Y = 20;

   @Getter
   protected final BuildingScreenContext context;

   private LazyLoadTabController tabController;

   @Getter
   protected TabCoords tabCoords;

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

   protected abstract ATab createDefaultTab(ILazyLoadTabHost tabHost);

   protected abstract List<Button> createTabButtons(ILazyLoadTabHost tabHost);

   @Override
   protected void init() {
      super.init();

      tabCoords =
            new TabCoords(
                  leftPos + CONTENT_MARGIN_X,
                  topPos + CONTENT_MARGIN_Y,
                  imageWidth - CONTENT_MARGIN_X * 2,
                  imageHeight - CONTENT_MARGIN_Y);

      tabController = new LazyLoadTabController(this::createDefaultTab, this::addWidget, this::removeWidget);

      createTabButtons(this).forEach(this::addRenderableWidget);

      ATab openTab = tabController.changeToDefaultTabIfNotSet(this);
      menu.setPlayerInventoryVisible(openTab.showsPlayerInventory());
   }

   @Override
   public ATab changeTab(ATab newTab) {
      tabController.changeTab(newTab);
      menu.setPlayerInventoryVisible(newTab.showsPlayerInventory());
      return newTab;
   }

   /**
    * Child widgets of the Building Screen (particularly buttons inside them) can change tabs. Minecraft focuses
    * whatever widget took the click once the click is done, which by then is the tab that was just switched away from
    * and is no longer on the screen. In this method override, we ensure that focus is always on the current tab -
    * otherwise, remove tabs will still receive GUI events such a mouse clicks etc.
    */
   @Override
   public void setFocused(@Nullable GuiEventListener listener) {
      if (listener instanceof ATab tab && tab != tabController.getCurrentTab())
         listener = null;

      super.setFocused(listener);
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
