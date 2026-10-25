package com.uncreated.civilized.entity.data;

import java.util.Optional;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.settlement.permission.PlayerPermission;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ProjectileWeaponItem;

public class VillagerXpCollector {

   /** How much XP a full working day of productive work earns, whatever the job. */
   public static final int WORK_XP_PER_DAY = 300;
   public static final int GUARD_DUTY_XP_PER_DAY = 60;
   public static final int XP_PER_HIT = 20;
   public static final int XP_PER_KILL = 150;
   public static final int XP_PER_FLIRT = 150;

   private static final float WORK_XP_PER_TICK = WORK_XP_PER_DAY / (float) CivilizedVillager.WORK_DAY_DURATION_TICKS;
   private static final float GUARD_DUTY_XP_PER_TICK =
         GUARD_DUTY_XP_PER_DAY / (float) CivilizedVillager.WORK_DAY_DURATION_TICKS;
   private static final float XP_MULTIPLIER_PER_UPGRADE_LEVEL = 0.5F;

   private static final long SYNC_INTERVAL_TICKS = 30 * 20;
   /** A small amount of guard duty XP is paid out in batches at this interval. */
   private static final int GUARD_DUTY_INTERVAL_TICKS = 5 * 20;

   private final CivilizedVillager villager;
   private final float[] partialXp = new float[VillagerSkill.values().length];
   private boolean hasUnsyncedXp;
   private long lastSyncTime;

   public VillagerXpCollector(CivilizedVillager villager) {
      this.villager = villager;
   }

   public void gainWorkXp(int workTicks, Building workBuilding) {
      VillagerSkill skill = villager.getInfo().getOccupation().relevantSkill();
      if (skill == null || workTicks <= 0)
         return;

      gainXp(skill, workTicks * WORK_XP_PER_TICK * upgradeMultiplier(workBuilding));
   }

   public void gainHitXp() {
      gainXp(combatSkill(), XP_PER_HIT);
   }

   public void gainKillXp() {
      gainXp(combatSkill(), XP_PER_KILL);
   }

   public void gainFlirtXp() {
      gainXp(VillagerSkill.SOCIAL, XP_PER_FLIRT);
   }

   private static float upgradeMultiplier(Building building) {
      return 1 + (building.getUpgradeLevel() - Building.FIRST_UPGRADE_LEVEL) * XP_MULTIPLIER_PER_UPGRADE_LEVEL;
   }

   private VillagerSkill combatSkill() {
      if (villager.getEquipment()
            .getEquippedOrCarried(CarriedEquipmentSlot.PRIMARY_WEAPON)
            .getItem() instanceof ProjectileWeaponItem)
         return VillagerSkill.SHOOTING;

      return VillagerSkill.MELEE;
   }

   private void gainXp(VillagerSkill skill, float xp) {
      partialXp[skill.ordinal()] += xp;
      int wholeXp = (int) partialXp[skill.ordinal()];
      if (wholeXp <= 0)
         return;

      partialXp[skill.ordinal()] -= wholeXp;
      VillagerInfo info = villager.getInfo();
      int levelsGained = info.getSkills().addXp(skill, wholeXp);
      ServerVillagerStore.INSTANCE.setDirty();

      if (levelsGained > 0) {
         sync();
         announceLevelUp(skill);
      } else
         hasUnsyncedXp = true;
   }

   public void serverTick(ServerLevel level, long gameTime) {
      if (gameTime % GUARD_DUTY_INTERVAL_TICKS == 0)
         gainGuardDutyXp();

      if (hasUnsyncedXp && gameTime - lastSyncTime >= SYNC_INTERVAL_TICKS)
         sync();
   }

   private void gainGuardDutyXp() {
      VillagerInfo info = villager.getInfo();
      if (!info.getOccupation().is(VillagerOccupations.SOLDIER))
         return;

      if (!villager.getBrain().isActive(Activity.WORK))
         return;

      Optional<LoadedBuilding> post = LoadedBuildings.checkLoaded(info.getWorkBuildingId());
      if (post.isEmpty())
         return;

      float multiplier = upgradeMultiplier(post.get().getBuilding());
      gainXp(combatSkill(), GUARD_DUTY_INTERVAL_TICKS * GUARD_DUTY_XP_PER_TICK * multiplier);
   }

   private void sync() {
      hasUnsyncedXp = false;
      lastSyncTime = villager.level().getGameTime();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);
   }

   private void announceLevelUp(VillagerSkill skill) {
      if (!(villager.level() instanceof ServerLevel serverLevel))
         return;

      VillagerInfo info = villager.getInfo();
      int level = VillagerSkills.currentLevel(info.getSkills().getTotalXp(skill).orElse(0));

      serverLevel.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            villager.getX(),
            villager.getY() + villager.getBbHeight() + 0.3,
            villager.getZ(),
            10,
            0.3,
            0.3,
            0.3,
            0);

      SettlementPermissions permissions = ServerSettlementPermissionStore.INSTANCE.getOrCreate(info.getSettlementId());
      for (PlayerPermission entry : permissions.entries()) {
         ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.playerId());
         if (player == null)
            continue;

         if (!permissions.hasCreateBuildingsPermission(entry.playerId()))
            continue;

         player.displayClientMessage(
               Component
                     .translatable(
                           "message.notification.villager.skill_level_up",
                           info.getFirstName(),
                           Component.translatable("villager.skill." + skill.name().toLowerCase()),
                           level)
                     .withColor(Colors.SKILL_LEVEL_UP),
               false);
      }
   }
}
