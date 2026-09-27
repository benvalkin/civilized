package com.uncreated.civilized.core.settlement.permission.events;

import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public class SettlementPermissionEvents {

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
      // also covers blocks that take up several spaces, like beds and doors, which fire EntityMultiPlaceEvent
      if (!(event.getEntity() instanceof Player player) || !(event.getLevel() instanceof ServerLevel level))
         return;

      if (!checkBlockPermission(player, event.getPos(), level, false))
         event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockBroken(BlockEvent.BreakEvent event) {
      if (!(event.getLevel() instanceof ServerLevel level))
         return;

      if (!checkBlockPermission(event.getPlayer(), event.getPos(), level, true))
         event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockUsed(PlayerInteractEvent.RightClickBlock event) {
      if (!(event.getLevel() instanceof ServerLevel level))
         return;

      // anything with an inventory, e.g. chests, barrels, furnaces and hoppers. Ender chests aren't included, since
      // their contents belong to the player opening them
      if (!(level.getBlockEntity(event.getPos()) instanceof BaseContainerBlockEntity))
         return;

      // only stops the container from opening. The held item can still be used, e.g. to place a block against the
      // container, which the block placing check takes care of
      if (!checkContainerPermission(event.getEntity(), event.getPos(), level))
         event.setUseBlock(TriState.FALSE);
   }

   private static boolean checkContainerPermission(Player player, BlockPos pos, ServerLevel level) {
      // future work: hoppers/other modded pipes can drain chests if the player can manage to place them. This is fine
      // as long as block placement is successfully prevented.
      Optional<Settlement> settlement =
            LoadedSettlements.findEnclosing(pos, level).map(LoadedSettlement::getSettlement);
      if (settlement.isEmpty())
         return true;

      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId());
      if (permissions.hasOpenAllChestsPermission(player.getUUID()))
         return true;

      boolean playerOwned = settlement.get().getOwnerId() != null;
      Optional<Building> enclosingBuilding = ServerBuildingsStore.INSTANCE.findEnclosingBuilding(pos, level);
      if (permissions.hasOpenSelectChestsPermission(player.getUUID())) {
         // with OpenSelectChests, you can open non-building chests with a few exceptions to some building types
         if (enclosingBuilding.isEmpty())
            return true;

         if (enclosingBuilding.get().getBuildingType().isFoodVendor())
            return true;

      } else {
         if (enclosingBuilding.isEmpty() && !playerOwned)
            return true; // without OpenSelectChests, you can open non-building chests in NPC villages
      }

      player.displayClientMessage(
            Component.translatable(
                  "message.settlement.permission.denied.containers",
                  settlement.get().displayNameTranslation()),
            true);
      return false;
   }

   private static boolean checkBlockPermission(Player player, BlockPos pos, ServerLevel level, boolean breaking) {
      if (breaking && level.getBlockEntity(pos) instanceof BaseContainerBlockEntity) {
         // special handling for chests/container blocks:
         // you shouldn't be allowed to break a chest if you don't have permission to open it.
         if (!checkContainerPermission(player, pos, level))
            return false;
         // this should run before the rest of the method to avoid looking up settments/permissions twice
      }

      Optional<Settlement> settlement =
            LoadedSettlements.findEnclosing(pos, level).map(LoadedSettlement::getSettlement);
      if (settlement.isEmpty())
         return true;

      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId());

      if (permissions.hasGeneralBlockPlacingPermission(player.getUUID())) {
         if (permissions.hasEditBuildingPermission(player.getUUID())) {
            // if you also have edit building permission, you can place blocks anywhere, including in buildings
            return true;
         } else {
            if (ServerBuildingsStore.INSTANCE.findEnclosingBuilding(pos, level).isEmpty())
               // if not, you can only place blocks outside buildings
               return true;
         }
      } else {
         boolean playerOwned = settlement.get().getOwnerId() != null;
         // in NPC villages, if you don't have block placing permission you can only place blocks outside of buildings.
         if (!playerOwned && ServerBuildingsStore.INSTANCE.findEnclosingBuilding(pos, level).isEmpty())
            return true;
      }

      player.displayClientMessage(
            Component.translatable(
                  "message.settlement.permission.denied.blocks",
                  settlement.get().displayNameTranslation()),
            true);
      return false;
   }
}
