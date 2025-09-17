package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class InferenceController {

    // POST /predict
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predict(@RequestParam("file") MultipartFile file) {
        try {
            // Read uploaded image (digit.png from frontend)
            BufferedImage img = ImageIO.read(file.getInputStream());

            if (img == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid image"));
            }

            // Resize to 28x28 grayscale (MNIST format)
            BufferedImage resized = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = resized.createGraphics();
            g.drawImage(img, 0, 0, 28, 28, null);
            g.dispose();

            // Convert image to input vector [0..1]
            double[] input = new double[28 * 28];
            for (int y = 0; y < 28; y++) {
                for (int x = 0; x < 28; x++) {
                    int rgb = resized.getRGB(x, y);
                    int gray = rgb & 0xFF;
                    input[y * 28 + x] = gray / 255.0;
                }
            }

            // 🔥 Call your trained neural net here
            // Example: Model model = MyModelLoader.load();
            // PredictionResult result = model.predict(input);

            // Temporary mock result for now:
            int predictedLabel = 5;
            double score = 0.987;

            Map<String, Object> response = new HashMap<>();
            response.put("label", predictedLabel);
            response.put("score", score);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process image"));
        }
    }
}
