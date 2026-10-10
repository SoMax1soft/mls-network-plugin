// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bus;

import java.util.function.Consumer;

public interface EventBus {

    <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler);

    void publish(Object event);
}
