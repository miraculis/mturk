import com.sumsubstance.AutoWire;
import com.sumsubstance.Bean;
import com.sumsubstance.Env;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class TestEnv {
    static Executor e;

    @BeforeClass
    public static void init() {
        e = Executors.newFixedThreadPool(2);
    }

    @Bean
    public static class A {

    }

    @Bean
    public static class B {
        @AutoWire
        private A a;

        public void check() {
            assert a != null;
        }
    }

    public static class C {
    }

    @Test
    public void testAutowire() {
        B b = Env.get(B.class);
        b.check();
    }

    @Test
    public void testBean() {
        C c = Env.get(C.class);
        assert c == null;
    }

    @Test
    public void testConcurrency() {
        IntStream.of(10).forEach((x) -> e.execute(() -> {
            B b = Env.get(B.class);
            b.check();
        }));
    }
}
