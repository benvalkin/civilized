package com.uncreated.civilized.ui.menu.building;

import java.util.List;
import java.util.UUID;

import com.uncreated.civilized.client.BuildingRedrawSession;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.networking.packets.ResizeBuilding;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConfirmRedrawnBoundsScreen extends ABoundsConfirmationScreen {

   private final UUID buildingId;
   private final BuildingBounds bounds;

   public ConfirmRedrawnBoundsScreen(
         UUID buildingId,
         BuildingType buildingType,
         BuildingBounds bounds,
         List<? extends IBuildingRequirementResult> requirements) {
      super(Component.translatable("menu.building.redraw.heading", buildingType.translationDark()), requirements);
      this.buildingId = buildingId;
      this.bounds = bounds;
   }

   @Override
   protected void onConfirm() {
      PacketDistributor.sendToServer(new ResizeBuilding(buildingId, bounds));
      BuildingRedrawSession.end();
   }

   @Override
   protected Component closedWithoutConfirmingMessage() {
      return Component.translatable("message.building.redraw.help.placed_destination")
            .withColor(Colors.VALIDATION_PARTIAL_SUCCESS);
   }
}
