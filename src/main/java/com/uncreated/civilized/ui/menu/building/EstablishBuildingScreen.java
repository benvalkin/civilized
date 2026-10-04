package com.uncreated.civilized.ui.menu.building;

import java.util.List;

import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
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
   private final BuildingBounds bounds;

   public EstablishBuildingScreen(
         BuildingType buildingType,
         BuildingBounds bounds,
         List<? extends IBuildingRequirementResult> requirements) {
      super(
            Component.translatable("menu.building.management.create.heading", buildingType.translationDark()),
            requirements);
      this.buildingType = buildingType;
      this.bounds = bounds;
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
         PacketDistributor.sendToServer(new CreateNewBuilding(buildingType, bounds));

      BuildingBoundsDragTool.resetDragging();
   }

   @Override
   protected Component closedWithoutConfirmingMessage() {
      return Component.translatable("message.building.placement.help.placed_destination")
            .withColor(Colors.VALIDATION_PARTIAL_SUCCESS);
   }
}
