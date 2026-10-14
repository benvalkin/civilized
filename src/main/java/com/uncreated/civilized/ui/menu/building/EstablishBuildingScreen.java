package com.uncreated.civilized.ui.menu.building;

import java.util.List;

import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.client.renderer.BuildingDetectionTool;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.placement.BuildingPlacement;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.item.BuildingDeedItem;
import com.uncreated.civilized.networking.packets.CreateNewBuilding;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shows a new building's requirements before the player establishes it. */
public class EstablishBuildingScreen extends ABoundsConfirmationScreen {

   private final BuildingType buildingType;
   private final BuildingPlacement placement;

   public EstablishBuildingScreen(
         BuildingType buildingType,
         BuildingPlacement placement,
         List<? extends IBuildingRequirementResult> requirements) {
      super(
            Component.translatable("menu.building.management.create.heading", buildingType.translationDark()),
            requirements);
      this.buildingType = buildingType;
      this.placement = placement;
   }

   @Override
   protected void onConfirm() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null)
         return;

      // the server checks the placement and requirements again and uses up the deed if the building is established
      ItemStack itemInHand = player.getMainHandItem();
      if (itemInHand.getItem() instanceof BuildingDeedItem buildingDeed
            && buildingDeed.getBuildingType() == buildingType)
         PacketDistributor.sendToServer(new CreateNewBuilding(buildingType, placement));

      BuildingBoundsDragTool.resetDragging();
      BuildingDetectionTool.reset();
   }

   @Override
   protected Component closedWithoutConfirmingMessage() {
      String translationKey = switch (placement) {
         case BuildingPlacement.Detected ignore -> "message.building.placement.help.detected";
         case BuildingPlacement.Dragged ignore -> "message.building.placement.help.placed_destination";
      };
      return Component.translatable(translationKey, buildingType.translation())
            .withColor(Colors.VALIDATION_PARTIAL_SUCCESS);
   }
}
