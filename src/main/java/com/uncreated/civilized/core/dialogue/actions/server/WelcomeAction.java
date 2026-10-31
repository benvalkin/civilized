package com.uncreated.civilized.core.dialogue.actions.server;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;

import net.minecraft.server.level.ServerPlayer;

public class WelcomeAction implements IServerDialogueAction {

   @Override
   public boolean run(ServerPlayer player, CivilizedVillager villager) {
      if (VisitorBehaviour.tryGet(villager).isEmpty())
         return false;

      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(villager.getInfo().getVisitingSettlementId());
      if (!permissions.hasCitizenshipPermission(player.getUUID()))
         return false;

      if (!villager.getInfo().getHospitality().welcome(VisitorHospitality.WELCOME_BONUS))
         return false;

      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);
      return true;
   }
}
