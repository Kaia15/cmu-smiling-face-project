package io.smilingface.controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.smilingface.services.IImageSrv;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:3000")
public class ImageController {
	private final IImageSrv imageSrv;

    @GetMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}

	@Autowired
	public ImageController(IImageSrv imageSrv) {
		this.imageSrv = imageSrv;
	}

	@PostMapping
    public ResponseEntity<?> processImage(@RequestParam String topic) {
        try {
            Future<List<Map<String, String>>> futureResult = this.imageSrv.imageProcess(topic);
            List<Map<String, String>> result = futureResult.get(); // blocks until done
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during image processing: " + e.getMessage());
        }
    }
	
}
