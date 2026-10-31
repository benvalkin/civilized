package com.uncreated.civilized.core.dialogue.actions.server;

import static net.minecraft.network.chat.Component.translatable;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.recruitment.Impression;
import com.uncreated.civilized.core.recruitment.WillingToJoinNotification;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class JoinSettlementAction implements IServerDialogueAction {

   @Override
   public boolean run(ServerPlayer player, CivilizedVillager villager) {
      if (VisitorBehaviour.tryGet(villager).isEmpty())
         return false;

      VillagerInfo info = villager.getInfo();
      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(info.getVisitingSettlementId());
      if (settlement.isEmpty())
         return false;

      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId());
      if (!permissions.hasRecruitNewVillagersPermission(player.getUUID()))
         return false;

      UUID settlementId = settlement.get().getSettlementId();
      if (!Impression.compute(info, settlementId, ServerBuildingsStore.INSTANCE, ServerVillagerStore.INSTANCE)
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

      WillingToJoinNotification.withdrawNotification(info);

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
