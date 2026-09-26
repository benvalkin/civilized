//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.uncreated.civilized.entity.behaviour;

import static com.uncreated.civilized.core.building.util.BuildingUtil.isFenceGateOpen;
import static com.uncreated.civilized.core.building.util.BuildingUtil.setOpenFenceGate;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class InteractWithDoorAndGate {
   private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
   private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = (double) 3.0F;
   private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = (double) 2.0F;

   public static BehaviorControl<LivingEntity> create() {
      MutableObject<Node> mutableobject = new MutableObject((Object) null);
      MutableInt mutableint = new MutableInt(0);
      return BehaviorBuilder.create(
            (instance) -> instance
                  .group(
                        instance.present(MemoryModuleType.PATH),
                        instance.registered(MemoryModuleType.DOORS_TO_CLOSE),
                        instance.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES))
                  .apply(instance, (pathMemory, doorsToClode, doorPositions) -> (level, entity, gameTicks) -> {
                     Path path = (Path) instance.get(pathMemory);
                     Optional<Set<GlobalPos>> rememberToCloseDoor = instance.tryGet(doorsToClode);
                     if (!path.notStarted() && !path.isDone()) {
                        if (Objects.equals(mutableobject.getValue(), path.getNextNode())) {
                           mutableint.setValue(20);
                        } else if (mutableint.decrementAndGet() > 0) {
                           return false;
                        }

                        mutableobject.setValue(path.getNextNode());
                        Node node = path.getPreviousNode();
                        Node node1 = path.getNextNode();
                        BlockPos blockpos = node.asBlockPos();
                        BlockState blockstate = level.getBlockState(blockpos);
                        if (blockstate.is(
                              BlockTags.MOB_INTERACTABLE_DOORS,
                              (p_201959_) -> p_201959_.getBlock() instanceof DoorBlock)) {
                           DoorBlock doorblock = (DoorBlock) blockstate.getBlock();
                           if (!doorblock.isOpen(blockstate)) {
                              doorblock.setOpen(entity, level, blockstate, blockpos, true);
                           }

                           rememberToCloseDoor =
                                 rememberDoorToClose(doorsToClode, rememberToCloseDoor, level, blockpos);
                        } else if (blockstate.is(
                              BlockTags.FENCE_GATES,
                              (p_201959_) -> p_201959_.getBlock() instanceof FenceGateBlock)) {
                           if (!isFenceGateOpen(blockstate)) {
                              setOpenFenceGate(level, entity, blockpos, blockstate, true);
                           }

                           rememberToCloseDoor =
                                 rememberDoorToClose(doorsToClode, rememberToCloseDoor, level, blockpos);
                        }

                        BlockPos blockpos1 = node1.asBlockPos();
                        BlockState blockstate1 = level.getBlockState(blockpos1);
                        if (blockstate1.is(
                              BlockTags.MOB_INTERACTABLE_DOORS,
                              (p_201957_) -> p_201957_.getBlock() instanceof DoorBlock)) {
                           DoorBlock doorBlock1 = (DoorBlock) blockstate1.getBlock();
                           if (!doorBlock1.isOpen(blockstate1)) {
                              doorBlock1.setOpen(entity, level, blockstate1, blockpos1, true);
                              rememberToCloseDoor =
                                    rememberDoorToClose(doorsToClode, rememberToCloseDoor, level, blockpos1);
                           }
                        } else if (blockstate1.is(
                              BlockTags.FENCE_GATES,
                              (p_201957_) -> p_201957_.getBlock() instanceof FenceGateBlock)) {
                           if (!isFenceGateOpen(blockstate1)) {
                              setOpenFenceGate(level, entity, blockpos1, blockstate1, true);
                              rememberToCloseDoor =
                                    rememberDoorToClose(doorsToClode, rememberToCloseDoor, level, blockpos1);
                           }
                        }

                        rememberToCloseDoor.ifPresent(
                              globalPos -> closeDoorsThatIHaveOpenedOrPassedThrough(
                                    level,
                                    entity,
                                    node,
                                    node1,
                                    globalPos,
                                    instance.tryGet(doorPositions)));
                        return true;
                     } else {
                        return false;
                     }
                  }));
   }

   public static void closeDoorsThatIHaveOpenedOrPassedThrough(
         ServerLevel level,
         LivingEntity entity,
         @Nullable Node previous,
         @Nullable Node next,
         Set<GlobalPos> doorPositions,
         Optional<List<LivingEntity>> nearestLivingEntities) {
      Iterator<GlobalPos> iterator = doorPositions.iterator();

      while (iterator.hasNext()) {
         GlobalPos globalpos = (GlobalPos) iterator.next();
         BlockPos blockpos = globalpos.pos();
         if ((previous == null || !previous.asBlockPos().equals(blockpos))
               && (next == null || !next.asBlockPos().equals(blockpos))) {
            if (isDoorTooFarAway(level, entity, globalpos)) {
               iterator.remove();
            } else {
               BlockState blockstate = level.getBlockState(blockpos);
               if (blockstate.is(BlockTags.MOB_INTERACTABLE_DOORS, (base) -> base.getBlock() instanceof DoorBlock)) {
                  DoorBlock doorblock = (DoorBlock) blockstate.getBlock();
                  if (!doorblock.isOpen(blockstate)) {
                     iterator.remove();
                  } else if (areOtherMobsComingThroughDoor(entity, blockpos, nearestLivingEntities)) {
                     iterator.remove();
                  } else {
                     doorblock.setOpen(entity, level, blockstate, blockpos, false);
                     iterator.remove();
                  }
               } else if (blockstate.is(BlockTags.FENCE_GATES, (base) -> base.getBlock() instanceof FenceGateBlock)) {
                  if (!isFenceGateOpen(blockstate)) {
                     iterator.remove();
                  } else if (areOtherMobsComingThroughDoor(entity, blockpos, nearestLivingEntities)) {
                     iterator.remove();
                  } else {
                     setOpenFenceGate(level, entity, blockpos, blockstate, false);
                     iterator.remove();
                  }
               } else {
                  iterator.remove();
               }
            }
         }
      }

   }

   private static boolean areOtherMobsComingThroughDoor(
         LivingEntity entity,
         BlockPos pos,
         Optional<List<LivingEntity>> nearestLivingEntities) {
      if (nearestLivingEntities.isEmpty())
         return false;

      List<LivingEntity> livingEntities = nearestLivingEntities.get();

      return livingEntities.stream()
            .filter((o) -> o.getType() == entity.getType())
            .filter((o) -> pos.closerToCenterThan(o.position(), (double) 2.0F))
            .anyMatch((p_258454_) -> isMobComingThroughDoor(p_258454_.getBrain(), pos));
   }

   private static boolean isMobComingThroughDoor(Brain<?> brain, BlockPos pos) {
      if (!brain.hasMemoryValue(MemoryModuleType.PATH)) {
         return false;
      } else {
         Path path = (Path) brain.getMemory(MemoryModuleType.PATH).get();
         if (path.isDone()) {
            return false;
         } else {
            Node node = path.getPreviousNode();
            if (node == null) {
               return false;
            } else {
               Node node1 = path.getNextNode();
               return pos.equals(node.asBlockPos()) || pos.equals(node1.asBlockPos());
            }
         }
      }
   }

   private static boolean isDoorTooFarAway(ServerLevel level, LivingEntity entity, GlobalPos pos) {
      return pos.dimension() != level.dimension() || !pos.pos().closerToCenterThan(entity.position(), (double) 3.0F);
   }

   /**
    * Mojang wrote this this atrocious method.
    */
   private static Optional<Set<GlobalPos>> rememberDoorToClose(
         MemoryAccessor<OptionalBox.Mu, Set<GlobalPos>> doorsToClose,
         Optional<Set<GlobalPos>> doorPositions,
         ServerLevel level,
         BlockPos pos) {
      GlobalPos globalpos = GlobalPos.of(level.dimension(), pos);
      return Optional.of((Set) doorPositions.map((p_261437_) -> {
         p_261437_.add(globalpos);
         return p_261437_;
      }).orElseGet(() -> {
         Set<GlobalPos> set = Sets.newHashSet(new GlobalPos[] { globalpos });
         doorsToClose.set(set);
         return set;
      }));
   }
}
