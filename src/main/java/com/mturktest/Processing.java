package com.mturktest;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class Processing {
    public static final int MAX_ASSIGNMENTS = 3;

    public static class Assignment implements Comparable<Assignment> {
        private final String hitId;
        private final String asnId;
        private final String workerId;
        private final long expiration;
        private final String taskId;
        private final boolean result;

        public Assignment(String hitId, String asnId, String workerId, long expiration, String taskId, boolean result) {
            this.hitId = hitId;
            this.asnId = asnId;
            this.workerId = workerId;
            this.expiration = expiration;
            this.taskId = taskId;
            this.result = result;
        }

        public String getHitId() {
            return hitId;
        }

        public String getAsnId() {
            return asnId;
        }

        public String getWorkerId() {
            return workerId;
        }

        public long getExpiration() {
            return expiration;
        }

        public String getTaskId() {
            return taskId;
        }

        public boolean isResult() {
            return result;
        }

        @Override
        public int compareTo(Assignment o) {
            return (int)(expiration - o.getExpiration());
        }
    }

    private final Cache<String, Actor> state = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

    private static class Actor {
        private final Collection<Assignment> state = new PriorityQueue<>();
        private final BiConsumer<String, Boolean> callback;
        private final int max;

        private Actor(BiConsumer<String, Boolean> callback, int max) {
            this.callback = callback;
            this.max = max;
        }

        static Actor actor(BiConsumer<String, Boolean> callback, int max) {
            return new Actor(callback, max);
        }

        void receive(Assignment a) {
            state.add(a);
            if (state.size() >= max) callback.accept(a.getHitId(), result());
        }

        private boolean result() {
            System.out.println("calculating result");
            long trues = state.stream().filter(Assignment::isResult).count();
            return trues >= state.size() - trues;
        }
    }

    public void receive(Assignment a, BiConsumer<String, Boolean> callback) {
        System.out.println("receiving: " + a);
        try {
            state.get(a.getHitId(), () -> Actor.actor(callback, MAX_ASSIGNMENTS)).receive(a);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
