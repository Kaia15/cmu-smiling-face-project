## Backend
### Requirements
**1. Concurrency in the backend.** Rewrite the backend to perform computations concurrently. We have the following requirements of the final implementation:

* 1.1 The server makes requests to Wikipedia and the Google Cloud API concurrently, speeding up responses significantly.
* 1.2 The server can answer multiple requests concurrently (i.e., multiple calls to `/job` and `/job/:id`).
* 1.3 The server reports an error when more than 5 jobs are processed concurrently asking users to try again later. Reject additional requests with HTTP error code 503.
* 1.4 The server never makes more than 5 concurrent requests to Wikipedia and never more than 5 concurrent requests to the Google Cloud API in order to not overload those servers (this limit is shared by all jobs).
* 1.5 If multiple topics are analyzed, the server does not wait until all images are collected from all topics, but starts analyzing images as soon as the images from each topic are identified.

**2. Error handling.** Make the implementation robust to errors. Specifically we expect you to handle the following kind of errors:

* When connections to Wikipedia or the Google Cloud API fail (error, timeout, or invalid results) retry two more times after a short wait of one second.
* When connections to Wikipedia or the Google Cloud API fail and cannot be recovered or any other computations fail, report an error message to the frontend gracefully. Your server should still be able to handle 5 concurrent jobs and up to 5 concurrent backend requests afterward.
* The backend validates inputs received from the frontend. Reject empty and invalid inputs with HTTP error code 400.

**3. Solution** 

* Set up & Configure 3 Limiters (GCPLimiter, WikiLimiter, TaskLimiter): 

There are multiple threads in ThreadPool, but there are maximum 5 permits allowed to take on the jobs/requests received from the frontend. As one request comes in, one of threads in the pool will pick it up. This thread tries to acquire the permit before running the job:
    - If acquired, the job can be processed and rejected otherwise. Once the job completes, the thread releases the permit.
    - If waiting (for another later job, within the condition that all the 5 earlier requests are still pending), TaskLimiter rejects the job coming immediately.

* What does **At most 5 jobs are processed concurrently at any time** actually mean? 

No matter how many the number of active threads, each time of proceeding a job, we always check whether the number of requests coming in exceeds the capacity of each limiter (1). 

Also, we need to ensure that we do not wait all the requests process sequentially with the chained pipeline: send url with "topic" parameter to Wikipedia -> get responses with image urls -> send each url to GCP -> get responses with image analysis result. Meanwhile, we can proceed these steps parallelly in multiple threads while still keeping (1) maintain (2).

* Use `CompletableFuture` & Apply asynchronous functions `.runAsync(), .thenCompose()`: 

As soon as we retrieve the result from previous step in the chained pipeline mentioned in (2), we release the limiter at previous step immediately so it can allow new requests to proceed.

Checkpoint 1:

    ```
    return CompletableFuture.supplyAsync(() -> {
            if (!this.taskLimiter.tryAcquire()) {
                // throw Exception since the TaskLimiter reaches the capacity of 5 (OR 5 requests sent from FE to BE are pending)
            }
            return topic;
        }, executorSrv)
        .thenCompose(t -> {
            if (!this.wikiLimiter.tryAcquire()) {
                this.taskLimiter.release(); 
                // throw Exception since the WikiLimiter reaches the capacity of 5 (OR 5 requests already sent to Wikipedia and NONE of them returns back!)
            }

            CompletableFuture<List<String>> fetchImagesFuture = CompletableFuture.supplyAsync(() -> {
                List<String> imageUrls = helper.fetchImage(t); 
                return imageUrls;
            }, executorSrv)
            .whenComplete((result, ex) -> { 
                
                !---------------------CHECKPOINT 1 ----------------------!
                this.wikiLimiter.release();
            });
        ...
        })
    ```
    
Checkpoint 2:

    ```

    /*
     * This acquisition for GCP is more complex since it requires long timing to proceed each image, therefore, we need to check and release the permit as soon as 
     * GCP returns the response of image analysis on GCPLimiter (CHECKPOINT 2)
     *
     * Use of `.stream()`: We do not need to wait for all the URL(s) collected in imageUrls but as soon as we get the available urls, we send them first to proceed with * GCP to save time
     */
    
    List<CompletableFuture<Map<String, String>>> imageAnalysisFutures = imageUrls.stream()
    .map(url -> {
    CompletableFuture<Void> acquirePermit = CompletableFuture.runAsync(() -> {
        try {
            this.googleLimiter.tryAcquire(); 
        } catch (Exception e) {
            // throw Exception since the GCPLimiter reaches the capacity of 5 (OR 5 requests sent from the list of urls created by `fetchImage` are pending and NONE of those returns back with image analysis result)
        }
        }, executorSrv); 

        return acquirePermit.thenCompose(v -> CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> analysisResult = helper.analyzeImage(url); 
                return analysisResult;
            } catch (Exception err) {
                return Map.of("image", url, "status", "analysis_failed", "error", "HTTP Response Code: 429");
            } finally {
                                
                !-------------- CHECKPOINT 2 ----------------------------------!

                this.googleLimiter.release(); 
            }
            }, executorSrv)).collect(Collectors.toList());

    // more steps to join the results from all active threads
    })
        
    ```

