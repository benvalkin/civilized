package com.uncreated.civilized.core.dialogue.actions.server;

import static net.minecraft.network.chat.Component.translatable;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.recruitment.Impression;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** The visitor joins the settlement of the player who asked them, if they're willing to and there's work for them. */
public class JoinSettlementAction implements IServerDialogueAction {

   @Override
   public boolean run(ServerPlayer player, CivilizedVillager villager) {
      if (VisitorBehaviour.of(villager).isEmpty())
         return false;

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.findFromOwner(player.getUUID());
      if (settlement.isEmpty())
         return false;

      UUID settlementId = settlement.get().getSettlementId();
      VillagerInfo info = villager.getInfo();
      if (!Impression.of(info, settlementId, ServerBuildingsStore.INSTANCE, ServerVillagerStore.INSTANCE)
            .isWillingToJoin())
         return false;

      Optional<Building> worksite =
            BuildingUtil.findWorksiteFor(
                  info.getOccupation(),
                  settlementId,
                  ServerBuildingsStore.INSTANCE,
                  ServerVillagerStore.INSTANCE);
      if (worksite.isEmpty())
         return false;

      // the town square or inn they were staying at isn't a home, so the usual home search finds them one
      UUID previousHomeId = info.getHomeBuildingId();
      info.setHomeBuildingId(null);
      info.setSettlementId(settlementId);
      info.setWorkBuildingId(worksite.get().getBuildingId());
      villager.changeNpcRole(VillagerNpcRoles.CITIZEN);
      villager.refreshBrain((ServerLevel) villager.level());

      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);
      BuildingUtil.serverRefreshAndReplicate(previousHomeId, worksite.get().getBuildingId());

      player.sendSystemMessage(
            translatable("villager.dialogue.quest.migrant_worker.misc.complete", info.getFullName())
                  .withColor(Colors.VALIDATION_SUCCESS));
      return true;
   }
}
