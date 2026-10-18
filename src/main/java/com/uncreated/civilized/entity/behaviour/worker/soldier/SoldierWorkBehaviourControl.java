package com.uncreated.civilized.entity.behaviour.worker.soldier;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.behaviour.worker.WorkBehaviourControl;

public class SoldierWorkBehaviourControl extends WorkBehaviourControl {
   public SoldierWorkBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ImmutableList<BehaviourState> idleTasks) {
      super(tasks, coreTasks, idleTasks);
   }
}
