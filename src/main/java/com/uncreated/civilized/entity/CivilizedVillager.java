package com.uncreated.civilized.entity;

import static com.uncreated.civilized.entity.behaviour.CivilizedVillagerActivities.*;
import static com.uncreated.civilized.entity.behaviour.worker.CombatActivities.getCombatPackage;
import static com.uncreated.civilized.entity.behaviour.worker.WorkActivities.getWorkPackage;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.controller.DialogueController;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
import com.uncreated.civilized.core.dialogue.specialized.ItemDepotDialogue;
import com.uncreated.civilized.core.settlement.defense.CombatTarget;
import com.uncreated.civilized.core.settlement.defense.ICombatCommand;
import com.uncreated.civilized.core.settlement.defense.SettlementDefenseHighCommand;
import com.uncreated.civilized.core.settlement.defense.TargetRequestResult;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviourControl;
import com.uncreated.civilized.entity.behaviour.worker.soldier.ArrowLineOfFire;
import com.uncreated.civilized.entity.control.CivilizedVillagerLookControl;
import com.uncreated.civilized.entity.data.VillagerHunger;
import com.uncreated.civilized.entity.pathfinding.VillagerGroundPathNavigation;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderer;
import com.uncreated.civilized.entity.stats.ClothingTextureRegistry;
import com.uncreated.civilized.entity.stats.HairTextureRegistry;
import com.uncreated.civilized.entity.stats.SkinTextureRegistry;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
import com.uncreated.civilized.ui.menu.dialogue.VillagerDialogueScreen;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public class CivilizedVillager extends AgeableMob
      implements InventoryCarrier, IEntityWithComplexSpawn, RangedAttackMob {

   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String FIELD_VILLAGER_ID = "villager_id";
   public static final String FIELD_LIFETIME_SEED = "lifetime_seed";
   public static final String FIELD_ROUTED = "routed";
   private static final float DEFAULT_RALLY_HEALTH_FRACTION = 0.8F;

   @Getter
   private UUID villagerId;

   @Getter
   private VillagerInfo info;

   @Getter
   private VillagerHunger hunger;

   @Getter
   private DialogueController dialogueController = DialogueController.noDialogue();

   private final SimpleContainer workInputInventory = new SimpleContainer(8);
   private final SimpleContainer workOutputInventory = new SimpleContainer(8);
   private final SimpleContainer logisticsInventory = new SimpleContainer(8);
   private final SimpleContainer weaponInventory = new SimpleContainer(8);

   private long lifetimeSeed;

   @Getter
   private final RandomSource lifetimeRandom;

   public CivilizedVillager(EntityType<? extends AgeableMob> entityType, Level level) {
      super(entityType, level);
      this.lookControl = new CivilizedVillagerLookControl(this);
      ((VillagerGroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
      this.getNavigation().setCanFloat(true);
      this.getNavigation().setRequiredPathLength(48.0F);
      // this.setCanPickUpLoot(true);
      this.lifetimeRandom = RandomSource.create();
      this.setPersistenceRequired(); // prevent auto-despawning
      this.hunger = new VillagerHunger(this);
   }

   public void initBrandNewVillager() {
      info = ServerVillagerStore.INSTANCE.createNewVillager(this);
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.ADD_OR_OVERWRITE);
      ServerVillagerStore.INSTANCE.setDirty();
      villagerId = info.getVillagerId();
      setLifetimeRandom(getRandom().nextLong());
   }

   public void initVillagerFromSave() {
      info = ServerVillagerStore.INSTANCE.get(villagerId);
   }

   public void serverFinalizeSpawn() {
      refreshBrain((ServerLevel) level());

      if (routedOnLoad) {
         routedOnLoad = false;
         rout();
      }

      dialogueController = DialogueController.selectDialogueController(this);
   }

   private void setLifetimeRandom(long seed) {
      lifetimeSeed = seed;
      lifetimeRandom.setSeed(seed);
   }

   public RandomSource getConsistentLifetimeRandom() {
      return RandomSource.create(lifetimeSeed);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);

      compound.putUUID(FIELD_VILLAGER_ID, villagerId);
      compound.putLong(FIELD_LIFETIME_SEED, lifetimeSeed);
      compound.putBoolean(FIELD_ROUTED, isRouted());
      hunger.save(compound);
      this.writeInventoryToTag(compound, this.registryAccess());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      // when spawning a new villager, readAdditionalSaveData will still run, but the NBT data won't have our fields
      // yet, so we need to check
      if (compound.hasUUID(FIELD_VILLAGER_ID))
         villagerId = compound.getUUID(FIELD_VILLAGER_ID);
      setLifetimeRandom(compound.getLong(FIELD_LIFETIME_SEED));
      // the brain's activities aren't set up yet, so routing is restored once it is (see serverFinalizeSpawn)
      routedOnLoad = compound.getBoolean(FIELD_ROUTED);
      hunger.load(compound);

      this.readInventoryFromTag(compound, this.registryAccess());
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      info.encode(buf);
      buf.writeLong(lifetimeSeed);
   }

   public void readInventoryFromTag(CompoundTag tag, HolderLookup.Provider levelRegistry) {
      if (tag.contains("Inventory", 9)) {
         workOutputInventory.fromTag(tag.getList("Inventory", 10), levelRegistry);
      }
      if (tag.contains("WorkInput", 9)) {
         workInputInventory.fromTag(tag.getList("WorkInput", 10), levelRegistry);
      }
      if (tag.contains("Logistics", 9)) {
         logisticsInventory.fromTag(tag.getList("Logistics", 10), levelRegistry);
      }
      if (tag.contains("Weapons", 9)) {
         weaponInventory.fromTag(tag.getList("Weapons", 10), levelRegistry);
      }

   }

   public void writeInventoryToTag(CompoundTag tag, HolderLookup.Provider levelRegistry) {
      tag.put("Inventory", workOutputInventory.createTag(levelRegistry));
      tag.put("WorkInput", workInputInventory.createTag(levelRegistry));
      tag.put("Logistics", logisticsInventory.createTag(levelRegistry));
      tag.put("Weapons", weaponInventory.createTag(levelRegistry));
   }

   @Getter
   private ResourceLocation skin = SkinTextureRegistry.FALLBACK;
   @Getter
   private ResourceLocation hair = SkinTextureRegistry.FALLBACK;
   @Getter
   private ResourceLocation clothing = ClothingTextureRegistry.FALLBACK;

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      info = ClientVillagerStore.INSTANCE.addFromServer(VillagerInfo.decode(buf));
      lifetimeSeed = buf.readLong();
      dialogueController = DialogueController.selectDialogueController(this);
      ClientVillagerStore.INSTANCE.addFromServer(info);
      villagerId = info.getVillagerId();
      updateSkin();
      updateClothing();
   }

   public void updateSkin() {
      skin = SkinTextureRegistry.getRandomSkin(getConsistentLifetimeRandom(), "default", info.getGender()).getValue();
      hair = HairTextureRegistry.getRandomSkin(getConsistentLifetimeRandom(), "default", info.getGender()).getValue();
   }

   public void updateClothing() {
      clothing =
            ClothingTextureRegistry
                  .getRandomClothingTexture(
                        getConsistentLifetimeRandom(),
                        "default",
                        info.getOccupation(),
                        info.getGender())
                  .getValue();
   }

   @Override
   public @NotNull SimpleContainer getInventory() {
      return workOutputInventory;
   }

   public @NotNull SimpleContainer getWorkInputInventory() {
      return workInputInventory;
   }

   public @NotNull SimpleContainer getWorkOutputInventory() {
      return workOutputInventory;
   }

   public @NotNull SimpleContainer getLogisticsInventory() {
      return logisticsInventory;
   }

   public @NotNull SimpleContainer getInventory(VillagerInventoryType inventoryType) {
      return switch (inventoryType) {
      case WORK_TASK -> workInputInventory;
      case WORK_OUTPUT -> workOutputInventory;
      case LOGISTICS -> logisticsInventory;
      case WEAPONS -> weaponInventory;
      };
   }

   public @NotNull SimpleContainer getWeaponInventory() {
      return weaponInventory;
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
      return null;
   }

   @Override
   public HumanoidArm getMainArm() {
      return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {

      dialogueController = DialogueController.selectDialogueController(this);
      DialogueFlow dialogueFlow = dialogueController.getDialogueFlow(this, player, hand);

      if (dialogueFlow == null)
         return InteractionResult.PASS;

      DialogueContext context = dialogueFlow.buildDialogueContext(this, player, hand);
      IVillageDialogue dialogue = dialogueFlow.getOpeningDialogue(context);

      while (dialogue != null && !dialogue.isAvailableToPlayer(context)) {
         dialogue = dialogue.getFallback();
      }

      if (dialogue == null) {
         return InteractionResult.PASS;
      }
      if (dialogue instanceof ItemDepotDialogue itemDepotDialogue) {

         ItemStack itemInHand = player.getItemInHand(hand);
         if (itemInHand.isEmpty())
            return InteractionResult.PASS;

         if (!itemDepotDialogue.getCanConsumeItemCheck().canConsumeItem(context, itemInHand, hand))
            return InteractionResult.PASS;

         ItemStack consumed = itemDepotDialogue.getConsumeItemAction().consumeItem(context, itemInHand, hand);
         return InteractionResult.CONSUME.heldItemTransformedTo(consumed);
      }

      if (player.level().isClientSide) {
         VillagerDialogueScreen screen = new VillagerDialogueScreen(this, dialogue, context);
         Minecraft.getInstance().setScreen(screen);
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   protected PathNavigation createNavigation(Level level) {
      return new VillagerGroundPathNavigation(this, level);
   }

   @Override
   public Brain<CivilizedVillager> getBrain() {
      return (Brain<CivilizedVillager>) super.getBrain();
   }

   @Override
   protected Brain.Provider<CivilizedVillager> brainProvider() {
      return Brain.provider(
            List.of(
                  AIRegistry.MM_CROP_FIELD_CENTER.get(),
                  AIRegistry.MM_VILLAGER_WORKTIME_OCCUPATION.get(),
                  AIRegistry.MM_DIALOGUE_TARGET.get(),
                  AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(),
                  AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(),
                  MemoryModuleType.JOB_SITE,
                  MemoryModuleType.HOME,
                  MemoryModuleType.PATH,
                  MemoryModuleType.WALK_TARGET,
                  MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                  MemoryModuleType.LOOK_TARGET,
                  MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                  MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                  MemoryModuleType.NEAREST_PLAYERS,
                  MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                  MemoryModuleType.DOORS_TO_CLOSE,
                  MemoryModuleType.HURT_BY,
                  MemoryModuleType.HURT_BY_ENTITY,
                  MemoryModuleType.INTERACTION_TARGET),
            List.of(
                  SensorType.NEAREST_LIVING_ENTITIES,
                  SensorType.NEAREST_PLAYERS,
                  SensorType.NEAREST_ITEMS,
                  SensorType.NEAREST_BED,
                  SensorType.HURT_BY,
                  AIRegistry.CIVILIZED_VILLAGER_SENSOR.get()));
   }

   // TECHDEBT: since this is called in constructor, registerBrainGoals cannot be called here because it depends on
   // villagerInfo. Not sure if this a problem
   @Override
   protected Brain<?> makeBrain(Dynamic<?> dynamic) {
      return this.brainProvider().makeBrain(dynamic);
   }

   private void registerBrainGoals(Brain<CivilizedVillager> brain) {
      brain.addActivity(Activity.CORE, getCorePackage(0.33f));
      if (!info.getOccupation().is(VillagerOccupations.UNEMPLOYED)) {
         brain.addActivityWithConditions(
               Activity.WORK,
               getWorkPackage(info.getOccupation()),
               Set.of(Pair.of(AIRegistry.MM_VILLAGER_WORKTIME_OCCUPATION.get(), MemoryStatus.VALUE_PRESENT)));

         if (info.getOccupation().is(VillagerOccupations.SOLDIER)) {
            brain.addActivity(AIRegistry.A_DRAFTED.get(), getCombatPackage());
         }
      }
      brain.addActivityWithConditions(
            Activity.REST,
            getRestPackage(0.4F),
            Set.of(Pair.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT)));
      brain.addActivityAndRemoveMemoriesWhenStopped(
            AIRegistry.A_SPEAK_TO_PLAYER.get(),
            getSpeakToPlayerPackage(),
            Set.of(Pair.of(AIRegistry.MM_DIALOGUE_TARGET.get(), MemoryStatus.VALUE_PRESENT)),
            Set.of(AIRegistry.MM_DIALOGUE_TARGET.get()));
      brain.addActivity(Activity.PANIC, getPanicPackage(0.7f));
      brain.addActivity(Activity.IDLE, getIdlePackage(0.25f));
      brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
      brain.setDefaultActivity(Activity.IDLE);
      brain.setActiveActivityIfPossible(Activity.IDLE);
   }

   public static final EntityDataAccessor<Byte> FLOOR_SLEEPING_DIRECTION =
         SynchedEntityData.defineId(CivilizedVillager.class, EntityDataSerializers.BYTE);
   private static final byte NOT_SLEEPING_ON_FLOOR = -1;

   public boolean isSleepingOnFloor() {
      return getFloorSleepingDirection() != null;
   }

   public @Nullable Direction getFloorSleepingDirection() {
      byte direction = getEntityData().get(FLOOR_SLEEPING_DIRECTION);
      return direction == NOT_SLEEPING_ON_FLOOR ? null : Direction.from2DDataValue(direction);
   }

   /**
    * @param pos
    *           where the villager's head lies
    * @param headDirection
    *           the direction from the villager's feet to its head, so its body takes up {@code pos} and the block
    *           behind it, just like a bed
    */
   public void sleepOnFloor(BlockPos pos, Direction headDirection) {
      getEntityData().set(FLOOR_SLEEPING_DIRECTION, (byte) headDirection.get2DDataValue());
      startSleeping(pos);
      // startSleeping raises the villager up to where a mattress would be
      setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
   }

   @Override
   public void stopSleeping() {
      super.stopSleeping();
      getEntityData().set(FLOOR_SLEEPING_DIRECTION, NOT_SLEEPING_ON_FLOOR);
   }

   public void refreshBrain(ServerLevel serverLevel) {
      boolean wasRouted = isRouted();

      Brain<CivilizedVillager> brain = this.getBrain();
      brain.stopAll(serverLevel, this);
      this.brain = brain.copyWithoutBehaviors();
      this.registerBrainGoals(this.getBrain());

      // registering the goals resets the brain to its default activity, so routing has to be restored
      if (wasRouted)
         rout();
   }

   @Override
   public void aiStep() {
      this.updateSwingTime();
      super.aiStep();
   }

   @Override
   protected void customServerAiStep(ServerLevel serverLevel) {

      updateActivity(serverLevel.getDayTime(), serverLevel.getGameTime());
      regenerateHealth(serverLevel.getGameTime());
      hunger.serverTickHunger(serverLevel.getGameTime());

      reportNearbyHostiles();
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("civilizedVillagerBrain");
      this.getBrain().tick(serverLevel, this);
      profilerFiller.pop();

      if (CivilizedVillagerRenderer.DEBUG) {
         List<BehaviorControl<? super CivilizedVillager>> runningBehaviours = getBrain().getRunningBehaviors();
         String activityName =
               getBrain().getActiveNonCoreActivity().map(Activity::getName).orElse("none").toUpperCase();
         String behaviourName =
               runningBehaviours.stream()
                     .filter(b -> b instanceof StatefulBehaviourControl)
                     .map(BehaviorControl::debugString)
                     .findFirst()
                     .orElse("none");

         getEntityData().set(CURRENT_WORK_BEHAVIOUR, activityName + ": " + behaviourName);
      }

      super.customServerAiStep(serverLevel);
   }

   public static final EntityDataAccessor<Integer> HUNGER =
         SynchedEntityData.defineId(CivilizedVillager.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Integer> SATURATION =
         SynchedEntityData.defineId(CivilizedVillager.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Long> HUNGRY_START_TIME =
         SynchedEntityData.defineId(CivilizedVillager.class, EntityDataSerializers.LONG);

   public void addWorkExhaustion(float times) {
      float toAdd = 0.4f * times;
      hunger.addExhaustion(toAdd);
   }

   /** Health regenerated every {@link #REGEN_INTERVAL_TICKS}. */
   private static final float REGEN_AMOUNT = 1.0F;
   private static final int REGEN_INTERVAL_TICKS = 5 * 20;
   /** Regeneration only happens once the villager hasn't taken damage for this long. */
   private static final int REGEN_DELAY_AFTER_DAMAGE_TICKS = 10 * 20;

   private long lastDamageTime;

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      boolean hurt = super.hurtServer(level, source, amount);
      if (hurt) {
         lastDamageTime = level.getGameTime();
         if (!isRouted() && shouldRout())
            rout();
      }
      return hurt;
   }

   private void regenerateHealth(long gameTime) {
      if (!isAlive() || getHealth() >= getMaxHealth())
         return;

      if (gameTime - lastDamageTime < REGEN_DELAY_AFTER_DAMAGE_TICKS)
         return;

      if (tickCount % REGEN_INTERVAL_TICKS == 0)
         heal(REGEN_AMOUNT);
   }

   private void reportNearbyHostiles() {
      Optional<LivingEntity> nearestHostile = getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE);
      if (nearestHostile.isEmpty())
         return;

      if (!nearestHostile.get().isAlive())
         return;

      Optional<LoadedSettlement> loadedSettlement = LoadedSettlements.checkLoaded(info.getSettlementId());
      if (loadedSettlement.isEmpty())
         return;

      boolean isVillagerInsideSettlement = loadedSettlement.get().getSettlement().getBounds().contains(position());
      if (!isVillagerInsideSettlement)
         return;

      SettlementDefenseHighCommand defenseHighCommand = loadedSettlement.get().getBehaviour().getDefenseHighCommand();
      defenseHighCommand.reportHostile(this, nearestHostile.get());
   }

   private long lastScheduleUpdate = 0;

   public void updateActivity(long dayTime, long gameTime) {
      if (gameTime < this.lastScheduleUpdate)
         return;

      this.lastScheduleUpdate = gameTime + 20;
      int todayTime = (int) (dayTime % 24000L);

      if (getBrain().isActive(AIRegistry.A_DRAFTED.get()) || getBrain().isActive(AIRegistry.A_SPEAK_TO_PLAYER.get())
            || isRouted())
         return;

      if (todayTime >= 1000 && todayTime < 9000) { // 7am-3pm
         getBrain().setActiveActivityIfPossible(Activity.WORK);
      } else if (todayTime >= 16000) { // 10pm-6am
         getBrain().setActiveActivityIfPossible(Activity.REST);
      } else { // 6-7am, 3-10pm
         getBrain().setActiveActivityIfPossible(Activity.IDLE);
      }
   }

   public void goSpeakToPlayer(Player player) {
      if (isRouted())
         return;

      getBrain().setMemory(AIRegistry.MM_DIALOGUE_TARGET.get(), player);
      getBrain().setActiveActivityIfPossible(AIRegistry.A_SPEAK_TO_PLAYER.get());
   }

   public void stopSpeakingToPlayer() {
      getBrain().eraseMemory(AIRegistry.MM_DIALOGUE_TARGET.get());
      if (!isRouted())
         brain.setActiveActivityIfPossible(Activity.IDLE);
   }

   // The generic type must match the one of the second parameter below.
   public static final EntityDataAccessor<String> CURRENT_WORK_BEHAVIOUR =
         SynchedEntityData.defineId(
               // The class of the entity.
               CivilizedVillager.class,
               // The entity data accessor type.
               EntityDataSerializers.STRING);

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SATURATION, 0);
      builder.define(HUNGER, 20);
      builder.define(HUNGRY_START_TIME, -1L);
      builder.define(CURRENT_WORK_BEHAVIOUR, "");
      builder.define(FLOOR_SLEEPING_DIRECTION, NOT_SLEEPING_ON_FLOOR);
   }

   @Nullable
   private ICombatCommand combatCommand;

   public void draft(ICombatCommand combatCommand) {

      if (this.combatCommand != combatCommand) {
         // event maybe
      }

      this.combatCommand = combatCommand;
      // routed villagers stay in the panic activity until they unrout, which switches them to the drafted activity
      if (!isRouted())
         getBrain().setActiveActivityIfPossible(AIRegistry.A_DRAFTED.get());
   }

   public void undraft() {
      if (combatCommand == null) {
         // event maybe
      }

      combatCommand = null;
      if (!isRouted())
         getBrain().setActiveActivityIfPossible(Activity.IDLE);
   }

   public boolean isDrafted() {
      return combatCommand != null;
   }

   public @Nullable ICombatCommand getCombatCommand() {
      return combatCommand;
   }

   private boolean routedOnLoad;

   public boolean isRouted() {
      return getBrain().isActive(Activity.PANIC);
   }

   public void rout() {
      getBrain().setActiveActivityIfPossible(Activity.PANIC);
   }

   /**
    * Leaves the panic activity. Drafted villagers go back to fighting, while others go idle until their schedule picks
    * their next activity.
    */
   public void unrout() {
      if (isDrafted())
         getBrain().setActiveActivityIfPossible(AIRegistry.A_DRAFTED.get());
      else
         getBrain().setActiveActivityIfPossible(Activity.IDLE);
   }

   public boolean shouldRout() {
      if (!isDrafted()) {
         // undrafted villagers rout as soon as they are attacked
         LivingEntity attacker = getLastHurtByMob();
         return attacker != null && attacker.isAlive();
      }

      // drafted villagers ask their command if they should rout
      return combatCommand.shouldRout(this);
   }

   public boolean shouldUnrout() {
      if (!isDrafted()) {

         // undrafted villagers stay routed until they have regenerated enough
         // health
         if (getHealth() < getMaxHealth() * DEFAULT_RALLY_HEALTH_FRACTION)
            return false;

         // Optional<LivingEntity> attacker = getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY);
         // return attacker.isEmpty() || !attacker.get().isAlive();

         return true;
      }

      // drafted villagers ask their command if they should unrout
      return combatCommand.shouldUnrout(this);
   }

   private int combatTargetPriority = Integer.MAX_VALUE;

   public TargetRequestResult requestTargetFromCommand() {
      if (combatCommand == null) {
         if (getTarget() != null)
            setTarget(null);
         combatTargetPriority = Integer.MAX_VALUE;
         return TargetRequestResult.NO_TARGET;
      }

      Optional<CombatTarget> combatTarget = combatCommand.requestTargetForCombatant(this);
      if (combatTarget.isPresent()) {
         LivingEntity oldTarget = getTarget();
         if (combatTarget.get().priority() < combatTargetPriority || oldTarget == null || !oldTarget.isAlive()) {
            combatTargetPriority = combatTarget.get().priority();
            setTarget(combatTarget.get().entity());
         }

         if (oldTarget != getTarget())
            return TargetRequestResult.ACQUIRED_NEW_TARGET;

         return TargetRequestResult.REACQUIRED_SAME_TARGET;
      }

      if ((getTarget() != null && getTarget().isAlive()) || getTarget() != null)
         setTarget(null);
      combatTargetPriority = Integer.MAX_VALUE;
      return TargetRequestResult.NO_TARGET;
   }

   public ItemStack findMeleeWeapon() {
      return new ItemStack(Items.IRON_SWORD);
      // return weaponInventory.getItems()
      // .stream()
      // .filter(i -> i.has(DataComponents.TOOL))
      // .findFirst()
      // .orElse(ItemStack.EMPTY);
   }

   public ItemStack findBow() {
      return new ItemStack(Items.BOW);
      // return weaponInventory.getItems()
      // .stream()
      // .filter(i -> i.getItem() instanceof BowItem)
      // .findFirst()
      // .orElse(ItemStack.EMPTY);
   }

   public static final float ARROW_VELOCITY = 1.6F;
   private static final float ARROW_INACCURACY = 6.0F;

   @Override
   public ItemStack getProjectile(ItemStack weapon) {
      if (!(weapon.getItem() instanceof ProjectileWeaponItem weaponItem))
         return CommonHooks.getProjectile(this, weapon, ItemStack.EMPTY);

      // need to make this method return a non-empty ItemStack, otherwise fired projectiles throw an exception when the
      // game saves due to attempting save an empty item stack
      return CommonHooks.getProjectile(this, weapon, new ItemStack(Items.ARROW));
   }

   @Override
   public void performRangedAttack(LivingEntity target, float velocity) {
      if (!(level() instanceof ServerLevel serverLevel))
         return;

      ItemStack weapon = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
      ItemStack projectile = getProjectile(weapon);
      AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectile, velocity, weapon);
      if (weapon.getItem() instanceof ProjectileWeaponItem weaponItem)
         arrow = weaponItem.customArrow(arrow, projectile, weapon);

      Vec3 aim = ArrowLineOfFire.aimAt(arrow.position(), target);
      Projectile.spawnProjectileUsingShoot(
            arrow,
            serverLevel,
            projectile,
            aim.x,
            aim.y,
            aim.z,
            ARROW_VELOCITY,
            ARROW_INACCURACY);
      playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));

      addWorkExhaustion(1);
   }

   @Override
   protected AABB getAttackBoundingBox() {
      AABB aabb = super.getAttackBoundingBox();
      return aabb.inflate(0.5F, (double) 0.0F, 0.5F);
   }

   @Override
   public String toString() {
      if (info == null)
         return super.toString();

      return String.format(
            "%s \"%s %s\" %s - %s",
            info.getOccupation(),
            info.getFirstName(),
            info.getLastName(),
            info.getNpcRoles(),
            super.toString());
   }
}
