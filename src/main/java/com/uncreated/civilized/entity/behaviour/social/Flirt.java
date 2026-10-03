package com.uncreated.civilized.entity.behaviour.social;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class Flirt extends Socialize {

   private static final double INVITE_RANGE = 30;
   /** On average, how often the suitor shows a heart while flirting. */
   private static final int TICKS_PER_FLIRTING_HEART = 60;
   private static final int HEARTS_WHEN_DATING = 7;
   /** the odds of a relationship forming after this behaviour ends are 1 in this number. */
   public static final int RELATIONSHIP_SUCCESS_ODDS = 4;

   public Flirt(float speedModifier) {
      super(BehaviourStates.FLIRTING, speedModifier, 30 * 20, 120 * 20, 0);
   }

   @Override
   protected @NotNull List<CivilizedVillager> getCandidateConversationMembers(
         ServerLevel level,
         CivilizedVillager villager) {
      List<CivilizedVillager> candidates =
            level.getEntitiesOfClass(
                  CivilizedVillager.class,
                  villager.getBoundingBox().inflate(INVITE_RANGE),
                  // TODO: eventually need to filter out hostile villagers
                  other -> other != villager && other.isAlive()
                        && other.getInfo().getGender() == villager.getInfo().getGender().opposite()
                        && !other.getInfo().isTaken() && !other.getInfo().getNpcRole().is(VillagerNpcRoles.SUITOR)
                        && other.getInfo().getSettlementId() != null /* && villager.hasLineOfSight(other) */)
                  .stream()
                  .sorted(Comparator.comparingDouble(villager::distanceToSqr))
                  .toList();
      return candidates;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (villager.getInfo().isTaken())
         return false;

      return super.checkExtraStartConditions(level, villager);
   }

   @Override
   protected void tickDuringConversation(ServerLevel level, CivilizedVillager villager, long gameTime) {
      if (villager.getRandom().nextInt(TICKS_PER_FLIRTING_HEART) == 0)
         emitHearts(level, villager, 1);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      // check if taken because the partner's behaviour may have already run this part
      if (!villager.getInfo().isTaken() && isMet() && villager.getRandom().nextInt(RELATIONSHIP_SUCCESS_ODDS) == 0) {
         Optional<Conversation> conversation = villager.getBrain().getMemory(AIRegistry.MM_CONVERSATION.get());
         if (conversation.isPresent() && conversation.get().getPastMembers().size() == 2) {
            CivilizedVillager suitor = conversation.get().getPastMembers().getFirst();
            CivilizedVillager settlementVillager = conversation.get().getPastMembers().getLast();
            startDating(suitor, settlementVillager, level);
         }
      }
      super.stop(level, villager, gameTime);
   }

   protected void startDating(CivilizedVillager suitor, CivilizedVillager villager, ServerLevel level) {
      suitor.getInfo().setPartnerId(villager.getInfo().getVillagerId());
      villager.getInfo().setPartnerId(suitor.getInfo().getVillagerId());
      alignSettlementIds(suitor, villager);
      alignSettlementIds(villager, suitor);
      convertToSpouseIfNecessary(suitor);
      convertToSpouseIfNecessary(villager);

      // the suitor stops being a visitor once it's a spouse, but its partner may still be one
      VisitorBehaviour.of(suitor).ifPresent(visitor -> visitor.setDepartAt(VisitorBehaviour.NO_DEPARTURE));
      VisitorBehaviour.of(villager).ifPresent(visitor -> visitor.setDepartAt(VisitorBehaviour.NO_DEPARTURE));

      suitor.refreshBrain(level);
      villager.refreshBrain(level);

      emitHearts(level, suitor, HEARTS_WHEN_DATING);
      emitHearts(level, villager, HEARTS_WHEN_DATING);

      ServerVillagerStore.INSTANCE.replicateChange(suitor.getInfo(), StoreOperation.UPDATE);
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);
      ServerVillagerStore.INSTANCE.setDirty();

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(villager.getInfo().getSettlementId());
      if (settlement.isEmpty())
         return;

      Player owner = level.getPlayerByUUID(settlement.get().getOwnerId());
      if (owner == null)
         return;

      owner.displayClientMessage(
            Component
                  .translatable(
                        "message.notification.villager.started_dating",
                        villager.getInfo().getFirstName(),
                        suitor.getInfo().getFirstName())
                  .withColor(Colors.LOVE),
            false);
   }

   private static void emitHearts(ServerLevel level, CivilizedVillager villager, int count) {
      // spread out around the villager's head, like the hearts of vanilla villagers and animals
      level.sendParticles(
            ParticleTypes.HEART,
            villager.getX(),
            villager.getEyeY() + 0.5,
            villager.getZ(),
            count,
            villager.getBbWidth() * 0.5,
            0.25,
            villager.getBbWidth() * 0.5,
            0.02);
   }

   private static void alignSettlementIds(CivilizedVillager villager, CivilizedVillager other) {
      if (villager.getInfo().getSettlementId() == null)
         villager.getInfo().setSettlementId(other.getInfo().getSettlementId());
   }

   private static void convertToSpouseIfNecessary(CivilizedVillager villager) {
      if (villager.getInfo().getNpcRole().is(VillagerNpcRoles.SUITOR))
         villager.changeNpcRole(VillagerNpcRoles.SPOUSE);
   }

   public ConversationTopic getConversationTopic() {
      return ConversationTopic.FLIRTING;
   }
}
