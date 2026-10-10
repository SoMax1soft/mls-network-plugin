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

    void start();

    void stop();
}
