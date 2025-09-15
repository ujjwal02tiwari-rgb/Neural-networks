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

    // Endpoint to start training (dummy implementation)
    @PostMapping("/train")
    public ResponseEntity<Map<String, String>> train() {
        return ResponseEntity.ok(Map.of("status", "started"));
    }

    // SSE endpoint for activations stream (dummy data)
    @GetMapping("/activations/stream")
    public SseEmitter activationsStream() {
        SseEmitter emitter = new SseEmitter();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    // Simulate activation data as random percentages
                    emitter.send(Map.of(
                            "layer", i,
                            "active", Math.random(),
                            "sparsity", Math.random()
                    ));
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        executor.shutdown();
        return emitter;
    }

    // SSE endpoint for layers stream (dummy data)
    @GetMapping("/layers/stream")
    public SseEmitter layersStream() {
        SseEmitter emitter = new SseEmitter();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    // Simulate layer graph data as random values
                    emitter.send(Map.of(
                            "layer", i,
                            "nodes", (int)(Math.random() * 10 + 1)
                    ));
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        executor.shutdown();
        return emitter;
    }
}
