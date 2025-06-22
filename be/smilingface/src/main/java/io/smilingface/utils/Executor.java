package io.smilingface.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    public Future<List<Map<String, String>>> submit(String topic) {
        return executorSrv.submit(() -> {
            if (!this.taskLimiter.tryAcquire()) {
                throw new IllegalStateException("Reached max capacity of 5 concurrent jobs!");
            }

            try {
                if (!this.wikiLimiter.tryAcquire()) {
                    throw new IllegalStateException("Wikipedia limiter blocked the request.");
                }

                try {
                    List<String> imageUrls = helper.fetchImage(topic);
                    for (String url : imageUrls) {
                        
                        if (this.googleLimiter.tryAcquire()) {
                            try {
                                return helper.analyzeImage(url);
                            } catch (Exception err) {
                                err.printStackTrace();
                            } finally {
                                this.googleLimiter.release();
                            }
                        }
                    }
                } finally {
                    this.wikiLimiter.release();
                }
            } finally {
                this.taskLimiter.release();
            }

            return Collections.emptyList();
        });
    }

}
