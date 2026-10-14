// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

public interface ServerHooks {

    ServerHooks NONE = new ServerHooks() {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    static ServerHooks inOrder(ServerHooks first, ServerHooks second) {
        return new ServerHooks() {
            @Override
            public void start() {
                first.start();
                second.start();
            }

            @Override
            public void stop() {
                second.stop();
                first.stop();
            }
        };
    }

    void start();

    void stop();
}
