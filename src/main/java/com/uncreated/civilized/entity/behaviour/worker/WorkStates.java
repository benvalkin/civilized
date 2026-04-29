package com.uncreated.civilized.entity.behaviour.worker;

import com.uncreated.civilized.entity.behaviour.BehaviourState;

public class WorkStates {

   public static final BehaviourState DROPPING_OFF_WORK_OUTPUT_AT_HOME =
         new BehaviourState("dropping_off_work_output_at_home");
   public static final BehaviourState DROPPING_OFF_EXPORTS_AT_STOREHOUSE =
         new BehaviourState("dropping_off_exports_at_storehouse");
   public static final BehaviourState CRAFTING_ITEMS = new BehaviourState("crafting_items");
   public static final BehaviourState SMELTING_ITEMS = new BehaviourState("smelting_items");
   public static final BehaviourState BLASTING_ITEMS = new BehaviourState("blasting_items");
   public static final BehaviourState SMOKING_ITEMS = new BehaviourState("smoking_items");
   public static final BehaviourState REPLANT_SAPLINGS = new BehaviourState("replant_saplings");
   public static final BehaviourState CHECK_LOGISTICS_OPPORTUNITIES =
         new BehaviourState("check_logistics_opportunities");
   public static final BehaviourState FISHING = new BehaviourState("fishing");
   public static BehaviourState DROPPING_OFF_IMPORTS_AT_HOME = new BehaviourState("dropping_off_imports_at_home");
   public static final BehaviourState FETCHING_EXPORTS_FROM_HOME = new BehaviourState("fetching_exports_from_home");
   public static final BehaviourState FETCHING_IMPORTS_FROM_STOREHOUSE =
         new BehaviourState("fetching_imports_from_storehouse");
   public static BehaviourState FETCHING_WORK_INPUT_FROM_HOME = new BehaviourState("fetching_work_input_from_home");
   public static final BehaviourState BREEDING_ANIMALS = new BehaviourState("breeding_animals");
   public static final BehaviourState SLAUGHTERING_ANIMALS = new BehaviourState("slaughtering_animals");
   public static final BehaviourState CUTTING_DOWN_TREES = new BehaviourState("cutting_down_trees");
   public static final BehaviourState MINING_ORES = new BehaviourState("mining_ores");
   public static BehaviourState HARVESTING_CROPS = new BehaviourState("harvesting_crops");
   public static BehaviourState PLANTING_CROPS = new BehaviourState("planting_crops");
   public static BehaviourState STROLL_AROUND_WORKSITE = new BehaviourState("stroll_around_worksite");
   public static BehaviourState STROLL_OUTSIDE_WORKSITE = new BehaviourState("stroll_outside_worksite");
}
