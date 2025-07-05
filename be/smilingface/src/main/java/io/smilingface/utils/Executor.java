// package io.smilingface.utils;

// import java.io.IOException;
// import java.io.InputStream;
// import java.net.URL;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.stream.Collectors;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

// import com.google.cloud.vision.v1.AnnotateImageRequest;
// import com.google.cloud.vision.v1.AnnotateImageResponse;
// import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
// import com.google.cloud.vision.v1.FaceAnnotation;
// import com.google.cloud.vision.v1.Feature;
// import com.google.cloud.vision.v1.Image;
// import com.google.cloud.vision.v1.ImageAnnotatorClient;
// import com.google.protobuf.ByteString;

// // Placeholder for logging (replace with actual SLF4J/Logback imports if needed)
// // import org.slf4j.Logger;
// // import org.slf4j.LoggerFactory;

// @Component
// public class Executor implements IExecutor {

//     // private static final Logger logger = LoggerFactory.getLogger(Executor.class); // Uncomment for proper logging

//     private final int POOL_SIZE = 10; // For general tasks and dispatching
//     private final int GOOGLE_BATCH_SIZE = 5; // Define your batch size for Google Vision API
//     private ILimiter wikiLimiter;
//     private ILimiter googleLimiter; // This limiter now applies to BATCH requests
//     private ILimiter taskLimiter;
//     private final Helper helper;

//     private ExecutorService executorSrv = Executors.newFixedThreadPool(POOL_SIZE);

//     @Autowired
//     public Executor(ILimiter wikiLimiter, ILimiter googleLimiter, ILimiter taskLimiter, Helper helper) {
//         this.wikiLimiter = wikiLimiter;
//         this.googleLimiter = googleLimiter; // googleLimiter now for batches
//         this.taskLimiter = taskLimiter;
//         this.helper = helper;
//     }

//     /**
//      * Helper method to convert a single URL to an AnnotateImageRequest.
//      * This method handles fetching image bytes and building the request.
//      * It also includes the 20MB size check.
//      * Uses try-with-resources for InputStream for proper closing.
//      *
//      * @param imageUrl The URL of the image to process.
//      * @return An AnnotateImageRequest object.
//      * @throws IOException If there's an error fetching the image or if it exceeds the size limit.
//      */
//     private AnnotateImageRequest createAnnotateImageRequest(String imageUrl) throws IOException {
//         String fixedUrl = imageUrl.replace("\\", "/"); // Ensure consistent URL format
//         // Use try-with-resources to ensure InputStream is closed
//         try (InputStream input = new URL(fixedUrl).openStream()) {
//             ByteString imgBytes = ByteString.readFrom(input);

//             if (imgBytes.size() > 20_000_000) { // 20MB limit
//                 // logger.error("Error: Image size exceeds 20MB limit for URL: {}", fixedUrl); // Use logger
//                 System.err.println("Error: Image size exceeds 20MB limit for URL: " + fixedUrl);
//                 throw new IOException("Image size exceeds 20MB limit for URL: " + fixedUrl);
//             }

//             Image img = Image.newBuilder().setContent(imgBytes).build();
//             Feature feat = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build(); // Assuming FACE_DETECTION

//             return AnnotateImageRequest.newBuilder()
//                     .addFeatures(feat)
//                     .setImage(img)
//                     .build();
//         }
//     }

//     /**
//      * Processes a single AnnotateImageResponse and extracts face data into a Map.
//      * This helps centralize the parsing logic for individual image results from a batch.
//      *
//      * @param res The AnnotateImageResponse for a single image.
//      * @param originalImageUrl The original URL of the image corresponding to this response.
//      * @return A Map containing the analysis results for the image.
//      */
//     private Map<String, String> parseFaceAnnotationResponse(AnnotateImageResponse res, String originalImageUrl) {
//         Map<String, String> faceData = new HashMap<>();
//         faceData.put("image", originalImageUrl); // Add the original URL to the result map

