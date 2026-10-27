package com.uncreated.civilized.item.quality.events;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.item.quality.ItemQuality;
import com.uncreated.civilized.item.quality.QualityEffects;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class ItemQualityProjectileEvents {

   @SubscribeEvent
   public static void onArrowSpawned(EntityJoinLevelEvent event) {

      if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow))
         return;

      // an arrow loaded from disk has already been fired
      if (event.loadedFromDisk())
         return;

      ItemStack weapon = arrow.getWeaponItem();
      if (weapon == null)
         return;

      ItemQuality quality = ItemQuality.of(weapon);
      if (quality == ItemQuality.NORMAL)
         return;

      arrow.setBaseDamage(arrow.getBaseDamage() * (1 + QualityEffects.getProjectileDamageBonus(quality)));
   }
}
