package com.uncreated.civilized.ui.menu.building;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirements;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;
import com.uncreated.civilized.ui.tabs.TabController;
import com.uncreated.civilized.ui.tabs.TabCoords;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
public abstract class ABuildingMenuScreen extends AbstractContainerScreen<BuildingMenu>
      implements ITabHost, IBuildingScreen {

   private static final ResourceLocation MENU_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/building_menu.png");

   private static final int CONTENT_MARGIN_X = 25;
   private static final int CONTENT_MARGIN_Y = 20;
   private static final int UPGRADE_BUTTON_WIDTH = 80;
   private static final int UPGRADE_BUTTON_HEIGHT = 18;

   @Getter
   protected final BuildingScreenContext context;

   private TabController tabController;
   private Button upgradeButton;

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
      this.inventoryLabelY = this.imageHeight - 107;
   }

   protected abstract ATab createDefaultTab(ITabHost tabHost);

   protected abstract List<Button> createTabButtons(ITabHost tabHost);

   @Override
   protected void init() {
      super.init();

      tabCoords =
            new TabCoords(
                  leftPos + CONTENT_MARGIN_X,
                  topPos + CONTENT_MARGIN_Y,
                  imageWidth - CONTENT_MARGIN_X * 2,
                  imageHeight - CONTENT_MARGIN_Y * 2);

      tabController = new TabController(this::createDefaultTab, this::addWidget, this::removeWidget);

      createTabButtons(this).forEach(this::addRenderableWidget);

      // added before the tab, so that it gets clicks before the tab does
      upgradeButton =
            Button.builder(
                  Component.translatable("menu.building.upgrade.button"),
                  button -> changeTab(new UpgradeBuildingTab(this, font, context)))
                  .pos(
                        tabCoords.contentLeftPos() + tabCoords.tabWidth() - UPGRADE_BUTTON_WIDTH,
                        tabCoords.contentTopPos() + tabCoords.tabHeight() - UPGRADE_BUTTON_HEIGHT)
                  .size(UPGRADE_BUTTON_WIDTH, UPGRADE_BUTTON_HEIGHT)
                  .build();
      addRenderableWidget(upgradeButton);

      ATab openTab = tabController.changeToDefaultTabIfNotSet(this);
      menu.setPlayerInventoryVisible(openTab.showsPlayerInventory());
      updateUpgradeButton();
   }

   @Override
   public ATab changeTab(ATab newTab) {
      tabController.changeTab(newTab);
      menu.setPlayerInventoryVisible(newTab.showsPlayerInventory());
      updateUpgradeButton();
      return newTab;
   }

   /**
    * Only shows the Upgrade button on the info tab, and only lets players with permission to create buildings press
    * it, while the building has a level to upgrade to.
    */
   private void updateUpgradeButton() {
      if (upgradeButton == null)
         return;

      upgradeButton.visible = getCurrentTab() instanceof IBuildingInfoTab;

      Building building = context.building();
      boolean canUpgrade =
            BuildingRequirements.find(building.getBuildingType(), building.getUpgradeLevel() + 1).isPresent();
      boolean hasPermission =
            Minecraft.getInstance().player != null
                  && context.permissions().hasCreateBuildingsPermission(Minecraft.getInstance().player.getUUID());

      upgradeButton.active = canUpgrade && hasPermission;
      if (!canUpgrade)
         upgradeButton.setTooltip(
               Tooltip.create(
                     Component.translatable("menu.building.upgrade.highest_level", building.getUpgradeLevel())));
      else if (!hasPermission)
         upgradeButton.setTooltip(Tooltip.create(Component.translatable("menu.building.upgrade.no_permission")));
      else
         upgradeButton.setTooltip(
               Tooltip.create(
                     Component.translatable(
                           "menu.building.upgrade.levels",
                           building.getUpgradeLevel(),
                           building.getUpgradeLevel() + 1)));
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

   public @Nullable ATab getCurrentTab() {
      return tabController.getCurrentTab();
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
      // commented out the below line as it might be better for GUI space not to render the the building name at the top
      // graphics.drawString(font, title, titleLabelX, titleLabelY, Colors.MENU_TEXT_DARK, false);

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

   @Override
   public void refresh() {
      if (tabController != null)
         tabController.refresh();

      // the building's level or the player's permissions may have changed
      updateUpgradeButton();
   }
}
