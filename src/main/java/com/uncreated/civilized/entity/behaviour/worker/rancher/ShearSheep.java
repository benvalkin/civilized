package com.uncreated.civilized.entity.behaviour.worker.rancher;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TakeToInventoryInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ToolRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shears the sheep at a sheep farm. The villager never goes into the pen itself, since villagers are bad at closing
 * gates behind them and would let the animals out. Instead, it walks along the outside of the pen to whichever sheep
 * with wool is closest, and shears it over the fence.
 */
public class ShearSheep extends WorkTaskBehaviour {

   private static final int WORK_INTERVAL_TICKS = 20;
   /** How close the villager has to be to a sheep to shear it, in blocks. Far enough to reach over a fence. */
   private static final double SHEARING_REACH = 3.5;

   private long lastWorkTime;
   private MediumDistanceTravelTask travelHelper;
   private ItemStack shears;
   private boolean hasShearedSheep;
   private static final ToolRequirement shearsRequirement = new ToolRequirement("shears", i -> i.is(Items.SHEARS));

   public ShearSheep() {
      super(WorkStates.SHEARING_SHEEP, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      if (!getWorksite().getBuilding().getBuildingType().is(BuildingTypes.SHEEP_FARM))
         return false;

      if (getShearableSheep(level).isEmpty())
         return false;

      InventoryStockRequirement.StockResult carrying = shearsRequirement.evaluate(villager);
      if (!carrying.satisfied()) {
         String reservationKey = villager.getInfo().getVillagerId().toString();
         Optional<TakeToInventoryInstruction> instruction =
               TakeToInventoryInstruction
                     .createIfMetFromSourceBuildings(reservationKey, shearsRequirement, homeAndStorehouseIfPresent());
         if (instruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), instruction.get());
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            getStateMachine().queueActionOnce(this.getState());
            // todo: send notification that the villager is missing shears
         }
         return false;
      }

      shears = carrying.stock().getItemStacks().getFirst();
      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      AABB tooCloseBounds = getWorksite().getBuilding().getBounds().getEncapsulatingAABB();
      AABB closeEnoughBounds = tooCloseBounds.inflate(4);

      travelHelper =
            new MediumDistanceTravelTask(
                  villager,
                  getWorksite().getBuilding().getBlockPos(),
                  (v, d, closeEnough) -> closeEnoughBounds.contains(v.position()),
                  Math.max((int) closeEnoughBounds.getXsize() / 2, (int) closeEnoughBounds.getZsize() / 2));

      villager.setItemSlot(EquipmentSlot.MAINHAND, shears);
      hasShearedSheep = false;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      if (travelHelper.isJourneySuccessful())
         pickUpDroppedItemsAtWorksite(level, villager);

      if (hasShearedSheep)
         goDropOffWorkOutputAtHome(villager);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getBrain().checkMemory(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(gameTime);
         return;
      }

      if (gameTime - lastWorkTime <= WORK_INTERVAL_TICKS)
         return;

      lastWorkTime = gameTime;

      List<Sheep> shearableSheep = getShearableSheep(level);
      if (shearableSheep.isEmpty()) {
         doStop(level, villager, gameTime);
         return;
      }

      // sheep in the middle of a big pen can't be reached over the fence, so wait for them to wander closer. If they
      // never do, the behaviour eventually times out
      AABB pen = getWorksite().getBuilding().getBounds().getEncapsulatingAABB();
      Optional<Sheep> sheep =
            shearableSheep.stream()
                  .filter(s -> distanceToPenEdge(pen, s.position()) <= SHEARING_REACH - 1)
                  .min(Comparator.comparingDouble(s -> s.distanceToSqr(villager)));
      if (sheep.isEmpty())
         return;

      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(sheep.get(), true));

      // sheep wander about, so walk over and shear it on a later work tick once it's within reach
      if (!villager.closerThan(sheep.get(), SHEARING_REACH)) {
         BlockPos spotOutsidePen = BlockPos.containing(closestSpotOutsidePen(pen, sheep.get().position()));
         villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spotOutsidePen, 0.4f, 0));
         return;
      }

      shear(level, villager, sheep.get());
   }

   /** How far a position inside the pen is from the pen's nearest side. */
   private static double distanceToPenEdge(AABB pen, Vec3 pos) {
      return Math.min(Math.min(pos.x - pen.minX, pen.maxX - pos.x), Math.min(pos.z - pen.minZ, pen.maxZ - pos.z));
   }

   /** The spot just outside the pen's nearest side to a position inside it. */
   private static Vec3 closestSpotOutsidePen(AABB pen, Vec3 pos) {
      double toWest = pos.x - pen.minX;
      double toEast = pen.maxX - pos.x;
      double toNorth = pos.z - pen.minZ;
      double toSouth = pen.maxZ - pos.z;
      double closest = Math.min(Math.min(toWest, toEast), Math.min(toNorth, toSouth));

      if (closest == toWest)
         return new Vec3(pen.minX - 0.5, pos.y, pos.z);
      if (closest == toEast)
         return new Vec3(pen.maxX + 0.5, pos.y, pos.z);
      if (closest == toNorth)
         return new Vec3(pos.x, pos.y, pen.minZ - 0.5);
      return new Vec3(pos.x, pos.y, pen.maxZ + 0.5);
   }

   /**
    * Shears the sheep like a player would, except the wool goes straight into the villager's inventory instead of being
    * scattered on the ground.
    */
   private void shear(ServerLevel level, CivilizedVillager villager, Sheep sheep) {
      villager.swing(InteractionHand.MAIN_HAND, true);

      LootParams lootParams =
            new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, sheep.position())
                  .withParameter(LootContextParams.THIS_ENTITY, sheep)
                  .withParameter(LootContextParams.TOOL, shears)
                  .create(LootContextParamSets.SHEARING);
      LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SHEAR_SHEEP);
      lootTable.getRandomItems(lootParams).forEach(i -> villager.getWorkOutputInventory().addItem(i));

      level.playSound(null, sheep, SoundEvents.SHEEP_SHEAR, villager.getSoundSource(), 1.0F, 1.0F);
      level.gameEvent(villager, GameEvent.SHEAR, sheep.position());
      sheep.setSheared(true);

      hasShearedSheep = true;
   }

   private List<Sheep> getShearableSheep(ServerLevel level) {
      return level.getEntitiesOfClass(
            Sheep.class,
            getWorksite().getBuilding().getBounds().getEncapsulatingAABB(),
            Sheep::readyForShearing);
   }
}
