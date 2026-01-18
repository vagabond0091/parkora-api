package com.parkora.api.controller;

import com.parkora.api.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> hello() {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .message("Request successful")
                .errorCode(200)
                .status("SUCCESS")
                .data("Hello, World!")
                .build();

        return ResponseEntity.ok(response);
    }
}
