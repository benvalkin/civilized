package com.uncreated.civilized.core.building.events;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.events.model.BuildingDeletedEvent;
import com.uncreated.civilized.core.building.events.model.BuildingUpdatedEvent;
import com.uncreated.civilized.core.building.signs.SignHelper;
import com.uncreated.civilized.core.settlement.events.SettlementUpdatedEvent;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.events.model.VillagerInfoUpdatedEvent;
import com.uncreated.civilized.neoforge.registration.attachments.DataAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class BuildingSignEvents {

   @SubscribeEvent
   public static void onBuildingUpdated(BuildingUpdatedEvent event) {
      if (event.isClientside())
         return;

      updateSign(event.getBuilding());
   }

   @SubscribeEvent
   public static void onSettlementUpdated(SettlementUpdatedEvent event) {
      if (event.isClientside())
         return;

      // town hall often cares when a settlement is updated
      ServerBuildingsStore.INSTANCE.findTownHall(event.getSettlement().getSettlementId())
            .ifPresent(BuildingSignEvents::updateSign);
   }

   @SubscribeEvent
   public static void onVillagerInfoUpdated(VillagerInfoUpdatedEvent event) {
      if (event.isClientside())
         return;

      // update buildings relevant to this villager
      VillagerInfo villager = event.getVillagerInfo();
      Stream.of(
            ServerBuildingsStore.INSTANCE.findTownHall(villager.getSettlementId()),
            ServerBuildingsStore.INSTANCE.find(villager.getHomeBuildingId()),
            ServerBuildingsStore.INSTANCE.find(villager.getPrimaryWorksiteId()))
            .flatMap(Optional::stream)
            .distinct()
            .forEach(BuildingSignEvents::updateSign);
   }

   /**
    * Tells clients to redraw the building's sign from what they know about it. Links the building to one of its signs
    * first, if it isn't linked to one yet.
    */
   public static void updateSign(Building building) {
      Optional<LoadedBuilding> loadedBuilding = LoadedBuildings.checkLoaded(building);
      if (loadedBuilding.isEmpty())
         return;

      SignBlockEntity primarySign = building.getPrimarySign(loadedBuilding.get().getLevel());
      if (primarySign == null) {
         Level level = loadedBuilding.get().getLevel();
         // this building has no sign, we need to find a new sign and link it to this building
         // BAD IMPLEMENTATION: I don't like how much querying this method is doing. Might be time to start rethinking
         // how we store linked signs.
         List<SignBlockEntity> signs =
               building.getBounds()
                     .signArea()
                     .getBlockEntitiesInsideBuilding(level, true)
                     .stream()
                     .filter(b -> b instanceof SignBlockEntity)
                     .map(b -> (SignBlockEntity) b)
                     .filter(sign -> signBelongsToBuilding(sign, building, level))
                     .toList();

         if (signs.isEmpty())
            return;

         // link primary sign if it has not been set yet (usually after creating a new building)
         primarySign = signs.stream().min((s1, s2) -> {
            if (SignHelper.signTextHasSpecialTag(s1.getFrontText()))
               return -2;
            if (SignHelper.signTextHasSpecialTag(s2.getFrontText()))
               return 2;
            if (SignHelper.signIsBlank(s1.getFrontText()))
               return -1;
            if (SignHelper.signIsBlank(s2.getFrontText()))
               return 1;
            return 0;
         }).orElseThrow();

         building.setPrimarySignPos(primarySign.getBlockPos());
         primarySign
               .setData(DataAttachments.LINKED_BUILDING, new DataAttachments.LinkedBuilding(building.getBuildingId()));
      }

      SignHelper.serverTriggerBuildingSignUpdate(primarySign);
   }

  private static boolean signBelongsToBuilding(SignBlockEntity sign, Building building, Level level) {
      Optional<Building> owner = ServerBuildingsStore.INSTANCE.findBuildingForSign(sign.getBlockPos(), level);
      return owner.isPresent() && owner.get() == building;
   }

   @SubscribeEvent
   private static void onBuildingDeleted(BuildingDeletedEvent event) {

      if (event.isClientside())
         return;

      Optional<LoadedBuilding> loadedBuilding = LoadedBuildings.checkLoaded(event.getBuilding());
      if (loadedBuilding.isEmpty())
         return;

      SignBlockEntity sign = event.getBuilding().getPrimarySign(loadedBuilding.get().getLevel());
      if (sign == null)
         return;

      sign.setData(DataAttachments.LINKED_BUILDING, new DataAttachments.LinkedBuilding());
      sign.setText(new SignText(), true);
   }
}
