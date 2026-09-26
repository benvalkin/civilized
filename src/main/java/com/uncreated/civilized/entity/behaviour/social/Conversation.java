package com.uncreated.civilized.entity.behaviour.social;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;

public class Conversation {

   public static final int PAIR = 2;

   @Getter
   private final CivilizedVillager host;
   private final int maxMembers;
   private final List<CivilizedVillager> members = new ArrayList<>();
   @Getter
   private boolean ended;

   public Conversation(CivilizedVillager host, int maxMembers) {
      this.host = host;
      this.maxMembers = maxMembers;
      members.add(host);
   }

   public boolean join(CivilizedVillager villager) {
      if (ended || members.size() >= maxMembers || members.contains(villager))
         return false;

      members.add(villager);
      return true;
   }

   public void leave(CivilizedVillager villager) {
      members.remove(villager);

      if (villager == host || members.size() < 2)
         ended = true;
   }

   public boolean isMember(CivilizedVillager villager) {
      return !ended && members.contains(villager);
   }

   public List<CivilizedVillager> members() {
      return List.copyOf(members);
   }

   public List<CivilizedVillager> othersThan(CivilizedVillager villager) {
      return members.stream().filter(member -> member != villager).toList();
   }
}