//         if (res.hasError()) {
//             // logger.error("Error for {}: {}", originalImageUrl, res.getError().getMessage()); // Use logger
//             System.err.format("Error for %s: %s%n", originalImageUrl, res.getError().getMessage());
//             faceData.put("status", "error");
//             faceData.put("error", res.getError().getMessage());
//         } else if (!res.getFaceAnnotationsList().isEmpty()) {
//             FaceAnnotation annotation = res.getFaceAnnotationsList().get(0); // Assuming you want the first face

//             faceData.put("status", "success");
//             faceData.put("anger", annotation.getAngerLikelihood().name());
//             faceData.put("joy", annotation.getJoyLikelihood().name());
//             faceData.put("surprise", annotation.getSurpriseLikelihood().name());
//             faceData.put("boundingPoly", annotation.getBoundingPoly().toString()); // For simplicity, toString()
//         } else {
//             faceData.put("status", "no_face_detected");
//             faceData.put("note", "No face detected.");
//         }
//         return faceData;
//     }

//     /**
//      * Performs analysis on a BATCH of image URLs using the Google Cloud Vision API's batch endpoint.
//      * This method is the core interaction with the GCP Vision client library.
//      * It builds a list of requests, sends them in one batch API call, and then
//      * maps the responses back to their original URLs, including handling for
//      * images that failed request preparation or API calls.
//      *
//      * @param imageUrls A list of image URLs to analyze.
//      * @return A list of Maps, where each map contains the analysis result for an image.
//      * @throws Exception If there's a critical error during the batch processing.
//      */
//     public List<Map<String, String>> analyzeImagesBatch(List<String> imageUrls) throws Exception {
//         if (imageUrls == null || imageUrls.isEmpty()) {
//             return new ArrayList<>();
//         }

//         // logger.info(Thread.currentThread().getName() + ": Preparing batch request for {} images.", imageUrls.size()); // Use logger
//         System.out.println(Thread.currentThread().getName() + ": Preparing batch request for " + imageUrls.size() + " images.");

//         List<AnnotateImageRequest> validRequests = new ArrayList<>();
//         // This list maintains the order of original URLs that correspond to the validRequests list.
//         // Google Vision API guarantees response order matches request order, so this is crucial.
//         List<String> urlsForValidRequests = new ArrayList<>();
//         List<Map<String, String>> immediateErrors = new ArrayList<>(); // To collect errors for images that fail *before* the API call

//         for (String url : imageUrls) {
//             try {
//                 AnnotateImageRequest request = createAnnotateImageRequest(url);
//                 validRequests.add(request);
//                 urlsForValidRequests.add(url); // Add original URL for successful request preparation
//             } catch (IOException e) {
//                 // logger.error(Thread.currentThread().getName() + ": Failed to prepare request for {}: {}", url, e.getMessage()); // Use logger
//                 System.err.println(Thread.currentThread().getName() + ": Failed to prepare request for " + url + ": " + e.getMessage());
//                 Map<String, String> errorResult = new HashMap<>();
//                 errorResult.put("image", url); // Use "image" for consistency
//                 errorResult.put("status", "request_preparation_failed");
//                 errorResult.put("error", e.getMessage());
//                 immediateErrors.add(errorResult);
//             }
//         }

//         // If no valid requests were prepared (e.g., all images were too large or inaccessible)
//         if (validRequests.isEmpty()) {
//             // logger.warn(Thread.currentThread().getName() + ": No valid requests prepared for batch. Returning immediate errors."); // Use logger
//             System.out.println(Thread.currentThread().getName() + ": No valid requests prepared for batch. Returning immediate errors.");
//             return immediateErrors; // Return all collected immediate errors
//         }

