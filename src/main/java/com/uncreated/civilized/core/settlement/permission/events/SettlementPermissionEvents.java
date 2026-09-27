package com.uncreated.civilized.core.settlement.permission.events;

import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.permission.ClientSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
      if (!(event.getEntity() instanceof Player player) || !(event.getLevel() instanceof Level level))
         return;

      if (!checkBlockPermission(player, event.getPos(), level, false))
         event.setCanceled(true);
   }

   /**
    * Stops the player from starting to break a block at all. The client breaks blocks without waiting for the server,
    * which only fires {@link BlockEvent.BreakEvent} once the block is broken, so this is where the client can stop it.
    */
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockLeftClicked(PlayerInteractEvent.LeftClickBlock event) {
      if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START)
         return;

      if (!checkBlockPermission(event.getEntity(), event.getPos(), event.getLevel(), true))
         event.setCanceled(true);
   }

   /** Only fires on the server, where it catches anything that got past {@link #onBlockLeftClicked}. */
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockBroken(BlockEvent.BreakEvent event) {
      if (!(event.getLevel() instanceof Level level))
         return;

      if (!checkBlockPermission(event.getPlayer(), event.getPos(), level, true))
         event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onFarmlandTrampled(BlockEvent.FarmlandTrampleEvent event) {
      if (!(event.getLevel() instanceof Level level))
         return;

      Player player = findTrampler(event.getEntity());
      if (player == null)
         return;

      // trampling is usually an accident so we don't message the player
      if (!isBlockChangeAllowed(player, event.getPos(), level))
         event.setCanceled(true);
   }

   private static @Nullable Player findTrampler(Entity entity) {
      if (entity instanceof Player player)
         return player;
      return entity.getControllingPassenger() instanceof Player rider ? rider : null;
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockModifiedWithTool(BlockEvent.BlockToolModificationEvent event) {
      // this check if for tilling dirt, stripping logs, making paths and scraping copper

      Player player = event.getPlayer(); // player can be null if its a dispenser modifiyng the block
      if (player == null || !(event.getLevel() instanceof Level level))
         return;

      // simulated modifications only check what would happen, so the player shouldn't be told about those
      boolean allowed =
            event.isSimulated() ? isBlockChangeAllowed(player, event.getPos(), level)
                  : checkBlockPermission(player, event.getPos(), level, false);
      if (!allowed)
         event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onItemUsed(PlayerInteractEvent.RightClickItem event) {
      // this checks stops you from placing buckets in settlements where you have no permission
      if (!(event.getItemStack().getItem() instanceof BucketItem))
         return;

      Level level = event.getLevel();
      Player player = event.getEntity();

      // an empty bucket aims at fluid sources, while a full one aims at the block it would pour against
      ClipContext.Fluid fluidMode =
            event.getItemStack().is(Items.BUCKET) ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
      Vec3 eye = player.getEyePosition();
      Vec3 reach = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
      BlockHitResult hit = level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, fluidMode, player));
      if (hit.getType() != HitResult.Type.BLOCK)
         return;

      // the fluid goes into the block that was hit if it can hold fluids (e.g. waterlogging), or next to it otherwise
      BlockPos targetPos = hit.getBlockPos();
      BlockPos adjacentPos = targetPos.relative(hit.getDirection());
      if (checkBlockPermission(player, targetPos, level, false)
            && checkBlockPermission(player, adjacentPos, level, false))
         return;

      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.FAIL);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onBlockUsed(PlayerInteractEvent.RightClickBlock event) {
      Level level = event.getLevel();

      // anything with an inventory, e.g. chests, barrels, furnaces and hoppers. Ender chests are not included in this
      // check.
      if (!(level.getBlockEntity(event.getPos()) instanceof BaseContainerBlockEntity))
         return;

      // only stops the container from opening. The held item can still be used, e.g. to place a block against the
      // container, which the block placing check takes care of
      if (!checkContainerPermission(event.getEntity(), event.getPos(), level))
         event.setUseBlock(TriState.FALSE);
   }

   private static boolean checkContainerPermission(Player player, BlockPos pos, Level level) {
      // future work: hoppers/other modded pipes can drain chests if the player can manage to place them. This is fine
      // as long as block placement is successfully prevented.
      Optional<Settlement> settlement = findEnclosingSettlement(pos, level);
      if (settlement.isEmpty())
         return true;

      SettlementPermissions permissions = permissionStore(level).getOrCreate(settlement.get().getSettlementId());
      if (permissions.hasOpenAllChestsPermission(player.getUUID()))
         return true;

      boolean playerOwned = settlement.get().getOwnerId() != null;
      Optional<Building> enclosingBuilding = buildingStore(level).findEnclosingBuilding(pos, level);
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

      notifyDenied(
            player,
            level,
            Component.translatable(
                  "message.settlement.permission.denied.containers",
                  settlement.get().displayNameTranslation()));
      return false;
   }

   private static boolean checkBlockPermission(Player player, BlockPos pos, Level level, boolean breaking) {
      if (breaking && level.getBlockEntity(pos) instanceof BaseContainerBlockEntity) {
         // special handling for chests/container blocks:
         // you shouldn't be allowed to break a chest if you don't have permission to open it.
         if (!checkContainerPermission(player, pos, level))
            return false;
         // this should run before the rest of the method to avoid looking up settments/permissions twice
      }

      Optional<Settlement> settlement = findEnclosingSettlement(pos, level);
      if (settlement.isEmpty() || mayChangeBlock(player, pos, level, settlement.get()))
         return true;

      notifyDenied(
            player,
            level,
            Component.translatable(
                  "message.settlement.permission.denied.blocks",
                  settlement.get().displayNameTranslation()));
      return false;
   }

   private static boolean isBlockChangeAllowed(Player player, BlockPos pos, Level level) {
      Optional<Settlement> settlement = findEnclosingSettlement(pos, level);
      if (settlement.isEmpty())
         return true;

      return mayChangeBlock(player, pos, level, settlement.get());
   }

   private static boolean mayChangeBlock(Player player, BlockPos pos, Level level, Settlement settlement) {
      SettlementPermissions permissions = permissionStore(level).getOrCreate(settlement.getSettlementId());

      if (permissions.hasGeneralBlockPlacingPermission(player.getUUID())) {
         if (permissions.hasEditBuildingPermission(player.getUUID())) {
            // if you also have edit building permission, you can place blocks anywhere, including in buildings
            return true;
         } else {
            if (buildingStore(level).findEnclosingBuilding(pos, level).isEmpty())
               // if not, you can only place blocks outside buildings
               return true;
         }
      } else {
         boolean playerOwned = settlement.getOwnerId() != null;
         // in NPC villages, if you don't have block placing permission you can only place blocks outside of buildings.
         if (!playerOwned && buildingStore(level).findEnclosingBuilding(pos, level).isEmpty())
            return true;
      }

      return false;
   }

   private static Optional<Settlement> findEnclosingSettlement(BlockPos pos, Level level) {
      if (!level.isClientSide)
         return LoadedSettlements.findEnclosing(pos, level).map(LoadedSettlement::getSettlement);

      // TODO: clients don't have access to LoadedBuildings, to they have to brute force check every settlement to see
      // if it overlaps them. This needs to be improved.
      return ClientSettlementsStore.INSTANCE.all()
            .stream()
            .filter(settlement -> settlement.getBounds().contains(pos))
            .filter(
                  settlement -> ClientBuildingStore.INSTANCE.findForSettlement(settlement.getSettlementId())
                        .stream()
                        .anyMatch(building -> building.getDimension() == level.dimension()))
            .findFirst();
   }

   private static SettlementPermissionStore permissionStore(Level level) {
      return level.isClientSide ? ClientSettlementPermissionStore.INSTANCE : ServerSettlementPermissionStore.INSTANCE;
   }

   private static BuildingStore buildingStore(Level level) {
      return level.isClientSide ? ClientBuildingStore.INSTANCE : ServerBuildingsStore.INSTANCE;
   }

   private static void notifyDenied(Player player, Level level, Component message) {
      // Only the server tells the player, otherwise the message will be sent twice in singleplayer
      if (!level.isClientSide)
         player.displayClientMessage(message, true);
   }
}
