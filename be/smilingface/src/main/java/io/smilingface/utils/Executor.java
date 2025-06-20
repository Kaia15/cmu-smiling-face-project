package io.smilingface.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Executor implements IExecutor {
    private final int POOL_SIZE = 10;
    private ILimiter wikiLimiter;
    private ILimiter googleLimiter;
    private ILimiter taskLimiter;
    private final Helper helper;

    private ExecutorService executorSrv = Executors.newFixedThreadPool(POOL_SIZE);

    @Autowired
    public Executor(ILimiter wikiLimiter, ILimiter googleLimiter, ILimiter taskLimiter, Helper helper) {
        this.wikiLimiter = wikiLimiter;
        this.googleLimiter = googleLimiter;
        this.taskLimiter = taskLimiter;
        this.helper = helper;
    }

    public void submit(String topic) {
        executorSrv.submit(() -> {
            if (this.taskLimiter.tryAcquire()) {
                try {
                    // TO-DO: impplement the request coming in
                    if (this.wikiLimiter.tryAcquire()) {
                        try {
                            List<String> imageUrls = helper.fetchImage(topic);
                            for (String url : imageUrls) {
                                if (this.googleLimiter.tryAcquire()) {
                                    try {
                                        helper.analyzeImage(url);
                                    } finally {
                                        this.googleLimiter.release();
                                    }
                                }
                            }
                        } finally {
                            this.wikiLimiter.release();
                        }
                    } else {
                        return;
                    }
                } finally {
                    // release the permit if the job's done
                    this.taskLimiter.release();
                }
            } else {
                throw new IllegalStateException("Reached max capacity of 5 concurrent jobs!");
            }
        });
    }
}
