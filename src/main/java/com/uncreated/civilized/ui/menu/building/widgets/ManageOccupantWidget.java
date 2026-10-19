package com.uncreated.civilized.ui.menu.building.widgets;

import java.util.List;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ManageOccupantWidget extends AbstractContainerWidget {

   protected final Font font;
   protected final Building building;
   protected final VillagerInfo villagerInfo;
   private final Button button;
   private ManagementOption option;
   private final boolean isBuildingFull;

   public ManageOccupantWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         Building building,
         VillagerInfo villagerInfo,
         ManagementOption option,
         boolean isBuildingFull) {
      super(x, y, width, height, Component.literal("ManageOccupantWidget"));
      this.font = font;
      this.building = building;
      this.villagerInfo = villagerInfo;
      this.option = option;
      this.isBuildingFull = isBuildingFull;

      button = Button.builder(getButtonText(option), this::onPress).pos(x + width - 50, y + 10).size(50, 12).build();
   }

   private Component getButtonText(ManagementOption option) {
      return switch (option) {
      case ASSIGN -> getAssignButtonTranslation();
      case UNASSIGN -> getUnassignButtonTranslation();
      case NOT_APPLICABLE -> Component.translatable("gui.misc.label.na");
      };
   }

   protected Component getAssignButtonTranslation() {
      return Component.translatable("menu.building.residence.residents.button.assign_resident");
   }

   protected Component getUnassignButtonTranslation() {
      return Component.translatable("menu.building.residence.residents.button.evict_resident");
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
   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.drawString(font, villagerInfo.getFullName(), getX(), getY(), Colors.MENU_TEXT_DARK, false);

      button.active = option == ManagementOption.UNASSIGN || !isBuildingFull;
      button.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(button);
   }

   private void onPress(Button b) {
      if (option == ManagementOption.ASSIGN) {
         onPressedAssign(b, villagerInfo, building);
      } else if (option == ManagementOption.UNASSIGN) {
         onPressedUnassign(b, villagerInfo, building);
      }
   }

   protected void onPressedAssign(Button button, VillagerInfo villagerInfo, Building building) {
      // the job comes from the worksite, not the home
      villagerInfo.setHomeBuildingId(building.getBuildingId());
      ClientVillagerStore.INSTANCE.replicateChange(villagerInfo, StoreOperation.UPDATE);
      ClientBuildingStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
   }

   protected void onPressedUnassign(Button button, VillagerInfo villagerInfo, Building building) {
      villagerInfo.setHomeBuildingId(null);
      ClientVillagerStore.INSTANCE.replicateChange(villagerInfo, StoreOperation.UPDATE);
      ClientBuildingStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
   }

   public enum ManagementOption {
      ASSIGN, UNASSIGN, NOT_APPLICABLE
   }
}