//         List<Map<String, String>> apiCallResults = new ArrayList<>();
//         try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
//             // logger.info(Thread.currentThread().getName() + ": Sending {} requests in batch to GCP Vision API.", validRequests.size()); // Use logger
//             System.out.println(Thread.currentThread().getName() + ": Sending " + validRequests.size() + " requests in batch to GCP Vision API.");
//             BatchAnnotateImagesResponse response = client.batchAnnotateImages(validRequests);
//             List<AnnotateImageResponse> responses = response.getResponsesList();
//             // logger.info(Thread.currentThread().getName() + ": Received {} responses from GCP Vision API.", responses.size()); // Use logger
//             System.out.println(Thread.currentThread().getName() + ": Received " + responses.size() + " responses from GCP Vision API.");

//             // Iterate through responses, using urlsForValidRequests to map back to original URLs
//             for (int i = 0; i < responses.size(); i++) {
//                 AnnotateImageResponse res = responses.get(i);
//                 String originalUrl = urlsForValidRequests.get(i); // Guaranteed to be in sync due to careful list building
//                 apiCallResults.add(parseFaceAnnotationResponse(res, originalUrl));
//             }
//         } catch (Exception e) {
//             // logger.error(Thread.currentThread().getName() + ": Error calling Google Cloud Vision API for batch: {}", e.getMessage(), e); // Use logger
//             System.err.println(Thread.currentThread().getName() + ": Error calling Google Cloud Vision API for batch: " + e.getMessage());
//             // On a top-level batch API call failure, mark all images that were part of this batch as failed
//             return imageUrls.stream() // Use original imageUrls to capture all intended images for this batch
//                     .map(url -> Map.of("image", url, "status", "batch_api_call_failed", "error", e.getMessage()))
//                     .collect(Collectors.toList());
//         }

//         // Combine immediate errors with results from successful API calls
//         List<Map<String, String>> combinedResults = new ArrayList<>(immediateErrors);
//         combinedResults.addAll(apiCallResults);

//         // If you need the final list to preserve the original order of `imageUrls` (including those that failed
//         // request preparation), you'd need a more complex mapping here, e.g., building a map from URL to result,
//         // then iterating over the original `imageUrls` to construct the final list.
//         // For simplicity, this combines all results (errors first, then successful API calls).
//         return combinedResults;
//     }

//     /**
//      * Submits a topic for image analysis, coordinating fetching images,
//      * batching them for Google Cloud Vision API, and returning results asynchronously.
//      * Enforces concurrency limits using various ILimiter instances.
//      *
//      * @param topic The topic to analyze.
//      * @return A CompletableFuture that will eventually hold the list of analysis results.
//      */
//     public CompletableFuture<List<Map<String, String>>> submit(String topic) {
//         return CompletableFuture.supplyAsync(() -> {
//             // logger.info(Thread.currentThread().getName() + ": Attempting to acquire task limiter for topic: {}", topic); // Use logger
//             System.out.println(Thread.currentThread().getName() + ": Attempting to acquire task limiter for topic: " + topic);

//             if (!this.taskLimiter.tryAcquire()) {
//                 // logger.warn("Reached max capacity of 5 concurrent jobs for topic: {}", topic); // Use logger
//                 throw new IllegalStateException("Reached max capacity of 5 concurrent jobs for topic: " + topic + "!");
//             }

