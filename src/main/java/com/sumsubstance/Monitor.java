package com.sumsubstance;

@Bean
public class Monitor {
    @AutoWire
    private Proxy proxy;
    @AutoWire
    private Exporter exporter;

    public void scan() {
        exporter.export(proxy.processReviewablesHits());
    }
}
