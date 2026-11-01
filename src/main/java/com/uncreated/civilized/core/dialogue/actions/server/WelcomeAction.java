package com.uncreated.civilized.core.dialogue.actions.server;

import java.util.Optional;

import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;

import net.minecraft.server.level.ServerPlayer;

public class WelcomeAction implements IServerDialogueAction {

   @Override
   public boolean run(ServerPlayer player, CivilizedVillager villager) {
      Optional<VisitorBehaviour> visitor = VisitorBehaviour.tryGet(villager);
      if (visitor.isEmpty())
         return false;

      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(villager.getInfo().getVisitingSettlementId());
      if (!permissions.hasCitizenshipPermission(player.getUUID()))
         return false;

      return visitor.get().welcome();
   }
}
