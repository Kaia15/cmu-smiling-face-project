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
                times++;
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

            try (InputStream input = conn.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

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

        try {
            int offset = 0;
            boolean hasMore = true;

            while (hasMore && imageUrls.size() < 100) { 
                String encodedTopic = URLEncoder.encode(topic, "UTF-8");

                String apiUrl = String.format(
                    "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
                    "&generator=search&gsrnamespace=6&gsrlimit=50" +
                    "&gsroffset=%d&gsrsearch=%s&prop=imageinfo&iiprop=url",
                    offset, encodedTopic
                );

                JsonObject json = readJsonFromUrl(apiUrl);
                JsonObject query = json.getAsJsonObject("query");

                if (query != null && query.has("pages")) {
                    JsonObject pages = query.getAsJsonObject("pages");

                    for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
                        JsonObject page = entry.getValue().getAsJsonObject();

                        if (page.has("imageinfo")) {
                            JsonArray imageinfo = page.getAsJsonArray("imageinfo");

                            if (imageinfo != null && imageinfo.size() > 0) {
                                String imageUrl = imageinfo.get(0).getAsJsonObject().get("url").getAsString();
                                //System.out.println(imageUrl);
                                imageUrls.add(imageUrl);
                            }
                        }
                    }
                }

                if (json.has("continue") && json.getAsJsonObject("continue").has("gsroffset")) {
                    offset = json.getAsJsonObject("continue").get("gsroffset").getAsInt();
                } else {
                    hasMore = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageUrls;
    }

    public Map<String, String> analyzeImage(String filePath) throws IOException {
    Map<String, String> faceData = new HashMap<>();
    List<AnnotateImageRequest> requests = new ArrayList<>();
    
    String fixedUrl = filePath.replace("\\", "/");
    InputStream input = new URL(fixedUrl).openStream();
    ByteString imgBytes = ByteString.readFrom(input);

    if (imgBytes.size() > 20_000_000) { 
        System.out.println("Error: Image size exceeds 20MB limit.");
        faceData.put("error", "Image size exceeds 20MB limit.");
        return faceData;
    }

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(feat)
            .setImage(img)
            .build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
        BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        for (AnnotateImageResponse res : responses) {
            if (res.hasError()) {
                System.out.format("Error: %s%n", res.getError().getMessage());
                faceData.put("error", res.getError().getMessage());
                return faceData;
            }

            if (!res.getFaceAnnotationsList().isEmpty()) {
                FaceAnnotation annotation = res.getFaceAnnotationsList().get(0);

                faceData.put("image", fixedUrl);
                faceData.put("anger", annotation.getAngerLikelihood().name());
                faceData.put("joy", annotation.getJoyLikelihood().name());
                faceData.put("surprise", annotation.getSurpriseLikelihood().name());
                faceData.put("boundingPoly", annotation.getBoundingPoly().toString());
            } else {
                faceData.put("image", fixedUrl);
                faceData.put("note", "No face detected.");
            }
        }
    }

    return faceData;
}
}
