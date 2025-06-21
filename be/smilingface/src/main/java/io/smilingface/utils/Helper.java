package io.smilingface.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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

public class Helper {

    public final String baseWikiUrl = "";

    private JsonObject readJsonFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return JsonParser.parseString(response.toString()).getAsJsonObject();
        }
    }

    public List<String> fetchImage(String topic) {
        List<String> imageUrls = new ArrayList<>();
        try {
            // Step 1: Get images from the Wikipedia topic page
            String encodedTopic = URLEncoder.encode(topic, "UTF-8");
            String apiUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=" + encodedTopic
                    + "&prop=images&format=json&imlimit=20";

            JsonObject json = readJsonFromUrl(apiUrl);
            JsonObject pages = json.getAsJsonObject("query").getAsJsonObject("pages");

            for (String key : pages.keySet()) {
                JsonArray images = pages.getAsJsonObject(key).getAsJsonArray("images");
                if (images == null) {
                    continue;
                }

                for (JsonElement imageElement : images) {
                    String imageTitle = imageElement.getAsJsonObject().get("title").getAsString();
                    if (!imageTitle.toLowerCase().endsWith(".jpg") && !imageTitle.toLowerCase().endsWith(".jpeg")) {
                        continue; // Only get JPEG images
                    }

                    // Step 2: Get actual image URL
                    String imageInfoUrl = "https://en.wikipedia.org/w/api.php?action=query&titles="
                            + URLEncoder.encode(imageTitle, "UTF-8")
                            + "&prop=imageinfo&iiprop=url&format=json";

                    JsonObject imageInfoJson = readJsonFromUrl(imageInfoUrl);
                    JsonObject imagePages = imageInfoJson.getAsJsonObject("query").getAsJsonObject("pages");

                    for (String imageKey : imagePages.keySet()) {
                        JsonArray imageinfo = imagePages.getAsJsonObject(imageKey).getAsJsonArray("imageinfo");
                        if (imageinfo != null && imageinfo.size() > 0) {
                            String imageUrl = imageinfo.get(0).getAsJsonObject().get("url").getAsString();
                            imageUrls.add(imageUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUrls;
    }

    public void analyzeImage(String filePath) throws IOException {
        // TO-DO: send image to Google Cloud Vision to process
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
        AnnotateImageRequest request
                = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
                    System.out.format(
                            "anger: %s%njoy: %s%nsurprise: %s%nposition: %s",
                            annotation.getAngerLikelihood(),
                            annotation.getJoyLikelihood(),
                            annotation.getSurpriseLikelihood(),
                            annotation.getBoundingPoly());
                }
            }
        }
    }
}
