package com.sumsubstance;

import java.util.Collection;

@Bean
public class Detecter {
    @AutoWire
    private MTurk proxy;

    public void scan() {
        proxy.createHits(collectFreshDocIdsFromDb());
    }

    private Collection<String> collectFreshDocIdsFromDb() {
        return null; //todo: integration with core
    }
}
