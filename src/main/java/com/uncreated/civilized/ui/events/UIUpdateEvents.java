package com.uncreated.civilized.ui.events;

import java.util.UUID;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.events.model.BuildingUpdatedEvent;
import com.uncreated.civilized.core.villagerinfo.events.model.VillagerInfoUpdatedEvent;
import com.uncreated.civilized.ui.menu.building.IBuildingScreen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT, modid = CivilizedMod.CIVILIZED_MOD_ID)
public class UIUpdateEvents {
   @SubscribeEvent
   public static void onVillagerInfoUpdated(VillagerInfoUpdatedEvent event) {
      if (!event.isClientside())
         return;

      if (!(Minecraft.getInstance().screen instanceof IBuildingScreen buildingScreen))
         return;

      UUID viewedBuildingId = buildingScreen.getContext().building().getBuildingId();

      if (!(viewedBuildingId.equals(event.getVillagerInfo().getHomeBuildingId())
            || viewedBuildingId.equals(event.getVillagerInfo().getPrimaryWorksiteId())))
         return;

      buildingScreen.refresh();
   }

   @SubscribeEvent
   public static void onBuildingChanged(BuildingUpdatedEvent event) {
      if (!event.isClientside())
         return;

      if (!(Minecraft.getInstance().screen instanceof IBuildingScreen buildingScreen))
         return;

      UUID viewedBuildingId = buildingScreen.getContext().building().getBuildingId();

      if (!viewedBuildingId.equals(event.getBuilding().getBuildingId()))
         return;

      buildingScreen.refresh();
   }
}
