package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Lấy attribute từ Google
        String email     = (String) oAuth2User.getAttributes().get("email");
        String firstName = (String) oAuth2User.getAttributes().get("given_name");
        String lastName  = (String) oAuth2User.getAttributes().get("family_name");

        // fallback nếu Google không trả given_name / family_name
        if ((firstName == null || firstName.isBlank())
                && (lastName == null || lastName.isBlank())) {
            String fullName = (String) oAuth2User.getAttributes().get("name");
            if (fullName != null && !fullName.isBlank()) {
                String[] parts = fullName.trim().split("\\s+");
                if (parts.length == 1) {
                    firstName = parts[0];
                    lastName = "";
                } else {
                    lastName = parts[0];
                    firstName = String.join(" ",
                            java.util.Arrays.copyOfRange(parts, 1, parts.length));
                }
            }
        }

        // 🔥 AUTO REGISTER / UPDATE
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setRole(UserRole.USER);
                    // có thể set role mặc định, status ở đây
                    return u;
                });

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProvider("GOOGLE");
        // nếu bạn có field password thì có thể để null / "" cho account OAuth2
        userRepository.save(user);

        // Generate JWT
        String token = jwtUtil.generateToken(user);

        // Trả token về cho FE
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"token\":\"" + token + "\"}");
    }
}
