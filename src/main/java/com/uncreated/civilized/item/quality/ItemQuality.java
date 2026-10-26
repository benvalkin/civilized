package com.uncreated.civilized.item.quality;

import com.mojang.serialization.Codec;
import com.uncreated.civilized.neoforge.registration.attachments.MyDataComponents;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

@Getter
public enum ItemQuality implements StringRepresentable {
   AWFUL("awful", Colors.QUALITY_AWFUL),
   POOR("poor", Colors.QUALITY_POOR),
   NORMAL("normal", Colors.QUALITY_NORMAL),
   GOOD("good", Colors.QUALITY_GOOD),
   EXCELLENT("excellent", Colors.QUALITY_EXCELLENT),
   MASTERWORK("masterwork", Colors.QUALITY_MASTERWORK),
   LEGENDARY("legendary", Colors.QUALITY_LEGENDARY);

   public static final Codec<ItemQuality> CODEC = StringRepresentable.fromEnum(ItemQuality::values);
   public static final StreamCodec<FriendlyByteBuf, ItemQuality> STREAM_CODEC =
         NeoForgeStreamCodecs.enumCodec(ItemQuality.class);

   private final String serializedName;
   private final int colour;

   ItemQuality(String serializedName, int colour) {
      this.serializedName = serializedName;
      this.colour = colour;
   }

   public static ItemQuality of(ItemStack itemStack) {
      return itemStack.getOrDefault(MyDataComponents.ITEM_QUALITY, NORMAL);
   }

   @Override
   public String getSerializedName() {
      return serializedName;
   }

   public Component tooltipLine() {
      return Component.translatable("item.quality.line", Component.translatable("item.quality." + serializedName))
            .withColor(colour);
   }

   public Component tooltipLineCraftsmanship() {
      return Component.translatable("item.craftsmanship.line", Component.translatable("item.quality." + serializedName))
            .withColor(colour);
   }
}
