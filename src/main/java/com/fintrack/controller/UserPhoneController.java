package com.fintrack.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user/phone")
public class UserPhoneController {

    @PostMapping("/send-verification")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");

        try {
            // Firebase handles sending the SMS verification code
            FirebaseAuth.getInstance().createSessionCookie(phoneNumber);
            return ResponseEntity.ok().build();
        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyCode(@RequestBody Map<String, String> request) {
        String verificationId = request.get("verificationId");
        String code = request.get("code");
    
        try {
            // Verify the code using Firebase
            FirebaseAuth.getInstance().verifyIdToken(code);
            return ResponseEntity.ok(true);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.ok(false);
        }
    }
}