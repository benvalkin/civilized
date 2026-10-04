package com.uncreated.civilized.ui.menu.building;

import java.util.List;

import com.uncreated.civilized.client.BuildingRedrawSession;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirements;
import com.uncreated.civilized.networking.packets.CheckUpgradeRequirements;
import com.uncreated.civilized.networking.packets.UpgradeBuilding;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.widgets.BuildingRequirementsView;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shows the requirements for a building's next level, which the server checks, and lets the player upgrade it once
 * they're all met.
 */
public class UpgradeBuildingTab extends ABuildingScreenTab implements IRequirementsCheckListener {

   private static final int VIEW_TOP = 15;
   private static final int BUTTON_WIDTH = 88;
   private static final int BUTTON_HEIGHT = 18;

   private final BuildingRequirementsView requirementsView;
   private final Button upgradeButton;
   /** For when the next level needs more room than the building has, e.g. more floor space. */
   private final Button redrawButton;
   private int requestId;
   /** The level the shown requirements were checked at, so they're only checked again once the building changes. */
   private int checkedLevel;

   public UpgradeBuildingTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(
            tabHost,
            font,
            Component.translatable(
                  "menu.building.upgrade.tab.heading",
                  Building.upgradeLevelTranslationFull(context.building().getUpgradeLevel() + 1)),
            context);

      requirementsView =
            new BuildingRequirementsView(getX(), getY() + VIEW_TOP, width, height - VIEW_TOP - BUTTON_HEIGHT - 4, font);
      upgradeButton =
            Button.builder(Component.translatable("menu.building.upgrade.button"), button -> upgrade())
                  .pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      redrawButton =
            Button.builder(
                  Component.translatable("menu.building.redraw.button"),
                  button -> BuildingRedrawSession.startFromMenu(building()))
                  .pos(upgradeButton.getX() - BUTTON_WIDTH - 4, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .tooltip(Tooltip.create(Component.translatable("menu.building.redraw.button.tooltip")))
                  .build();
      redrawButton.active = AlterBuildingPermissions.hasPermission(context);

      checkRequirements();
   }

   private Building building() {
      return context.building();
   }

   private boolean isAtHighestLevel() {
      return BuildingRequirements.find(building().getBuildingType(), building().getUpgradeLevel() + 1).isEmpty();
   }

   private void checkRequirements() {
      checkedLevel = building().getUpgradeLevel();
      requirementsView.setResults(null);
      upgradeButton.active = false;

      if (isAtHighestLevel())
         return;

      requestId = CheckUpgradeRequirements.nextRequestId();
      PacketDistributor.sendToServer(new CheckUpgradeRequirements(requestId, building().getBuildingId()));
      requirementsView.updateConfirmButton(upgradeButton);
   }

   @Override
   public void receiveRequirementsChecked(int requestId, List<? extends IBuildingRequirementResult> results) {
      if (requestId != this.requestId)
         return;

      requirementsView.setResults(results);
      requirementsView.updateConfirmButton(upgradeButton);
   }

   private void upgrade() {
      PacketDistributor.sendToServer(new UpgradeBuilding(building().getBuildingId()));
      // checked again straight away, since the server handles packets in order. If the upgrade failed, this shows the
      // player why. If it worked, the server closes the menu, so the answer is ignored
      checkRequirements();
   }

   @Override
   public void refresh() {
      // the building changes often, e.g. when its villagers do something, but only an upgrade changes the requirements
      if (building().getUpgradeLevel() != checkedLevel)
         checkRequirements();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      if (isAtHighestLevel()) {
         graphics.drawWordWrap(
               font,
               Component.translatable("menu.building.upgrade.highest_level"),
               getX(),
               getY() + VIEW_TOP,
               width,
               Colors.MENU_TEXT_DARK,
               false);
         return;
      }

      // graphics.drawString(
      // font,
      // Component.translatable(
      // "menu.building.upgrade.levels",
      // Building.upgradeLevelTranslationFull(building().getUpgradeLevel() + 1)),
      // getX(),
      // getBottom() - BUTTON_HEIGHT + 5,
      // Colors.MENU_TEXT_DARK,
      // false);

      requirementsView.render(graphics, mouseX, mouseY, partialTicks);
      upgradeButton.render(graphics, mouseX, mouseY, partialTicks);
      redrawButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      if (isAtHighestLevel())
         return List.of();

      return List.of(requirementsView, upgradeButton, redrawButton);
   }
}
