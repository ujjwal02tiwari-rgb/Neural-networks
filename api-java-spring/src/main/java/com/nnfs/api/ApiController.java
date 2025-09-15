package com.nnfs.api;

import com.nnfs.nn.Model;
import com.nnfs.math.MatrixOps;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class ApiController {
    private final Model model;

    public ApiController() {
        Model tempModel;
        try (InputStream is = getClass().getResourceAsStream("/model/weights.json")) {
            if (is == null) {
                // No weights file found → fallback to default model
                tempModel = new Model(2, 64, 3, 1337L);
            } else {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                try {
                    tempModel = WeightsLoader.load(json);
                } catch (Exception e) {
                    // Defensive fallback in case of corrupt weights.json
                    e.printStackTrace();
                    tempModel = new Model(2, 64, 3, 1337L);
                }
            }
        } catch (IOException e) {
            // Defensive fallback in case of I/O error
            e.printStackTrace();
            tempModel = new Model(2, 64, 3, 1337L);
        }
        this.model = tempModel;
    }

    // --- Health check ---
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // --- Inference (alias for predict) ---
    public record PredictRequest(double[][] x) {}
    public record PredictResponse(double[][] probs, int[] argmax) {}

    @PostMapping("/predict")
    public PredictResponse predict(@RequestBody PredictRequest req) {
        double[][] probs = model.forward(req.x());
        int[] arg = MatrixOps.argmax(probs);
        return new PredictResponse(probs, arg);
    }

    @PostMapping("/inference")
    public PredictResponse inference(@RequestBody PredictRequest req) {
        return predict(req); // reuse predict
    }

    // --- Training Stream via SSE ---
    @GetMapping("/train/stream")
    public SseEmitter trainStream() {
        SseEmitter emitter = new SseEmitter();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                for (int epoch = 1; epoch <= 5; epoch++) {
                    // Simulate training metrics
                    double loss = Math.random();
                    double accuracy = Math.random();

                    emitter.send(Map.of(
                        "epoch", epoch,
                        "loss", loss,
                        "accuracy", accuracy
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
