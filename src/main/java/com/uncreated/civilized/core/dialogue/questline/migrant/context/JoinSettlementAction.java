package com.uncreated.civilized.core.dialogue.questline.migrant.context;

import static net.minecraft.network.chat.Component.translatable;

import java.util.function.Consumer;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.ui.style.Colors;

public class JoinSettlementAction implements Consumer<DialogueContext> {
   @Override
   public void accept(DialogueContext context) {

      JoinSettlementContext joinSettlement = context.as();

      Settlement settlement = joinSettlement.getSettlement();
      VillagerInfo info = joinSettlement.getVillager().getInfo();

      info.setSettlementId(settlement.getSettlementId());
      info.setHomeBuildingId(joinSettlement.getUnoccupiedBuilding().getBuildingId());
      joinSettlement.getVillager().changeNpcRole(VillagerNpcRoles.WORKER);

      ClientVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);
      ClientVillagerStore.INSTANCE.setDirty();

      joinSettlement.getPlayer()
            .displayClientMessage(
                  translatable("villager.dialogue.quest.migrant_worker.misc.complete", info.getFullName())
                        .withColor(Colors.VALIDATION_SUCCESS),
                  false);
   }
}
