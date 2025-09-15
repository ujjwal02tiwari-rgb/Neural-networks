package com.nnfs.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class ExtraEndpointsController {

    // Simple endpoint to kick off training. Adjust as needed.
    @PostMapping("/train")
    public ResponseEntity<Map<String, String>> train() {
        return ResponseEntity.ok(Map.of("status", "started"));
    }

    // SSE endpoint for the activation visualizer.
    // Sends a JSON object with a 64‑element array and an overall activity metric.
    @GetMapping("/activations/stream")
    public SseEmitter activationsStream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                while (true) {
                    // Generate 64 random activations between 0 and 1
                    List<Double> values = new ArrayList<>();
                    for (int i = 0; i < 64; i++) {
                        values.add(Math.random());
                    }
                    // Put values and an overall activity value into a map
                    Map<String, Object> data = new HashMap<>();
                    data.put("values", values);
                    data.put("activity", Math.random());
                    emitter.send(data);
                    Thread.sleep(1000); // 1‑second interval between updates
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        executor.shutdown();
        return emitter;
    }

    // SSE endpoint for the layer activity graph.
    // Sends a JSON object with a single "activity" value between 0 and 1.
    @GetMapping("/layers/stream")
    public SseEmitter layersStream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                while (true) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("activity", Math.random());
                    emitter.send(data);
                    Thread.sleep(1000); // 1‑second interval between updates
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        executor.shutdown();
        return emitter;
    }
}
