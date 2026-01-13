package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest){
        String result = userService.register(registerRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest){
        String result =  userService.login(loginRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello secured API";
    }

    @GetMapping("/admin/secret")
    public String adminOnly() {
        return "Admin only area!";
    }

    @GetMapping("/login/oauth2")
    public void loginOauth2(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/hi")
    public String hi() {
        return "Hi!";
    }
}
