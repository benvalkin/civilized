package com.uncreated.civilized.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.uncreated.civilized.item.quality.ItemQuality;
import com.uncreated.civilized.item.quality.QualityEffects;
import com.uncreated.civilized.neoforge.registration.attachments.MyDataComponents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackQualityMixin {

   @Inject(method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), cancellable = true)
   private void civilized$applyQualityToDurabilityLoss(
         int damage,
         ServerLevel level,
         @Nullable LivingEntity user,
         CallbackInfoReturnable<Integer> cir) {
      int durabilityLoss = cir.getReturnValue();
      if (durabilityLoss <= 0)
         return;

      ItemStack itemStack = (ItemStack) (Object) this;
      ItemQuality quality = itemStack.get(MyDataComponents.ITEM_QUALITY);
      if (quality == null || quality == ItemQuality.NORMAL)
         return;

      double mitigationLossMultiplier = QualityEffects.getDurabilityLossMultiplier(itemStack, quality);

      double mitigateLossChance = 1 - mitigationLossMultiplier;

      int adjustedLoss = durabilityLoss;

      // vanilla unbreaking also rolls a mitigation effect once per durability point loss
      for (int i = 0; i < durabilityLoss; i++) {
         double random = level.getRandom().nextDouble();
         if (mitigateLossChance > 0) {
            if (random < mitigateLossChance)
               adjustedLoss -= 1;
         } else if (random < -mitigateLossChance)
            adjustedLoss += 1;
      }

      cir.setReturnValue(adjustedLoss);
   }
}
