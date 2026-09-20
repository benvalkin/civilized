package com.uncreated.civilized.entity.behaviour.worker;

import com.uncreated.civilized.entity.behaviour.BehaviourState;

public class WorkStates {

   public static final BehaviourState CRAFTING_ITEMS = new BehaviourState("crafting_items");
   public static final BehaviourState SMELTING_ITEMS = new BehaviourState("smelting_items");
   public static final BehaviourState BLASTING_ITEMS = new BehaviourState("blasting_items");
   public static final BehaviourState SMOKING_ITEMS = new BehaviourState("smoking_items");
   public static final BehaviourState REPLANT_SAPLINGS = new BehaviourState("replant_saplings");
   public static final BehaviourState CHECK_LOGISTICS_OPPORTUNITIES =
         new BehaviourState("check_logistics_opportunities");
   public static final BehaviourState FISHING = new BehaviourState("fishing");
   public static BehaviourState TAKING_ITEMS_TO_INVENTORY = new BehaviourState("taking_items_to_inventory");
   public static BehaviourState DROPPING_OFF_ITEMS_AT_BUILDING = new BehaviourState("dropping_off_items_at_building");
   public static final BehaviourState BREEDING_ANIMALS = new BehaviourState("breeding_animals");
   public static final BehaviourState SLAUGHTERING_ANIMALS = new BehaviourState("slaughtering_animals");
   public static final BehaviourState SHEARING_SHEEP = new BehaviourState("shearing_sheep");
   public static final BehaviourState HARVESTING_HONEY = new BehaviourState("harvesting_honey");
   public static final BehaviourState CUTTING_DOWN_TREES = new BehaviourState("cutting_down_trees");
   public static final BehaviourState MINING_ORES = new BehaviourState("mining_ores");
   public static BehaviourState HARVESTING_CROPS = new BehaviourState("harvesting_crops");
   public static BehaviourState PLANTING_CROPS = new BehaviourState("planting_crops");
   public static BehaviourState MONITOR_WORKSITE = new BehaviourState("monitor_worksite");
}
