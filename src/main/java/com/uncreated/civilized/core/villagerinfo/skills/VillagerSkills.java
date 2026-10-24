package com.uncreated.civilized.core.villagerinfo.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class VillagerSkills {

   private final int[] skillXp;

   private VillagerSkills(int[] skillXp) {
      this.skillXp = skillXp;
   }

   public static VillagerSkills decode(FriendlyByteBuf buffer) {
      return new VillagerSkills(buffer.readVarIntArray());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeVarIntArray(skillXp);
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      for (VillagerSkill value : VillagerSkill.values()) {
         tag.putInt(value.name().toLowerCase(), getTotalXpIntl(value));
      }
      return tag;
   }

   public static VillagerSkills fromNbt(CompoundTag tag) {
      int[] skillXp = new int[VillagerSkill.values().length];
      for (VillagerSkill value : VillagerSkill.values()) {
         String skillName = value.name().toLowerCase();
         if (tag.contains(skillName))
            skillXp[value.ordinal()] = tag.getInt(skillName);
         else
            skillXp[value.ordinal()] = 0;
      }
      return new VillagerSkills(skillXp);
   }

   public static VillagerSkills allSkillsZeroXp() {
      return new VillagerSkills(new int[VillagerSkill.values().length]);
   }

   public static final int MAX_LEVEL = 10;
   public static final int MAX_TOTAL_XP = requiredTotalXpForLevel(10);
   public static final int BASE_LEVEL_XP = 1000;
   public static final int ADDITIONAL_XP_PER_LEVEL = 600;

   private int getTotalXpIntl(VillagerSkill skill) {
      return skillXp[skill.ordinal()];
   }

   public Optional<Integer> getTotalXp(VillagerSkill skill) {
      int totalXp = getTotalXpIntl(skill);
      if (totalXp < 0)
         return Optional.empty();

      return Optional.of(totalXp);
   }

   public boolean isCapableOf(VillagerSkill skill) {
      return getTotalXpIntl(skill) >= 0;
   }

   public List<SkillValue> allSkills() {
      List<SkillValue> capableSkills = new ArrayList<>();
      for (VillagerSkill skill : VillagerSkill.values())
         capableSkills.add(new SkillValue(skill, getTotalXpIntl(skill)));

      return capableSkills;
   }

   public List<CapableSkillValue> capableSkills() {
      List<CapableSkillValue> capableSkills = new ArrayList<>();
      for (VillagerSkill skill : VillagerSkill.values()) {
         Optional<Integer> totalXp = getTotalXp(skill);
         if (totalXp.isEmpty())
            continue;

         capableSkills.add(new CapableSkillValue(skill, totalXp.get()));
      }
      return capableSkills;
   }

   public static Integer currentLevel(int totalXp) {
      // the equation in requiredXpToReach solved using the quadratic formula
      int B = BASE_LEVEL_XP;
      int A = ADDITIONAL_XP_PER_LEVEL;

      int level =
            (int) Math
                  .floor(((-2 * B) + A + Math.sqrt((4 * B * B) - (4 * B * A) + (A * A) + (8 * A * totalXp))) / (2 * A));
      return Math.clamp(level, 0, MAX_LEVEL);
   }

   public static Integer currentLevelXp(int totalXp) {
      Integer currentLevel = currentLevel(totalXp);
      return totalXp - requiredTotalXpForLevel(currentLevel);
   }

   public static Integer nextLevelRequiredXp(int totalXp) {
      int currentLevel = currentLevel(totalXp);
      Integer requiredTotalXpForCurrentLevel = requiredTotalXpForLevel(currentLevel);
      Integer requiredTotalXpForNextLevel = requiredTotalXpForLevel(currentLevel + 1);
      return requiredTotalXpForNextLevel - requiredTotalXpForCurrentLevel;
   }

   public static Integer requiredTotalXpForLevel(int level) {

      level = Math.clamp(level, 0, MAX_LEVEL);

      // quadratic number series where each next level's required xp increases by ADDITIONAL_XP_PER_LEVEL, starting at
      // BASE_LEVEL_XP
      int B = BASE_LEVEL_XP;
      int A = ADDITIONAL_XP_PER_LEVEL;
      return B * level + (A / 2) * level * level - (A / 2) * level;
   }
}
