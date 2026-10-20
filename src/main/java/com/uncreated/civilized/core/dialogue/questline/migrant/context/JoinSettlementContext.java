package com.uncreated.civilized.core.dialogue.questline.migrant.context;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;

@Getter
@Setter
public class JoinSettlementContext extends DialogueContext {

   private @Nullable final Settlement settlement;
   private @Nullable Building freeWorksite;

   public JoinSettlementContext(CivilizedVillager villager, Player player, long gameTime, long dayTime) {
      super(villager, player, gameTime, gameTime);

      settlement = ClientSettlementsStore.INSTANCE.findFromOwner(player.getUUID()).orElse(null);
      if (settlement == null)
         return;

      VillagerOccupation occupation = villager.getInfo().getOccupation();
      Predicate<BuildingType> suitableWorksite =
            occupation.is(VillagerOccupations.UNEMPLOYED) ? BuildingType::isWorkBuilding : occupation::isValidWorksite;
      freeWorksite =
            BuildingUtil
                  .findUnoccupiedWorksite(
                        settlement.getSettlementId(),
                        suitableWorksite,
                        ClientBuildingStore.INSTANCE,
                        ClientVillagerStore.INSTANCE)
                  .orElse(null);
   }
}
