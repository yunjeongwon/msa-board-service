package com.example.boardservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CpuTestController {
    @GetMapping("/api/points/cpu-test")
    public String cpuTest() {
        long end = System.currentTimeMillis() + 5000;

        double sum = 0;
        while (System.currentTimeMillis() < end) {
            sum += Math.sqrt(System.nanoTime());
        }
        return "ok";
    }
}
