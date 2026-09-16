package com.uncreated.civilized.core.settlement.defense;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record CombatTarget(LivingEntity entity, int priority) {
}
