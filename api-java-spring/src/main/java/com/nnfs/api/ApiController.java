package com.nnfs.api;

import com.nnfs.nn.Model;
import com.nnfs.math.MatrixOps;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class ApiController {
    Model model;

    public ApiController() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/model/weights.json")) {
            if (is == null) {
                this.model = new Model(2, 64, 3, 1337L);
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            this.model = WeightsLoader.load(json);
        }
    }

    @GetMapping("/health")
    public Map<String, String> health(){
        return Map.of("status", "ok");
    }

    public record PredictRequest(double[][] x){}
    public record PredictResponse(double[][] probs, int[] argmax){}

    @PostMapping("/predict")
    public PredictResponse predict(@RequestBody PredictRequest req){
        double[][] probs = model.forward(req.x());
        int[] arg = MatrixOps.argmax(probs);
        return new PredictResponse(probs, arg);
    }
}