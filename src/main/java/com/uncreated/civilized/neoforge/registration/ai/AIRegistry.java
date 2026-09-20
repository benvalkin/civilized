package com.uncreated.civilized.neoforge.registration.ai;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.function.Supplier;

import com.uncreated.civilized.core.building.logistics.hauling.instruction.ConditionalHaulingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;
import com.uncreated.civilized.entity.sensor.CivilizedVillagerEnemySensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AIRegistry {
   // Create a Deferred Register to hold Blocks which will all be registered under the "civilized" namespace

   public static DeferredRegister<Activity> ACTIVITIES =
         DeferredRegister.create(BuiltInRegistries.ACTIVITY, CIVILIZED_MOD_ID);

   public static final Supplier<Activity> A_SPEAK_TO_PLAYER =
         ACTIVITIES.register("speak_to_player", () -> new Activity("speak_to_player"));

   public static final Supplier<Activity> A_DRAFTED = ACTIVITIES.register("drafted", () -> new Activity("drafted"));

   public static DeferredRegister<MemoryModuleType<?>> MEMORY_MODULES =
         DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, CIVILIZED_MOD_ID);

   public static final Supplier<MemoryModuleType<BlockPos>> MM_CROP_FIELD_CENTER =
         MEMORY_MODULES.register("crop_memory_module", () -> new MemoryModuleType<>(Optional.empty()));
   public static final Supplier<MemoryModuleType<BlockPos>> MM_WORK_POS =
         MEMORY_MODULES.register("work_pos_memory_module", () -> new MemoryModuleType<>(Optional.empty()));
   public static final Supplier<MemoryModuleType<VillagerOccupation>> MM_VILLAGER_WORKTIME_OCCUPATION =
         MEMORY_MODULES
               .register("villager_worktime_occupation_memory_module", () -> new MemoryModuleType<>(Optional.empty()));
   // public static final Supplier<MemoryModuleType<Boolean>> MM_HAS_NON_IDLE_WORK_TASK =
   // MEMORY_MODULES
   // .register("has_non_idle_work_task_memory_module", () -> new MemoryModuleType<>(Optional.empty()));
   // public static final Supplier<MemoryModuleType<Boolean>> MM_HAS_WORK_OUTPUT_RESOURCES =
   // MEMORY_MODULES.register("has_work_output_resources", () -> new MemoryModuleType<>(Optional.empty()));
   // public static final Supplier<MemoryModuleType<Boolean>> MM_HAS_WORK_INPUT_RESOURCES =
   // MEMORY_MODULES.register("has_work_input_resources", () -> new MemoryModuleType<>(Optional.empty()));
   // public static final Supplier<MemoryModuleType<Boolean>> MM_BUSY_OFFLOADING_IMPORTS =
   // MEMORY_MODULES.register("busy_offloading_imports", () -> new MemoryModuleType<>(Optional.empty()));
   // public static final Supplier<MemoryModuleType<Boolean>> MM_BUSY_OFFLOADING_EXPORTS =
   // MEMORY_MODULES.register("busy_offloading_exports", () -> new MemoryModuleType<>(Optional.empty()));
   // public static final Supplier<MemoryModuleType<Boolean>> MM_EXPORT_DESIRED =
   // MEMORY_MODULES.register("export_desired", () -> new MemoryModuleType<>(Optional.empty()));
   // public static final Supplier<MemoryModuleType<Boolean>> MM_IMPORT_DESIRED =
   // MEMORY_MODULES.register("import_desired", () -> new MemoryModuleType<>(Optional.empty()));
   public static final Supplier<MemoryModuleType<Player>> MM_DIALOGUE_TARGET =
         MEMORY_MODULES.register("dialogue_target_memory_module", () -> new MemoryModuleType<>(Optional.empty()));
   public static final Supplier<MemoryModuleType<ConditionalHaulingInstruction<? extends ItemStockRequirement>>> MM_TAKE_ITEMS_INSTRUCTION =
         MEMORY_MODULES.register("take_items_instruction", () -> new MemoryModuleType<>(Optional.empty()));
   public static final Supplier<MemoryModuleType<DropOffItemsInstruction>> MM_DROP_OFF_ITEMS_INSTRUCTION =
         MEMORY_MODULES.register("drop_off_items_instruction", () -> new MemoryModuleType<>(Optional.empty()));

   public static DeferredRegister<SensorType<?>> SENSORS =
         DeferredRegister.create(BuiltInRegistries.SENSOR_TYPE, CIVILIZED_MOD_ID);

   public static final Supplier<SensorType<CivilizedVillagerEnemySensor>> CIVILIZED_VILLAGER_SENSOR =
         SENSORS.register("civilized_villager_sensor", () -> new SensorType<>(CivilizedVillagerEnemySensor::new));

   public static DeferredRegister<Schedule> SCHEDULES =
         DeferredRegister.create(BuiltInRegistries.SCHEDULE, CIVILIZED_MOD_ID);

   public static final Supplier<Schedule> SCHED_CIVILIZED_VILLAGER_DEFAULT =
         SCHEDULES.register(
               "civilized_register_schedule_default",
               () -> new ScheduleBuilder(new Schedule()).changeActivityAt(10, Activity.IDLE)
                     .changeActivityAt(1000, Activity.WORK) // 7:00AM
                     .changeActivityAt(9000, Activity.REST) // 3:00PM
                     .build());

}
