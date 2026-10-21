package com.uncreated.civilized;

import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;

public class Test {

   @org.junit.jupiter.api.Test
   public void test() {

      float xpPerDay = 400;

      for (int level = 0; level <= 10; level++) {

         int myTotalXp = VillagerSkills.requiredTotalXpForLevel(level) + 1;

         int myCurrentLevel = VillagerSkills.currentLevel(myTotalXp);
         int myCurrentLevelXp = VillagerSkills.currentLevelXp(myTotalXp);
         int myNextLevelRequiredXp = VillagerSkills.nextLevelRequiredXp(myTotalXp);
         Integer myNextLevelRequiredTotalXp = VillagerSkills.requiredTotalXpForLevel(myCurrentLevel + 1);

         int daysToReachL1 = Math.round(myNextLevelRequiredXp / xpPerDay);
         int daysToReachL2 = Math.round(myNextLevelRequiredXp / (xpPerDay * 2f));
         int daysToReachL3 = Math.round(myNextLevelRequiredXp / (xpPerDay * 3f));

         int totalDaysToReachL1 = Math.round(myTotalXp / xpPerDay);
         int totalDaysToReachL2 = Math.round(myTotalXp / (xpPerDay * 2f));
         int totalDaysToReachL3 = Math.round(myTotalXp / (xpPerDay * 3f));


         System.out.printf(
               "Level: %-2s %s/%-5s %14s    days to reach: %-2s %-2s %-2s %n",
               myCurrentLevel,
               myCurrentLevelXp,
               myNextLevelRequiredXp,
               String.format("(%s/%s)", myTotalXp,
                       myNextLevelRequiredTotalXp),
               daysToReachL1,
               daysToReachL2,
               daysToReachL3);
      }
   }
}
