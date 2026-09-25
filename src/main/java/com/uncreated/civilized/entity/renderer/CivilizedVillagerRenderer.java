package com.uncreated.civilized.entity.renderer;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.data.VillagerHunger;
import com.uncreated.civilized.entity.renderer.layer.ClothingLayer;
import com.uncreated.civilized.entity.renderer.layer.HairLayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.BowItem;

public class CivilizedVillagerRenderer extends
      HumanoidMobRenderer<CivilizedVillager, CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>> {

   public static final boolean DEBUG = true;

   public CivilizedVillagerRenderer(EntityRendererProvider.Context context) {
      super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 1);

      this.addLayer(new ClothingLayer(this, context.getModelSet()));
      this.addLayer(new HairLayer(this, context.getModelSet())); // hair renders over clothing
   }

   @Override
   public CivilizedVillagerRenderState createRenderState() {
      return new CivilizedVillagerRenderState();
   }

   @Override
   public void extractRenderState(CivilizedVillager villager, CivilizedVillagerRenderState state, float partialTick) {
      super.extractRenderState(villager, state, partialTick);

      // floor sleepers are laid out as if they were in a bed facing their sleeping direction, so they fill exactly the
      // two blocks that were checked to be clear for them
      state.sleepingOnFloor = villager.isSleepingOnFloor();
      if (state.sleepingOnFloor)
         state.bedOrientation = villager.getFloorSleepingDirection();

      // vanilla moves a sleeper back from the centre of its head block by (eyeHeight - 0.1). That suits players, but
      // our
      // villager has a lower eye height while using the full size player model, which pokes its head out past the end
      // of its bed. Setting it from the model's length instead fits the model exactly into its two blocks.
      if (state.hasPose(Pose.SLEEPING))
         state.eyeHeight = SLEEPING_MODEL_LENGTH - 0.5F + 0.1F;
      state.villagerName = villager.getInfo().getFullNameComponent();
      state.occupation = villager.getInfo().getOccupation();
      state.skin = villager.getSkin();
      state.hair = villager.getHair();
      state.clothing = villager.getClothing();
      state.health = villager.getHealth();
      state.maxHealth = villager.getMaxHealth();
      int hunger = villager.getHunger().hunger();
      if (!villager.getInfo().getOccupation().is(VillagerOccupations.UNEMPLOYED)) {
         if (DEBUG) {
            state.title =
                  Component.translatable(
                        "villager.renderer.title.extended",
                        villager.getInfo().getOccupation().translation(),
                        healthIcon(state.health, state.maxHealth),
                        Math.ceil(state.health),
                        hungerIcon(hunger),
                        hunger);
         } else {
            Component healthC =
                  state.health < state.maxHealth ? healthIcon(state.health, state.maxHealth) : Component.empty();
            Component hungerC = hunger <= VillagerHunger.HUNGRY_THRESHOLD ? hungerIcon(hunger) : Component.empty();
            state.title =
                  Component.translatable(
                        "villager.renderer.title",
                        villager.getInfo().getOccupation().translation(),
                        healthC,
                        hungerC,
                        hungerIcon(hunger));
         }
      } else
         state.title = null;

      if (DEBUG) {
         state.debugBehavioursList = villager.getEntityData().get(CivilizedVillager.CURRENT_WORK_BEHAVIOUR);
      }
   }

   /**
    * Custom fonts for drawing icons in the villager's name tags {@code assets/civilized/font/icons.json}) Hunger and
    * Health icons are drawn as they are in vanilla, that's why we have so many different icons - each one is for an
    * overlay of some sort
    */
   private static final ResourceLocation ICON_FONT = ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "icons");
   private static final String FOOD_FULL_GLYPH = "\uE000";
   private static final String FOOD_HALF_GLYPH = "\uE001";
   private static final String FOOD_EMPTY_GLYPH = "\uE002";
   private static final String HEART_CONTAINER_GLYPH = "\uE003";
   private static final String HEART_FULL_GLYPH = "\uE004";
   private static final String HEART_HALF_GLYPH = "\uE005";
   /**
    * Steps back over an icon's container, so the icon is drawn on top of it the way vanilla's health and hunger bars
    * are. Every container is 9 pixels wide.
    */
   private static final String BACK_OVER_CONTAINER = "\uE00F";
   /**
    * Pads an icon out to its container's width, so the text after it lines up the same whichever icon was drawn. The
    * font trims empty columns off each sprite, which leaves the icons narrower than their containers.
    */
   private static final String PADDING_1 = "\uE00E";
   private static final String PADDING_4 = "\uE00D";

   /** An icon drawn over its container, then padded so it takes up exactly the container's width. */
   private static String overContainer(String container, String icon, String padding) {
      return container + BACK_OVER_CONTAINER + icon + padding;
   }

   private static Component healthIcon(float health, float maxHealth) {
      String glyphs;
      if (health >= maxHealth)
         glyphs = overContainer(HEART_CONTAINER_GLYPH, HEART_FULL_GLYPH, PADDING_1);
      else if (health > 0)
         glyphs = overContainer(HEART_CONTAINER_GLYPH, HEART_HALF_GLYPH, PADDING_4);
      else
         glyphs = HEART_CONTAINER_GLYPH;

      return appendedIcon(glyphs);
   }

   private static Component hungerIcon(int hunger) {
      // like hearts, vanilla draws the empty food icon as the outline behind the full and half ones
      String glyphs;
      if (hunger > VillagerHunger.HUNGRY_THRESHOLD)
         glyphs = overContainer(FOOD_EMPTY_GLYPH, FOOD_FULL_GLYPH, PADDING_1);
      else if (hunger > 0)
         glyphs = overContainer(FOOD_EMPTY_GLYPH, FOOD_HALF_GLYPH, PADDING_1);
      else
         glyphs = FOOD_EMPTY_GLYPH;

      return appendedIcon(glyphs);
   }

   private static Component icon(String glyphs) {
      return Component.literal(glyphs).withStyle(style -> style.withFont(ICON_FONT).withColor(ChatFormatting.WHITE));
   }

   private static Component appendedIcon(String glyphs) {
      Component icon = icon(glyphs);
      return Component.translatable("villager.renderer.icon", icon);
   }

   private static Component appendIconWithValue(String glyphs, int value) {
      Component icon = icon(glyphs);
      return Component.translatable("villager.renderer.icon_with_value", icon, value);
   }

   /** The player model villagers are drawn with is 32 pixels long, i.e. 2 blocks, the same as a bed. */
   private static final float SLEEPING_MODEL_LENGTH = 2.0F;

   /**
    * How far a villager sleeping without a bed is drawn above the floor, in blocks. This was added because floor
    * sleepers look a little weird being halfway into the floor.
    */
   private static final float FLOOR_SLEEPING_RENDER_OFFSET = 3 / 16F;

   @Override
   protected void setupRotations(
         CivilizedVillagerRenderState renderState,
         PoseStack poseStack,
         float bodyRot,
         float scale) {
      if (renderState.hasPose(Pose.SLEEPING) && renderState.sleepingOnFloor)
         poseStack.translate(0.0F, FLOOR_SLEEPING_RENDER_OFFSET, 0.0F);

      super.setupRotations(renderState, poseStack, bodyRot, scale);
   }

   @Override
   protected HumanoidModel.ArmPose getArmPose(CivilizedVillager villager, HumanoidArm arm) {
      InteractionHand hand = arm == villager.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
      if (villager.isAggressive() && villager.getItemInHand(hand).getItem() instanceof BowItem)
         return HumanoidModel.ArmPose.BOW_AND_ARROW;

      return super.getArmPose(villager, arm);
   }

   @Override
   public ResourceLocation getTextureLocation(CivilizedVillagerRenderState renderState) {
      return renderState.skin;
   }

   @Override
   protected boolean shouldShowName(CivilizedVillager p_115506_, double p_364446_) {
      return false;
   }

   @Override
   public void render(
         CivilizedVillagerRenderState renderState,
         PoseStack pose,
         MultiBufferSource bufferSource,
         int packedLight) {
      super.render(renderState, pose, bufferSource, packedLight);

      renderNameTag(renderState, pose, bufferSource, packedLight);
      renderJobTag(renderState, pose, bufferSource, packedLight);

      if (DEBUG)
         renderDebugInfo(renderState, pose, bufferSource, packedLight);
   }

   private static final int NAME_TAG_COLOUR = 0xFFFFFF;
   private static final int TAG_COLOUR = 0xCCCCCC;

   protected void renderNameTag(
         CivilizedVillagerRenderState renderState,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight) {
      if (renderState.villagerName != null)
         renderTag(
               renderState.villagerName,
               2.1,
               0.021F,
               NAME_TAG_COLOUR,
               renderState,
               poseStack,
               bufferSource,
               packedLight);
   }

   protected void renderJobTag(
         CivilizedVillagerRenderState renderState,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight) {
      if (renderState.title != null)
         renderTag(renderState.title, 1.8, 0.018F, TAG_COLOUR, renderState, poseStack, bufferSource, packedLight);
   }

   protected void renderDebugInfo(
         CivilizedVillagerRenderState renderState,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight) {
      if (renderState.debugBehavioursList != null)
         renderTag(
               Component.literal(renderState.debugBehavioursList),
               2.4,
               0.018F,
               TAG_COLOUR,
               renderState,
               poseStack,
               bufferSource,
               packedLight);
   }

   /**
    * Draws a line of text above the villager's head in the same way vanilla draws name tags. Due to rendering issues
    * when transparent stuff is in the background (e.g. water), the tag text is drawn twice:
    * <ol>
    * <li>First: see-through + with translucent background. This draw is usually what you'll see when there is a normal
    * (solid) background behind the tag. It shows through walls and writes no depth - this is so that translucent things
    * (e.g. water in the background) are always drawn behind it. However, even if transparent things are drawn behind,
    * they will still alter the tag text color, which arises the need for the second draw.</li>
    * <li>Secondary: fully opaque solid text. This pass does write depth such that it is always draws in front no matter
    * the circumstance.</li>
    * </ol>
    *
    * @param height
    *           how far above the villager's feet, in blocks, before vanilla's usual half block
    */
   private void renderTag(
         Component text,
         double height,
         float scale,
         int colour,
         CivilizedVillagerRenderState renderState,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight) {
      boolean seeThrough = !renderState.isDiscrete;

      poseStack.pushPose();
      poseStack.translate(0, height + 0.5, 0);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.scale(scale, -scale, scale);
      Matrix4f matrix = poseStack.last().pose();

      Font font = this.getFont();
      float x = -font.width(text) / 2.0F;
      int backgroundColour = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;

      font.drawInBatch(
            text,
            x,
            0,
            ARGB.color(0x80, colour),
            false,
            matrix,
            bufferSource,
            seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
            backgroundColour,
            packedLight);

      if (seeThrough)
         font.drawInBatch(
               text,
               x,
               0,
               ARGB.opaque(colour),
               false,
               matrix,
               bufferSource,
               Font.DisplayMode.NORMAL,
               0,
               LightTexture.lightCoordsWithEmission(packedLight, 2));

      poseStack.popPose();
   }
}
