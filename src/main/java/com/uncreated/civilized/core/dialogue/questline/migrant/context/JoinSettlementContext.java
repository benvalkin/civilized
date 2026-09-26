package com.uncreated.civilized.core.dialogue.questline.migrant.context;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;

@Getter
@Setter
public class JoinSettlementContext extends DialogueContext {

   private @Nullable final BuildingType requiredBuildingType;
   private @Nullable final Settlement settlement;
   private @Nullable Building unoccupiedBuilding;

   public JoinSettlementContext(CivilizedVillager villager, Player player, long gameTime, long dayTime) {
      super(villager, player, gameTime, gameTime);

      this.requiredBuildingType = villager.getInfo().getOccupation().homeType();
      settlement = ClientSettlementsStore.INSTANCE.findFromOwner(player.getUUID()).orElse(null);
      if (settlement == null)
         return;

      if (requiredBuildingType == null) {
         unoccupiedBuilding =
               BuildingUtil
                     .findUnoccupiedHome(
                           settlement.getSettlementId(),
                           ClientBuildingStore.INSTANCE,
                           ClientVillagerStore.INSTANCE,
                           false)
                     .orElse(null);
      } else {
         unoccupiedBuilding =
               BuildingUtil
                     .findUnoccupiedHome(
                           settlement.getSettlementId(),
                           requiredBuildingType,
                           ClientBuildingStore.INSTANCE,
                           ClientVillagerStore.INSTANCE)
                     .orElse(null);
      }
   }
}
