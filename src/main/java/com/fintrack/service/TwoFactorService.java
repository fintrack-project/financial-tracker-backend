package com.fintrack.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorService {

    private final GoogleAuthenticator googleAuthenticator;

    public TwoFactorService() {
        this.googleAuthenticator = new GoogleAuthenticator();
    }

    public GoogleAuthenticatorKey generateSecret() {
        return googleAuthenticator.createCredentials();
    }

    public String generateQRCode(String accountName, GoogleAuthenticatorKey credentials) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL("FinTrack", accountName, credentials);
    }

    public boolean verifyOTP(String secret, int otp) {
        return googleAuthenticator.authorize(secret, otp);
    }
}