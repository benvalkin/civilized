package com.uncreated.civilized.ui.menu.building.widgets;

import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.networking.packets.AssignWorkerRequest;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManageWorkerWidget extends ManageOccupantWidget {

   public ManageWorkerWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         Building building,
         VillagerInfo villagerInfo,
         ManagementOption option,
         boolean isBuildingFull) {
      super(x, y, width, height, font, building, villagerInfo, option, isBuildingFull);
   }

   @Override
   protected Component getAssignButtonTranslation() {
      return Component.translatable("menu.building.worksite.workers.button.assign_worker");
   }

   @Override
   protected Component getUnassignButtonTranslation() {
      return Component.translatable("menu.building.worksite.workers.button.unassign_worker");
   }

   @Override
   protected void onPressedAssign(Button button, VillagerInfo villagerInfo, Building building) {
      PacketDistributor.sendToServer(
            new AssignWorkerRequest(villagerInfo.getVillagerId(), Optional.of(building.getBuildingId())));
   }

   @Override
   protected void onPressedUnassign(Button button, VillagerInfo villagerInfo, Building building) {
      PacketDistributor.sendToServer(new AssignWorkerRequest(villagerInfo.getVillagerId(), Optional.empty()));
   }
}
