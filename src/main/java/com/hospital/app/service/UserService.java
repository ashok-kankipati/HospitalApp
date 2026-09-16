package com.hospital.app.service;

import com.hospital.app.dto.LoginRequest;
import com.hospital.app.dto.LoginResponse;
import com.hospital.app.model.User;
import com.hospital.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Authenticate user with username and password
     */
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        LoginResponse response = new LoginResponse();

        try {
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());

            if (userOpt.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("User not found");
                return response;
            }

            User user = userOpt.get();

            // Check if user is active
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                response.setSuccess(false);
                response.setMessage("User account is inactive");
                return response;
            }

            if (!AccountPasswords.matches(loginRequest.getPassword(), user.getPassword())) {
                response.setSuccess(false);
                response.setMessage("Invalid password");
                return response;
            }

            // Upgrade existing plaintext accounts after their next successful login.
            if (!AccountPasswords.isEncoded(user.getPassword())) {
                user.setPassword(AccountPasswords.encode(loginRequest.getPassword()));
                userRepository.save(user);
            }

            response.setSuccess(true);
            response.setMessage("Login successful");
            response.setUsername(user.getUsername());
            response.setRole(user.getRole());
            response.setEmail(user.getEmail());

            return response;

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("An error occurred during login: " + e.getMessage());
            return response;
        }
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
