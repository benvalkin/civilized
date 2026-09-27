package com.uncreated.civilized.core.settlement.permission.events;

import java.util.Optional;

import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public class SettlementPermissionEvents {

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
      // also covers blocks that take up several spaces, like beds and doors, which fire EntityMultiPlaceEvent
      if (!(event.getEntity() instanceof Player player) || !(event.getLevel() instanceof ServerLevel level))
         return;

      if (!checkPermission(player, event.getPos(), level))
         event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockBroken(BlockEvent.BreakEvent event) {
      if (!(event.getLevel() instanceof ServerLevel level))
         return;

      if (!checkPermission(event.getPlayer(), event.getPos(), level))
         event.setCanceled(true);
   }

   private static boolean checkPermission(Player player, BlockPos pos, ServerLevel level) {
      Optional<Settlement> settlement = LoadedSettlements.findEnclosing(pos, level).map(LoadedSettlement::getSettlement);
      if (settlement.isEmpty())
         return true;

      SettlementPermissions permissions = ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId());
      if (permissions.hasBlockPlacingPermission(player.getUUID()))
         return true;

      player.displayClientMessage(
              Component.translatable(
                      "message.settlement.permission.denied.blocks",
                      settlement.get().displayNameTranslation()),
              true);
      return false;
   }
}
