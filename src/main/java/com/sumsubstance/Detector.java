package com.sumsubstance;

import java.util.Collection;

@Bean
public class Detector {
    @AutoWire
    private Proxy proxy;

    public void scan() {
        proxy.createHits(collectFreshDocIdsFromDb());
    }

    private Collection<String> collectFreshDocIdsFromDb() {
        return null; //todo: integration with core
    }
}
