// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

final class StandIn<T> {

    private final Class<T> type;
    private final Map<String, Function<Object[], Object>> answers = new HashMap<>();

    private StandIn(Class<T> type) {
        this.type = type;
    }

    static <T> StandIn<T> of(Class<T> type) {
        return new StandIn<>(type);
    }

    StandIn<T> answer(String method, Object value) {
        answers.put(method, arguments -> value);
        return this;
    }

    StandIn<T> compute(String method, Function<Object[], Object> answer) {
        answers.put(method, answer);
        return this;
    }

    T build() {
        Object instance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, arguments) -> {
                    String name = method.getName();
                    if (name.equals("hashCode") && method.getParameterCount() == 0) {
                        return System.identityHashCode(proxy);
                    }
                    if (name.equals("equals") && method.getParameterCount() == 1) {
                        return proxy == arguments[0];
                    }
                    if (name.equals("toString") && method.getParameterCount() == 0) {
                        return type.getSimpleName() + " stand-in";
                    }
                    Function<Object[], Object> answer = answers.get(name);
                    if (answer == null) {
                        throw new UnsupportedOperationException(type.getSimpleName() + "." + name);
                    }
                    return answer.apply(arguments == null ? new Object[0] : arguments);
                });
        return type.cast(instance);
    }
}
