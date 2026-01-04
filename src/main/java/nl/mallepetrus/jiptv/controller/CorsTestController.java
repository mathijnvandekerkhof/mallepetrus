package nl.mallepetrus.jiptv.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cors-test")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class CorsTestController {

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of(
            "message", "CORS test successful",
            "timestamp", System.currentTimeMillis(),
            "status", "OK"
        ));
    }

    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(Map.of(
            "message", "CORS POST test successful",
            "received", data,
            "timestamp", System.currentTimeMillis()
        ));
    }

    @RequestMapping(method = RequestMethod.OPTIONS, value = "/**")
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }
}