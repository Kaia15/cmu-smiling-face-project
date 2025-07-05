package io.smilingface.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private static int count = 0;

    @GetMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}

	@Autowired
	public ImageController(IImageSrv imageSrv) {
		this.imageSrv = imageSrv;
	}

	@PostMapping
    public CompletableFuture<ResponseEntity<List<Map<String, String>>>> processImage(@RequestParam String topic) {
        count++;
        System.out.println("Received request for topic: " + topic + ". Total requests initiated: " + count);

        return this.imageSrv.imageProcess(topic)
            .handle((result, ex) -> { 
                if (ex != null) {
                    
                    System.err.println("Error during image processing for topic: " + topic + ": " + ex.getMessage());
                    
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR) 
                            .body(new ArrayList<>()); 
                                                      
                } else {
                    // Handle successful result:
                    System.out.println("Completed processing for topic: " + topic);
                    return ResponseEntity.ok(result);
                }
            });
    }
}
