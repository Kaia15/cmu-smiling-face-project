package io.smilingface.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Executor {
    private final int POOL_SIZE = 10;
    private ILimiter limiter;
    private ExecutorService executorSrv = Executors.newFixedThreadPool(POOL_SIZE);

    public Executor(ILimiter limiter) {
        this.limiter = limiter;
    }

    public void submit(Runnable task) {
        executorSrv.submit(() -> {
            if (this.limiter.tryAcquire()) {
                try {
                    // TO-DO: impplement the request coming in
                    task.run();
                } finally {
                    // release the permit if the job's done
                    this.limiter.release();
                }
            } else {

            }
        });
    }
}
