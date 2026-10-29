package com.uncreated.civilized.core.dialogue.questline.migrant;

import static net.minecraft.network.chat.Component.translatable;

import java.util.Optional;
import java.util.function.Predicate;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.dialogue.DialogueAction;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.ui.style.Colors;

public class JoinSettlementAction implements DialogueAction {

   @Override
   public void runOnClient(DialogueContext context) {
      Optional<Settlement> settlement = findPlayersSettlement(context);
      if (settlement.isEmpty())
         return;

      Optional<Building> worksite = findFreeWorksite(context, settlement.get());
      if (worksite.isEmpty())
         return;

      VillagerInfo info = context.getVillager().getInfo();
      info.setSettlementId(settlement.get().getSettlementId());
      info.setWorkBuildingId(worksite.get().getBuildingId());
      context.getVillager().changeNpcRole(VillagerNpcRoles.CITIZEN);

      ClientVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);
      ClientVillagerStore.INSTANCE.setDirty();

      context.getPlayer()
            .displayClientMessage(
                  translatable("villager.dialogue.quest.migrant_worker.misc.complete", info.getFullName())
                        .withColor(Colors.VALIDATION_SUCCESS),
                  false);
   }

   static Optional<Settlement> findPlayersSettlement(DialogueContext context) {
      return ClientSettlementsStore.INSTANCE.findFromOwner(context.getPlayer().getUUID());
   }

   static Optional<Building> findFreeWorksite(DialogueContext context, Settlement settlement) {
      VillagerOccupation occupation = context.getVillager().getInfo().getOccupation();
      Predicate<BuildingType> suitableWorksite =
            occupation.is(VillagerOccupations.UNEMPLOYED) ? BuildingType::isWorkBuilding : occupation::isValidWorksite;
      return BuildingUtil.findUnoccupiedWorksite(
            settlement.getSettlementId(),
            suitableWorksite,
            ClientBuildingStore.INSTANCE,
            ClientVillagerStore.INSTANCE);
   }
}