//             try {
//                 // logger.info(Thread.currentThread().getName() + ": Acquired task limiter. Attempting to acquire Wikipedia limiter for topic: {}", topic); // Use logger
//                 System.out.println(Thread.currentThread().getName() + ": Acquired task limiter. Attempting to acquire Wikipedia limiter for topic: " + topic);
//                 List<String> imageUrls;
//                 try {
//                     if (!this.wikiLimiter.tryAcquire()) {
//                         // logger.warn("Wikipedia limiter blocked the request for topic: {}", topic); // Use logger
//                         throw new IllegalStateException("Wikipedia limiter blocked the request for topic: " + topic + ".");
//                     }
//                     // logger.info(Thread.currentThread().getName() + ": Acquired Wikipedia limiter. Fetching images for topic: {}", topic); // Use logger
//                     System.out.println(Thread.currentThread().getName() + ": Acquired Wikipedia limiter. Fetching images for topic: " + topic);
//                     imageUrls = helper.fetchImage(topic); // This is still blocking for the current thread
//                     // logger.info(Thread.currentThread().getName() + ": Finished fetching images for topic: {}. Found {} images.", topic, imageUrls.size()); // Use logger
//                     System.out.println(Thread.currentThread().getName() + ": Finished fetching images for topic: " + topic + ". Found " + imageUrls.size() + " images.");
//                 } finally {
//                     this.wikiLimiter.release();
//                     // logger.info(Thread.currentThread().getName() + ": Released Wikipedia limiter for topic: {}", topic); // Use logger
//                     System.out.println(Thread.currentThread().getName() + ": Released Wikipedia limiter for topic: " + topic);
//                 }

//                 // --- BATCHING AND ASYNCHRONOUS BATCH ANALYSIS ---
//                 // Divide imageUrls into batches
//                 List<List<String>> batches = new ArrayList<>();
//                 for (int i = 0; i < imageUrls.size(); i += GOOGLE_BATCH_SIZE) {
//                     int endIndex = Math.min(i + GOOGLE_BATCH_SIZE, imageUrls.size());
//                     batches.add(imageUrls.subList(i, endIndex));
//                 }

//                 // Create a list of CompletableFutures, one for each batch analysis
//                 List<CompletableFuture<List<Map<String, String>>>> batchAnalysisFutures = batches.stream()
//                     .map(batchOfUrls -> CompletableFuture.supplyAsync(() -> {
//                         // logger.info(Thread.currentThread().getName() + ": Attempting to acquire Google limiter for a batch of {} URLs (Topic: {}).", batchOfUrls.size(), topic); // Use logger
//                         System.out.println(Thread.currentThread().getName() + ": Attempting to acquire Google limiter for a batch of " + batchOfUrls.size() + " URLs (Topic: " + topic + ")");
//                         if (this.googleLimiter.tryAcquire()) {
//                             try {
//                                 // logger.info(Thread.currentThread().getName() + ": Acquired Google limiter. Analyzing batch of {} URLs (Topic: {}).", batchOfUrls.size(), topic); // Use logger
//                                 System.out.println(Thread.currentThread().getName() + ": Acquired Google limiter. Analyzing batch of " + batchOfUrls.size() + " URLs (Topic: " + topic + ")");
//                                 List<Map<String, String>> batchResults = analyzeImagesBatch(batchOfUrls); // Direct call within Executor
//                                 // logger.info(Thread.currentThread().getName() + ": Finished analyzing batch of {} URLs (Topic: {}).", batchOfUrls.size(), topic); // Use logger
//                                 System.out.println(Thread.currentThread().getName() + ": Finished analyzing batch of " + batchOfUrls.size() + " URLs (Topic: " + topic + ")");
//                                 return batchResults;
//                             } catch (Exception err) {
//                                 // logger.error(Thread.currentThread().getName() + ": Error analyzing batch for topic {}: {}", topic, err.getMessage(), err); // Use logger
//                                 System.err.println(Thread.currentThread().getName() + ": Error analyzing batch for topic " + topic + ": " + err.getMessage());
//                                 // Return error maps for all URLs in the failed batch
//                                 return batchOfUrls.stream()
//                                     .map(url -> Map.of("image", url, "status", "batch_analysis_failed", "error", err.getMessage())) // Consistent "image" key
//                                     .collect(Collectors.toList());
//                             } finally {
//                                 this.googleLimiter.release();
//                                 // logger.info(Thread.currentThread().getName() + ": Released Google limiter for batch (Topic: {}).", topic); // Use logger
//                                 System.out.println(Thread.currentThread().getName() + ": Released Google limiter for batch (Topic: " + topic + ")");
//                             }
//                         } else {
//                             // logger.warn(Thread.currentThread().getName() + ": Google limiter blocked batch analysis for {} URLs (Topic: {}).", batchOfUrls.size(), topic); // Use logger
//                             System.out.println(Thread.currentThread().getName() + ": Google limiter blocked batch analysis for " + batchOfUrls.size() + " URLs (Topic: " + topic + ")");
//                             // Return results indicating that the batch was skipped
//                             return batchOfUrls.stream()
//                                 .map(url -> Map.of("image", url, "status", "skipped_google_limit_batch")) // Consistent "image" key
//                                 .collect(Collectors.toList());
//                         }
//                     }, executorSrv)) // Each batch analysis runs on a thread from executorSrv
//                     .collect(Collectors.toList());

