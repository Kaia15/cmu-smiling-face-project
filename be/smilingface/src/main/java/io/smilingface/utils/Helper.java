package io.smilingface.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;

@Component
public class Helper {

    public final String baseWikiUrl = "";
    public final int MAX_RETRIES = 3;
    private static final List<String> ALLOWED_IMAGE_SUFFIXES = Arrays.asList("jpeg", "jpg", "png", "webp");

    /* Implement retry mechanism to re-connect Wikipedia & GCP in 2 attempts
    */
    public <T> T retry(Callable<T> task) {
        int times = 0;
        while (times < MAX_RETRIES) {
            try {
                return task.call();
            } catch (Exception e) {
                times ++;
                if (times >= MAX_RETRIES) {
                    throw new RuntimeException(e);
                }
                try {
                    // Do the after-retry following before-retry in the duration of 1000(ms)
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry wait", ie);
                }
            }
        }
        throw new IllegalStateException("This code should never be reached.");
    }

    private JsonObject readJsonFromUrl(String urlString) throws IOException {
        return retry(() -> {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);

            try (InputStream input = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                return JsonParser.parseString(response.toString()).getAsJsonObject();
            }
        });
    }

    public List<String> fetchImage(String topic) {
        List<String> imageUrls = new ArrayList<>();
        String articleTitle = null;

        try {
            // Search for the Wikipedia article related to the topic
            String encodedTopic = URLEncoder.encode(topic, "UTF-8");
            String searchApiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" + encodedTopic + "&format=json";

            JsonObject searchJson = readJsonFromUrl(searchApiUrl);
            JsonArray searchResults = searchJson.getAsJsonObject("query").getAsJsonArray("search");

            if (searchResults.size() > 0) {
                // Get the title of the first search result (most relevant)
                articleTitle = searchResults.get(0).getAsJsonObject().get("title").getAsString();
            } 

            // Get image filenames from the identified Wikipedia article
            String imagesApiUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=" + URLEncoder.encode(articleTitle, "UTF-8")
                                + "&prop=images&format=json";

            JsonObject imagesJson = readJsonFromUrl(imagesApiUrl);
            JsonObject pages = imagesJson.getAsJsonObject("query").getAsJsonObject("pages");

            for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
                JsonObject page = entry.getValue().getAsJsonObject();
                JsonArray images = page.getAsJsonArray("images"); // This array holds image filenames

                if (images != null) {
                    for (JsonElement imageElement : images) {
                        String imageTitle = imageElement.getAsJsonObject().get("title").getAsString();
                    
                        // Get image info (URL) from Wikimedia Commons
                        // Ensure to use commons.wikimedia.org for imageinfo to get the direct URL
                        String imageInfoUrl = "https://commons.wikimedia.org/w/api.php?action=query&titles="
                                            + URLEncoder.encode(imageTitle, "UTF-8")
                                            + "&prop=imageinfo&iiprop=url&format=json";

                        JsonObject imageInfoJson = readJsonFromUrl(imageInfoUrl);
                        JsonObject imagePages = imageInfoJson.getAsJsonObject("query").getAsJsonObject("pages");

                        for (Map.Entry<String, JsonElement> imagePageEntry : imagePages.entrySet()) {
                            JsonObject imagePage = imagePageEntry.getValue().getAsJsonObject();
                            JsonArray imageinfo = imagePage.getAsJsonArray("imageinfo");

                            if (imageinfo != null && imageinfo.size() > 0) {
                                String imageUrl = imageinfo.get(0).getAsJsonObject().get("url").getAsString();

                                String fileExtension = "";
                                int dotIndex = imageUrl.lastIndexOf('.');
                                if (dotIndex > 0 && dotIndex < imageUrl.length() - 1) {
                                    fileExtension = imageUrl.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
                                }

                                if (ALLOWED_IMAGE_SUFFIXES.contains(fileExtension)) {
                                    imageUrls.add(imageUrl);
                                    
                                } 
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUrls;
    }

    public List<Map<String, String>> analyzeImage(String filePath) throws IOException {
        // TO-DO: send image to Google Cloud Vision to process
        List<Map<String, String>> results = new ArrayList<>();
        List<AnnotateImageRequest> requests = new ArrayList<>();
        String fixedUrl = filePath.replace("\\", "/");
        System.out.println(fixedUrl);
        
        InputStream input = new URL(fixedUrl).openStream();

        ByteString imgBytes = ByteString.readFrom(input);

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
        AnnotateImageRequest request
                = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    continue;
                }

                Map<String, String> faceData = new HashMap<>();
                faceData.put("image", fixedUrl);
                // For full list of available annotations, see http://g.co/cloud/vision/docs
                for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
                    
                    
                    faceData.put("anger", annotation.getAngerLikelihood().name());
                    faceData.put("joy", annotation.getJoyLikelihood().name());
                    faceData.put("surprise", annotation.getSurpriseLikelihood().name());
                    faceData.put("boundingPoly", annotation.getBoundingPoly().toString());
                    results.add(faceData);
                }
                
            }
        }
        return results;
    }
}
