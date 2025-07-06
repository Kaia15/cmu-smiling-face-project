# Facemini

Inspired by **CMU-17-214** course, in this project, you will work with concurrency in a Java/Spring Boot backend and a React frontend from scratch. You will get experience with writing asynchronous code, with error handling, and with handling state in React and Java/Spring.

https://github.com/user-attachments/assets/cac95d01-49ad-4bb1-b062-7ec3b8a18d8b

## Introduction

You will work on a semi-completed program *SmilingFaces* to analyze faces in Wikipedia pages -- for example are more people smiling in pictures of the Carnegie Mellon wikipedia page or in the University of Pittsburgh wikipedia page? In the web application, you can enter a *topic* for which a Wikipedia page exists and the program will identify all images in the page and determine with an ML model whether the picture contains smiling faces. It can also collect the pictures from other Wikipedia pages linked heavily from the target page ("include top neighbor topics").

The code will handle all the communication with Wikipedia and the Google Cloud Vision API, but the backend code is currently written synchronously (which is actually quite difficult to do and very unusual for Node code). As a consequence the backend can only respond to a single request at a time and it is very slow. The provided implementation is also bad at error handling.

The code consists of two related projects: The backend *java/spring boot* implementation in the root directory and the frontend *React* implementation in directory `frontend`.  Compile and run the code as follows:

* First, build the frontend
  * run `npm install`  in the `frontend/` directory
  * `npm run build` in `frontend/` to build the frontend, which will result in static pages in `frontend/build/`
* Second, build and run the backend
  * Make sure that you are signed into the Google Cloud API using `gcloud` (same as Lab 8)
  * `javac YourApplication.java` runs the server which you can then access at `http://localhost:8080`

The backend serves the frontend code in the root of the web server but also provides API endpoints for starting a task (`POST ?topic=${your_search_topic}`) both communicate in JSON format (using the [Long Running Operation with Polling Pattern](http://restalk-patterns.org/long-running-operation-polling.html) /*still on research*) . The frontend will make requests to the APIs to update the state within the page. If a job is not completed on the first request, it will check every second for updates.

In the user interface in the web browser you can enter a topic and start the analysis. Note that the response will take a very long time if you analyze any nontrivial pages. Analyzing the topic "Carnegie Mellon University" gathers and analyzes 30 images without neighboring pages (and many more with neighbors), which easily takes 30 seconds to respond. A good test page might be "David Tepper" which has only a single image and takes about 2 seconds to analyze.

## Tasks

**Concurrency in the backend.** Rewrite the backend to perform computations concurrently. We have the following requirements of the final implementation:

* The server makes requests to Wikipedia and the Google Cloud API concurrently, speeding up responses significantly.
* The server can answer multiple requests concurrently (i.e., multiple calls to `POST ?topic=${search_param}`).
* The server reports an error when more than 5 jobs are processed concurrently asking users to try again later. Reject additional requests with HTTP error code 503.
* The server never makes more than 5 concurrent requests to Wikipedia and never more than 5 concurrent requests to the Google Cloud API in order to not overload those servers (this limit is shared by all jobs).
* If multiple topics are analyzed, the server does not wait until all images are collected from all topics, but starts analyzing images as soon as the images from each topic are identified.

**Error handling.** Make the implementation robust to errors. Specifically, we expect you to handle the following kind of errors:

* When connections to Wikipedia or the Google Cloud API fail (error, timeout, or invalid results) retry two more times after a short wait of one second.
* When connections to Wikipedia or the Google Cloud API fail and cannot be recovered or any other computations fail, report an error message to the frontend gracefully. Your server should still be able to handle 5 concurrent jobs and up to 5 concurrent backend requests afterward.
* The backend validates inputs received from the frontend. Reject empty and invalid inputs with HTTP error code 400.

**Frontend improvements.** Improve the React frontend with some minor extensions

* Allow incremental loading in the frontend by polling regularly for updates from the backend. (This should work out of the box if the backend responds correctly)
* Show a progress bar while data is loaded.
* Show errors from the backend in the frontend, ideally with meaningful error messages.

**What not to change:** We plan to automate some testing of your code and ask you to NOT change the `Connections` interface and the signature of the `smilingFacesBackend` function. Make all external calls through the APIs in `Connections` and do not make web calls with any other API. You may, and probably should, develop your own abstractions on top of the functions in `Connections`.

## Solution
* Set up & Configure 3 Limiters (GCPLimiter, WikiLimiter, TaskLimiter): 

There are multiple threads in ThreadPool, but there is a maximum of 5 permits allowed to take on the jobs/requests received from the frontend. As one request comes in, one of the threads in the pool will pick it up. This thread tries to acquire the permit before running the job:

    - If acquired, the job can be processed and rejected; otherwise. Once the job completes, the thread releases the permit.
    
    - If waiting (for another later job, under the condition that all the 5 earlier requests are still pending), TaskLimiter rejects the job immediately.

* What does **At most 5 jobs are processed concurrently at any time** mean? 

(1) Regardless of the number of active threads, each time we proceed with a job, we always check whether the number of requests coming in exceeds the capacity of each limiter. 

(2) Also, we need to ensure that we do not wait for all the requests to process sequentially with the <u>chained pipeline</u>: 
**Send URL with "topic" parameter to Wikipedia -> get responses with image URLs -> send each URL to GCP -> get responses with image analysis result.** 
Meanwhile, we can proceed with these steps in parallel in multiple threads while still maintaining (1).

* Use `CompletableFuture` & Apply asynchronous functions `.runAsync(), .thenCompose()`: 

As soon as we retrieve the result from the previous step in the chained pipeline mentioned in (2), we release the limiter at **that** previous step immediately so it can allow new requests to proceed.

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
     * This acquisition for GCP is more complex since it requires a long time to process each image; therefore, we need to check and release the permit as soon as 
     * GCP returns the response of image analysis on GCPLimiter (CHECKPOINT 2)
     *
     * Use of `.stream()`: We do not need to wait for all the URL(s) collected in imageUrls, but as soon as we get the available URLs, we send them first to proceed with * GCP to save time (Lazily Evaluation)
     */
    
    List<CompletableFuture<Map<String, String>>> imageAnalysisFutures = imageUrls.stream()
    .map(url -> {
    CompletableFuture<Void> acquirePermit = CompletableFuture.runAsync(() -> {
        try {
            this.googleLimiter.tryAcquire(); 
        } catch (Exception e) {
            // throw Exception since the GCPLimiter reaches the capacity of 5 (OR 5 requests sent from the list of urls created by `fetchImage` are pending, and NONE of those returns back with image analysis result)
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

