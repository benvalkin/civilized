package com.uncreated.civilized.core.building.requirement.blockcount;

import java.util.List;

import com.uncreated.civilized.ui.style.Colors;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

public class BuildingBlockTypes {

   @Builder
   @Getter
   public static class BuildingBlockType {
      private final Component description;
      private final List<TagKey<Block>> allowedBlockTags;

      public BuildingBlockType(Component description, List<TagKey<Block>> allowedBlockTags) {
         this.description = description;
         this.allowedBlockTags = allowedBlockTags;
      }
   }

   public static final BuildingBlockType WOOD =
         new BuildingBlockType(
               Component.translatable("menu.building.management.requirements.block_types.type.wood")
                     .withColor(Colors.RESOURCE_WOOD),
               List.of(BlockTags.LOGS, BlockTags.PLANKS, BlockTags.WOODEN_SLABS, BlockTags.WOODEN_STAIRS));

   public static final BuildingBlockType STONE =
           new BuildingBlockType(
                   Component.translatable("menu.building.management.requirements.block_types.type.stone")
                           .withColor(Colors.RESOURCE_STONE),
                   List.of(Tags.Blocks.STONES, Tags.Blocks.COBBLESTONES));
}