//                 // Wait for all batch analysis futures to complete
//                 CompletableFuture<Void> allOfBatches = CompletableFuture.allOf(
//                     batchAnalysisFutures.toArray(new CompletableFuture[0])
//                 );

//                 // Collect results from all batches and flatten them into a single list
//                 List<Map<String, String>> finalResponse = allOfBatches.thenApply(v ->
//                     batchAnalysisFutures.stream()
//                         .map(CompletableFuture::join) // Get the List<Map<String, String>> from each batch future
//                         .flatMap(List::stream)         // Flatten the List<List<Map<String, String>>> into a single List
//                         .collect(Collectors.toList())
//                 ).join(); // Block current executorSrv thread until all batches are done and results collected

//                 return finalResponse;

//             } catch (IllegalStateException e) {
//                 // logger.error(Thread.currentThread().getName() + ": Limiter error during processing for topic {}: {}", topic, e.getMessage(), e); // Use logger
//                 throw e; // Re-throw to propagate to exceptionally
//             } catch (Exception e) {
//                 // logger.error(Thread.currentThread().getName() + ": General error during processing for topic {}: {}", topic, e.getMessage(), e); // Use logger
//                 System.err.println(Thread.currentThread().getName() + ": General error during processing for topic: " + topic + ": " + e.getMessage());
//                 throw new RuntimeException("Error in task for topic: " + topic, e);
//             } finally {
//                 this.taskLimiter.release();
//                 // logger.info(Thread.currentThread().getName() + ": Released task limiter for topic: {}", topic); // Use logger
//                 System.out.println(Thread.currentThread().getName() + ": Released task limiter for topic: " + topic);
//             }
//         }, executorSrv);
//     }

