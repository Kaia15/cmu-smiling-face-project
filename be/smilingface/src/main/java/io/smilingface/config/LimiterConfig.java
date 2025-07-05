package io.smilingface.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; // Import your Limiter class

import io.smilingface.utils.ILimiter;
import io.smilingface.utils.Limiter;

@Configuration
public class LimiterConfig {
    @Bean
    public ILimiter taskLimiter() {
        // This limiter controls how many overall "jobs" or "topics" can be processed concurrently.
        return new Limiter(5); 
    }

    @Bean
    public ILimiter wikiLimiter() {
        return new Limiter(5); 
    }

    @Bean
    public ILimiter googleLimiter() {
        // This limiter controls the concurrency of *batch* requests to Google Cloud Vision.
        // If GCP Vision allows 5 concurrent batch calls, set this to 5.
        // Each batch can contain multiple images (defined by GOOGLE_BATCH_SIZE in Executor).
        return new Limiter(5); 
    }
}