package com.uncreated.civilized.entity.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderState;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

// The generic parameters need the proper types you used everywhere else up to this point.
public class HairLayer
      extends RenderLayer<CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>> {
   // private final CivilizedVillagerModel model;

   // Create the render layer. The renderer parameter is required for passing to super.
   // Other parameters can be added as needed. For example, we need the EntityModelSet for model baking.
   public HairLayer(CivilizedVillagerRenderer renderer, EntityModelSet entityModelSet) {
      super(renderer);
      // Bake and store our layer definition, using the ModelLayerLocation from back when we registered the layer
      // definition.
      // If applicable, you can also store multiple models this way and use them below.
      // this.model = new CivilizedVillagerModel(entityModelSet.bakeLayer(MY_LAYER));
      entityModelSet.bakeLayer(ModelLayers.PLAYER);
   }

   @Override
   public void render(
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight,
         CivilizedVillagerRenderState renderState,
         float yRot,
         float xRot) {

      RenderLayer.renderColoredCutoutModel(
            getParentModel(),
            renderState.hair,
            poseStack,
            bufferSource,
            packedLight,
            renderState,
            0xffffff);
   }
}
