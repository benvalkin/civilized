package com.uncreated.civilized.entity.behaviour;

import com.uncreated.civilized.entity.behaviour.strike.GoToTownSquare;

public class AttendFestival extends StrollWithinBuildingBounds {

   public AttendFestival(float speedModifier) {
      super(BehaviourStates.CELEBRATING, 10 * 20, 30 * 20, 4, 2, speedModifier, GoToTownSquare::findLoadedTownSquare);
   }
}
