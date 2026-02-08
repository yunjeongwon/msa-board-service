package com.example.boardservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CpuTestController {
    @GetMapping("/api/points/cpu-test")
    public String cpuTest() {
        long end = System.currentTimeMillis() + 500;

        double sum = 0;
        while (System.currentTimeMillis() < end) {
            sum += Math.random();
        }
        return "ok";
    }
}
