package com.uncreated.civilized.core.recruitment;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

@Getter
public class VisitorHospitality {

   public static final float DAILY_CAP = 15;
   public static final int WELCOME_BONUS = 3;
   public static final int FED_BONUS = 5;
   public static final int FESTIVAL_BONUS = 10;

   private static final String FIELD_POINTS = "points";
   private static final String FIELD_GAINED_TODAY = "gained_today";
   private static final String FIELD_DAY = "day";
   private static final String FIELD_WELCOMED = "welcomed";
   private static final String FIELD_FED = "fed";

   private float points;
   private float gainedToday;
   private long day;
   private boolean welcomed;
   private boolean fed;

   public int getWholePoints() {
      return (int) points;
   }

   /**
    * Adds accumulated hospitality points to this visitor. This overload respects and counts toward the daily
    * hospitality limit.
    */
   public void award(float amount, long today) {
      if (today != day) {
         day = today;
         gainedToday = 0;
      }

      float gained = Math.min(amount, DAILY_CAP - gainedToday);
      if (gained <= 0)
         return;

      points += gained;
      gainedToday += gained;
   }

   /** Overload for {@link #award} that disregards and does not count towards the daily hospitality limit. */
   public void awardUncapped(float amount) {
      points += amount;
   }

   /** @return false if they had already been welcomed this visit */
   public boolean welcome(float amount) {
      if (welcomed)
         return false;

      welcomed = true;
      awardUncapped(amount);
      return true;
   }

   /** @return false if they had already been fed this visit */
   public boolean feed(float amount) {
      if (fed)
         return false; // todo: I think we should rather use a quest flag to keep track of fed

      fed = true;
      awardUncapped(amount);
      return true;
   }

   public static VisitorHospitality decode(FriendlyByteBuf buffer) {
      VisitorHospitality hospitality = new VisitorHospitality();
      hospitality.points = buffer.readFloat();
      hospitality.gainedToday = buffer.readFloat();
      hospitality.day = buffer.readVarLong();
      hospitality.welcomed = buffer.readBoolean();
      hospitality.fed = buffer.readBoolean();
      return hospitality;
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeFloat(points);
      buffer.writeFloat(gainedToday);
      buffer.writeVarLong(day);
      buffer.writeBoolean(welcomed);
      buffer.writeBoolean(fed);
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putFloat(FIELD_POINTS, points);
      tag.putFloat(FIELD_GAINED_TODAY, gainedToday);
      tag.putLong(FIELD_DAY, day);
      tag.putBoolean(FIELD_WELCOMED, welcomed);
      tag.putBoolean(FIELD_FED, fed);
      return tag;
   }

   public static VisitorHospitality fromNbt(CompoundTag tag) {
      VisitorHospitality hospitality = new VisitorHospitality();
      hospitality.points = tag.getFloat(FIELD_POINTS);
      hospitality.gainedToday = tag.getFloat(FIELD_GAINED_TODAY);
      hospitality.day = tag.getLong(FIELD_DAY);
      hospitality.welcomed = tag.getBoolean(FIELD_WELCOMED);
      hospitality.fed = tag.getBoolean(FIELD_FED);
      return hospitality;
   }
}
