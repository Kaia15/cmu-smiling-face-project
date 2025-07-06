package io.smilingface.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired; // Import Collectors
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
            if (!this.taskLimiter.tryAcquire()) {
                throw new IllegalStateException("Reached max capacity of 5 concurrent jobs for topic: " + topic + "!");
            }
            return topic;
        }, executorSrv)
        .thenCompose(t -> {
            System.out.println(Thread.currentThread().getName() + ": Acquired task limiter. Attempting to acquire Wikipedia limiter for topic: " + t);

            if (!this.wikiLimiter.tryAcquire()) {
                this.taskLimiter.release(); 
                System.out.println(Thread.currentThread().getName() + ": Released task limiter due to Wikipedia limiter blocking for topic: " + t);
                return CompletableFuture.failedFuture(new IllegalStateException("Wikipedia limiter blocked the request for topic: " + t + "."));
            }

            CompletableFuture<List<String>> fetchImagesFuture = CompletableFuture.supplyAsync(() -> {
                System.out.println(Thread.currentThread().getName() + ": Acquired Wikipedia limiter. Fetching images for topic: " + t);
                List<String> imageUrls = helper.fetchImage(t); 
                System.out.println(Thread.currentThread().getName() + ": Finished fetching images for topic: " + t + ". Found " + imageUrls.size() + " images.");
                return imageUrls;
            }, executorSrv)
            .whenComplete((result, ex) -> { 
                // Release wikiLimiter after fetchImage (async) completes
                this.wikiLimiter.release();
                System.out.println(Thread.currentThread().getName() + ": Released Wikipedia limiter for topic: " + t);
            });

            // Step 3: Chain image analysis after fetching images
            return fetchImagesFuture.thenCompose(imageUrls -> {
                List<CompletableFuture<Map<String, String>>> imageAnalysisFutures = imageUrls.stream()
                    .map(url -> {
                        // 1. Acquire the permit asynchronously
                        CompletableFuture<Void> acquirePermit = CompletableFuture.runAsync(() -> {
                            System.out.println(Thread.currentThread().getName() + ": Waiting to acquire Google limiter for URL: " + url + " (Topic: " + t + ")");
                            try {
                                this.googleLimiter.tryAcquire(); // This will block this specific thread until a permit is available
                                System.out.println(Thread.currentThread().getName() + ": Acquired Google limiter for URL: " + url + " (Topic: " + t + ")");
                            } catch (Exception e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Interrupted while acquiring Google limiter", e);
                            }
                        }, executorSrv); 

                        // 2. Once the permit is acquired, then initialize image analysis
                        return acquirePermit.thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                            try {
                                System.out.println(Thread.currentThread().getName() + ": Analyzing image: " + url + " (Topic: " + t + ")");
                                Map<String, String> analysisResult = helper.analyzeImage(url); 
                                System.out.println(Thread.currentThread().getName() + ": Finished analyzing image: " + url + " (Topic: " + t + ")");
                                return analysisResult;
                            } catch (Exception err) {
                                System.err.println(Thread.currentThread().getName() + ": Error analyzing image " + url + " for topic " + t + ": " + err.getMessage());
                                return Map.of("image", url, "status", "analysis_failed", "error", "HTTP Response Code: 429");
                            } finally {
                                // Release permit after analysis completes
                                this.googleLimiter.release(); 
                                System.out.println(Thread.currentThread().getName() + ": Released Google limiter for URL: " + url + " (Topic: " + t + ")");
                            }
                        }, executorSrv)); // Run the analysis on googleApiExecutor as well
                    })
                    .collect(Collectors.toList());

                return CompletableFuture.allOf(imageAnalysisFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> imageAnalysisFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
            });
        })
        .whenComplete((result, ex) -> { 
            this.taskLimiter.release();
            System.out.println(Thread.currentThread().getName() + ": Released task limiter for topic (final completion/error): " + topic);
        })
        .exceptionally(ex -> {
            System.err.println(Thread.currentThread().getName() + ": Error processing topic " + topic + ": " + ex.getMessage());
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof IllegalStateException) {
                throw (IllegalStateException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException("Error in task for topic: " + topic, cause);
            }
        });
    }
}