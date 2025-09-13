package com.nnfs.api;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class TrainWsController {

   
    @MessageMapping("/train")
    
    @SendTo("/topic/metrics")
    public Map<String, Object> trainMetrics(String msg) throws Exception {
       
        return Map.of(
            "epoch", (int)(Math.random() * 10),
            "loss", Math.random(),
            "accuracy", Math.random()
        );
    }
}