//     // You might want a shutdown hook for your executorSrv if not managed by Spring's lifecycle
//     // @PreDestroy // Requires import javax.annotation.PreDestroy if using Java EE / Jakarta EE
//     // public void shutdownExecutor() {
//     //     logger.info("Shutting down executor service.");
//     //     executorSrv.shutdown();
//     //     try {
//     //         if (!executorSrv.awaitTermination(60, TimeUnit.SECONDS)) {
//     //             logger.warn("Executor service did not terminate in time, forcing shutdown.");
//     //             executorSrv.shutdownNow();
//     //         }
//     //     } catch (InterruptedException e) {
//     //         logger.error("Executor service termination interrupted, forcing shutdown.", e);
//     //         executorSrv.shutdownNow();
//     //         Thread.currentThread().interrupt(); // Restore interrupt status
//     //     }
//     // }
// }

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
            // No longer need finalResponse here initially, it will be built asynchronously
            System.out.println(Thread.currentThread().getName() + ": Attempting to acquire task limiter for topic: " + topic);

            if (!this.taskLimiter.tryAcquire()) {
                throw new IllegalStateException("Reached max capacity of 5 concurrent jobs for topic: " + topic + "!");
            }

            try {
                System.out.println(Thread.currentThread().getName() + ": Acquired task limiter. Attempting to acquire Wikipedia limiter for topic: " + topic);
                if (!this.wikiLimiter.tryAcquire()) {
                    throw new IllegalStateException("Wikipedia limiter blocked the request for topic: " + topic + ".");
                }

                List<String> imageUrls;
                try {
                    System.out.println(Thread.currentThread().getName() + ": Acquired Wikipedia limiter. Fetching images for topic: " + topic);
                    imageUrls = helper.fetchImage(topic); // This is still blocking for the current thread
                    System.out.println(Thread.currentThread().getName() + ": Finished fetching images for topic: " + topic + ". Found " + imageUrls.size() + " images.");
                } finally {
                    this.wikiLimiter.release();
                    System.out.println(Thread.currentThread().getName() + ": Released Wikipedia limiter for topic: " + topic);
                }

                List<CompletableFuture<Map<String, String>>> imageAnalysisFutures = imageUrls.stream()
                    .map(url -> CompletableFuture.supplyAsync(() -> {
                        System.out.println(Thread.currentThread().getName() + ": Attempting to acquire Google limiter for URL: " + url + " (Topic: " + topic + ")");
                        if (this.googleLimiter.tryAcquire()) {
                            try {
                                System.out.println(Thread.currentThread().getName() + ": Acquired Google limiter. Analyzing image: " + url + " (Topic: " + topic + ")");
                                Map<String, String> analysisResult = helper.analyzeImage(url); // This is still blocking for this specific worker thread
                                System.out.println(Thread.currentThread().getName() + ": Finished analyzing image: " + url + " (Topic: " + topic + ")");
                                return analysisResult;
                            } catch (Exception err) {
                                System.err.println(Thread.currentThread().getName() + ": Error analyzing image " + url + " for topic " + topic + ": " + err.getMessage());
                                // Returning a specific error map for this image, so the main CompletableFuture doesn't fail
                                // If you want the whole submit() call to fail if *any* image analysis fails, re-throw a RuntimeException here.
                                return Map.of("image", url, "status", "analysis_failed", "error", err.getMessage());
                            } finally {
                                this.googleLimiter.release();
                                System.out.println(Thread.currentThread().getName() + ": Released Google limiter for URL: " + url + " (Topic: " + topic + ")");
                            }
                        } else {
                            System.out.println(Thread.currentThread().getName() + ": Google limiter blocked image analysis for URL: " + url + " (Topic: " + topic + ")");
                            // Return a map indicating it was skipped
                            return Map.of("image", url, "status", "skipped_google_limit");
                        }
                    }, executorSrv)) // Each analyzeImage call runs on a thread from executorSrv
                    .collect(Collectors.toList());

                // Wait for all image analysis futures to complete
                CompletableFuture<Void> allOfAnalysis = CompletableFuture.allOf(
                    imageAnalysisFutures.toArray(new CompletableFuture[0]) // Convert list to array
                );

                // Now, when allOfAnalysis completes, collect the results
                List<Map<String, String>> finalResponse = allOfAnalysis.thenApply(v ->
                    imageAnalysisFutures.stream()
                        .map(CompletableFuture::join) // join() gets the result, throws unchecked exception if future failed
                        .collect(Collectors.toList())
                ).join(); // Use join() to block the current thread until all results are collected.
                          // This .join() is happening on an executorSrv thread, NOT the HTTP request thread.

                // --- END REWRITTEN FOR LOOP ---

                return finalResponse;

            } catch (IllegalStateException e) {
                // Catch specific limiter exceptions so they don't get caught by the general Exception
                throw e; // Re-throw to propagate to exceptionally
            } catch (Exception e) {
                System.err.println(Thread.currentThread().getName() + ": General error during processing for topic: " + topic + ": " + e.getMessage());
                throw new RuntimeException("Error in task for topic: " + topic, e); // Wrap and re-throw
            } finally {
                this.taskLimiter.release();
                System.out.println(Thread.currentThread().getName() + ": Released task limiter for topic: " + topic);
            }
        }, executorSrv);
    }
}