package io.smilingface.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public CompletableFuture<List<Map<String, String>>> submit(String topic) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, String>> finalResponse = new ArrayList<>();
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
                        System.out.println(url);
                        if (this.googleLimiter.tryAcquire()) {
                            try {
                                finalResponse.add(helper.analyzeImage(url));
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

            return finalResponse;
        }, executorSrv);
    }

}
