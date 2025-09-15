package com.nnfs.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class ExtraEndpointsController {

   
    @PostMapping("/train")
    public ResponseEntity<Map<String, String>> train() {
        return ResponseEntity.ok(Map.of("status", "started"));
    }

   @GetMapping("/activations/stream")
public SseEmitter activationsStream() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
        try {
            while (true) {
                // Build an array of 64 random activation values
                List<Double> values = new ArrayList<>();
                for (int i = 0; i < 64; i++) {
                    values.add(Math.random());
                }
                Map<String, Object> data = new HashMap<>();
                data.put("values", values);
                data.put("activity", Math.random()); // random sparsity/activation
                emitter.send(data);
                Thread.sleep(1000); // send every second
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    executor.shutdown();
    return emitter;
}

@GetMapping("/layers/stream")
public SseEmitter layersStream() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
        try {
            while (true) {
                // Send only an activity value; the frontend will animate the graph
                Map<String, Object> data = new HashMap<>();
                data.put("activity", Math.random());
                emitter.send(data);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    executor.shutdown();
    return emitter;
}

}
