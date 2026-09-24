package com.uncreated.civilized.core;

import java.util.HashMap;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

public abstract class InMemoryDB<K, V> {

   private final HashMap<K, V> data;

   public InMemoryDB() {
      this.data = new HashMap<>();
   }

   protected abstract K getKey(V obj);

   protected void index(V obj) {
   }

   protected void unindex(V obj) {
   }

   public final void add(V obj) {
      K key = getKey(obj);
      V replaced = data.put(key, obj);
      if (replaced != null)
         unindex(replaced);
      index(obj);
   }

   public final void reindex(K key) {
      V obj = data.get(key);
      if (obj == null)
         return;

      unindex(obj);
      index(obj);
   }

   public final Optional<V> remove(K key) {
      Optional<V> removed = Optional.ofNullable(data.remove(key));
      removed.ifPresent(this::unindex);
      return removed;
   }

   public final V get(K key) {
      return find(key).orElseThrow();
   }

   public final Optional<V> find(K key) {
      return Optional.ofNullable(data.get(key));
   }

   public final boolean exists(K key) {
      return data.containsKey(key);
   }

   public final ImmutableList<V> all() {
      return ImmutableList.copyOf(data.values());
   }
}
