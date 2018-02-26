package com.sumsubstance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Env {
    private final static Logger log = LogManager.getLogger(Env.class);

    private final static Map<Class, Object> beans = new ConcurrentHashMap<>();

    public static void register(Class x) throws Exception {
        if (x.getDeclaredAnnotation(Bean.class) != null) {
            for (Class y : x.getInterfaces()) {
                Object z = x.getConstructor().newInstance();
                beans.put(y, z);
            }
        }
    }

    public static void init() {
        beans.values().stream().forEach((x)-> {
            Field[] fs = x.getClass().getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].getDeclaredAnnotation(AutoWire.class) != null) {
                    try {
                        fs[i].setAccessible(true);
                        fs[i].set(x, beans.get(fs[i].getType()));
                    } catch (IllegalAccessException e) {
                        log.error(e);
                    }
                }
            }
        });
    }

    public static<T> T get(Class<T> c) {
        return (T)beans.get(c);
    }
}
