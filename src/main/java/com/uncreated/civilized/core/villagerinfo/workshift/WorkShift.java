package com.uncreated.civilized.core.villagerinfo.workshift;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record WorkShift(@Nullable String presetId, int workStart, int workEnd, int restStart, int restEnd) {

   private static final Map<String, WorkShift> PRESETS = new HashMap<>();

   public static final int STANDARD_WORKSHIFT_DURATION_TICKS = 8000;

   public static final int CLOCK_7AM = 1000;
   public static final int CLOCK_2PM = 8000;
   public static final int CLOCK_3PM = 9000;
   public static final int CLOCK_10PM = 16000;
   public static final int CLOCK_6AM_EOD = 24000;

   public static final WorkShift MORNING = preset("morning", CLOCK_7AM, CLOCK_3PM, CLOCK_10PM, CLOCK_6AM_EOD);
   public static final WorkShift AFTERNOON = preset("afternoon", CLOCK_2PM, CLOCK_10PM, CLOCK_10PM, CLOCK_6AM_EOD);
   public static final WorkShift NIGHT = preset("night", CLOCK_10PM, CLOCK_6AM_EOD, CLOCK_7AM, CLOCK_3PM);
   public static final WorkShift UNEMPLOYED = preset("unemployed", -1, -1, CLOCK_10PM, CLOCK_6AM_EOD);

   public static final StreamCodec<ByteBuf, WorkShift> STREAM_CODEC =
         StreamCodec.of(WorkShift::encode, WorkShift::decode);

   private static void encode(ByteBuf buffer, WorkShift shift) {

      if (shift.presetId != null) {
         buffer.writeBoolean(true);
         ByteBufCodecs.STRING_UTF8.encode(buffer, shift.presetId);
         return;
      }

      buffer.writeBoolean(false);
      buffer.writeInt(shift.workStart);
      buffer.writeInt(shift.workEnd);
      buffer.writeInt(shift.restStart);
      buffer.writeInt(shift.restEnd);
   }

   private static WorkShift decode(ByteBuf buffer) {
      boolean isPreset = buffer.readBoolean();
      if (isPreset) {
         String presetId = ByteBufCodecs.STRING_UTF8.decode(buffer);
         WorkShift preset = PRESETS.get(presetId);
         if (preset == null)
            throw new DecoderException("Unknown work shift preset: " + presetId);
         return preset;
      }

      return custom(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt());
   }

   public static final String FIELD_PRESET_ID = "preset_id";
   public static final String FIELD_WORK_START = "work_start";
   public static final String FIELD_WORK_END = "work_end";
   public static final String FIELD_REST_START = "rest_start";
   public static final String FIELD_REST_END = "rest_end";

   public static WorkShift preset(String presetId, int workStart, int workEnd, int restStart, int restEnd) {
      WorkShift preset = new WorkShift(presetId, workStart, workEnd, restStart, restEnd);
      PRESETS.put(presetId, preset);
      return preset;
   }

   public static WorkShift custom(int workStart, int workEnd, int restStart, int restEnd) {
      return new WorkShift(null, workStart, workEnd, restStart, restEnd);
   }

   public static @NotNull WorkShift fromNbt(CompoundTag recordTag) {

      if (recordTag.contains(FIELD_PRESET_ID)) {
         String presetId = recordTag.getString(FIELD_PRESET_ID);
         if (PRESETS.containsKey(presetId))
            return PRESETS.get(presetId);

         LogUtils.getLogger()
               .warn("Work shift preset id '{}' no longer exists. Falling back to UNEMPLOYED work shift.", presetId);
         return UNEMPLOYED;
      }

      return new WorkShift(
            null,
            recordTag.getInt(FIELD_WORK_START),
            recordTag.getInt(FIELD_WORK_END),
            recordTag.getInt(FIELD_REST_START),
            recordTag.getInt(FIELD_REST_END));
   }

   public @NotNull CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();

      if (presetId != null) {
         tag.putString(FIELD_PRESET_ID, presetId);
         return tag;
      }

      tag.putInt(FIELD_WORK_START, workStart);
      tag.putInt(FIELD_WORK_END, workEnd);
      tag.putInt(FIELD_REST_START, restStart);
      tag.putInt(FIELD_REST_END, restEnd);
      return tag;
   }

   public boolean isRestTime(long todayTime) {
      return todayTime >= restStart && todayTime < restEnd;
   }

   public boolean isWorkTime(long todayTime) {
      return todayTime >= workStart && todayTime < workEnd;
   }

   public boolean isIdleTime(long todayTime) {
      return !isWorkTime(todayTime) && !isRestTime(todayTime);
   }

   public int getWorkDurationTicks() {
      return workEnd - workStart;
   }

   public MutableComponent translation() {
      if (workStart < 0 || workEnd < 0)
         return Component.literal(" - ");

      if (presetId != null)
         return Component.translatableWithFallback("workshift.preset." + presetId, hoursLiteral(workStart, workEnd));

      return Component.literal(hoursLiteral(workStart, workEnd));
   }

   private String hoursLiteral(int start, int finish) {
      return toClockTime(start) + " - " + toClockTime(finish);
   }

   public MutableComponent workHoursTranslation() {
      if (workStart < 0 || workEnd < 0)
         return Component.literal(" - ");

      return Component.literal(hoursLiteral(workStart, workEnd));
   }

   public MutableComponent restHoursTranslation() {
      if (restStart < 0 || restEnd < 0)
         return Component.literal(" - ");

      return Component.literal(hoursLiteral(restStart, restEnd));
   }

   public static String toClockTime(int dayTicks) {
      int ticks = Math.floorMod(dayTicks, 24000);
      int hour = (ticks / 1000 + 6) % 24;

      String suffix = hour < 12 ? "am" : "pm";
      int clockHour = hour % 12 == 0 ? 12 : hour % 12;
      return String.format("%d%s", clockHour, suffix);
   }

   public static String toClockTimeWithMinutes(int dayTicks) {
      int ticks = Math.floorMod(dayTicks, 24000);
      int hour = (ticks / 1000 + 6) % 24;
      int minute = ticks % 1000 * 60 / 1000;

      String suffix = hour < 12 ? "am" : "pm";
      int clockHour = hour % 12 == 0 ? 12 : hour % 12;
      return String.format("%d:%02d%s", clockHour, minute, suffix);
   }
}
