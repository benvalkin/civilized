package com.uncreated.civilized.item.quality.events;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.item.quality.ItemQuality;
import com.uncreated.civilized.item.quality.QualityEffects;
import com.uncreated.civilized.neoforge.registration.attachments.MyDataComponents;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class ItemQualityModifierEvents {

   private static final ResourceLocation ATTACK_DAMAGE_ID = qualityModifierId("attack_damage");
   private static final ResourceLocation ARMOR_ID = qualityModifierId("armor");
   private static final ResourceLocation ARMOR_TOUGHNESS_ID = qualityModifierId("armor_toughness");
   private static final ResourceLocation BLOCK_BREAK_SPEED_ID = qualityModifierId("block_break_speed");

   private static ResourceLocation qualityModifierId(String attribute) {
      return ResourceLocation.fromNamespaceAndPath(CivilizedMod.CIVILIZED_MOD_ID, "quality_" + attribute);
   }

   @SubscribeEvent
   public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
      ItemQuality quality = event.getItemStack().get(MyDataComponents.ITEM_QUALITY);
      if (quality == null || quality == ItemQuality.NORMAL)
         return;

      for (ItemAttributeModifiers.Entry entry : event.getDefaultModifiers().modifiers()) {
         AttributeModifier modifier = entry.modifier();
         if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE || modifier.amount() <= 0)
            continue;

         if (is(entry, Attributes.ATTACK_DAMAGE) && modifier.is(Item.BASE_ATTACK_DAMAGE_ID))
            addBonusMultiplier(event, entry, ATTACK_DAMAGE_ID, QualityEffects.getAttackDamageBonus(quality));
         else if (is(entry, Attributes.ARMOR))
            addBonusMultiplier(event, entry, ARMOR_ID, QualityEffects.getArmorBonus(quality));
         else if (is(entry, Attributes.ARMOR_TOUGHNESS))
            addBonusMultiplier(event, entry, ARMOR_TOUGHNESS_ID, QualityEffects.getArmorBonus(quality));
      }

      if (event.getItemStack().has(DataComponents.TOOL))
         event.addModifier(
               Attributes.BLOCK_BREAK_SPEED,
               new AttributeModifier(
                     BLOCK_BREAK_SPEED_ID,
                     QualityEffects.getBlockBreakSpeedBonus(quality),
                     AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
               EquipmentSlotGroup.MAINHAND); // only works in main hand
   }

   private static boolean is(ItemAttributeModifiers.Entry entry, Holder<Attribute> attribute) {
      return entry.attribute().value() == attribute.value();
   }

   private static void addBonusMultiplier(
         ItemAttributeModifierEvent event,
         ItemAttributeModifiers.Entry entry,
         ResourceLocation id,
         double bonus) {
      double amount = entry.modifier().amount() * bonus;
      event.addModifier(
            entry.attribute(),
            new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE),
            entry.slot());
   }
}
