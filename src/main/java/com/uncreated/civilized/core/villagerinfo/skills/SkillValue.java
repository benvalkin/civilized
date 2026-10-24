package com.uncreated.civilized.core.villagerinfo.skills;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class SkillValue {
   private final VillagerSkill skill;
   private final int totalXp;

   public Optional<Integer> totalXp() {
      if (totalXp < 0)
         return Optional.empty();

      return Optional.of(totalXp);
   }
}
