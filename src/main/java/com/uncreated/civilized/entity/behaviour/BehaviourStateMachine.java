package com.uncreated.civilized.entity.behaviour;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class BehaviourStateMachine {

   private Deque<BehaviourState> deque;

   @Getter
   @Setter(AccessLevel.PACKAGE)
   private BehaviourState currentState;

   @Getter
   @Setter(AccessLevel.PACKAGE)
   private boolean isIdle;

   public BehaviourStateMachine() {
      deque = new LinkedList<>();
      currentState = BehaviourStates.NONE;
   }

   public void queueAction(@NotNull BehaviourState newState) {
      deque.add(newState);
   }

   public void queueActionOnce(@NotNull BehaviourState newState) {
      if (deque.contains(newState))
         return;

      deque.add(newState);
   }

   public void queueImmediately(@NotNull BehaviourState newState) {
      deque.addFirst(newState);
   }

   Optional<BehaviourState> pollNextAction() {
      if (deque.isEmpty())
         return Optional.empty();

      return Optional.ofNullable(deque.poll());
   }

   boolean hasQueuedActions() {
      return !deque.isEmpty();
   }
}
