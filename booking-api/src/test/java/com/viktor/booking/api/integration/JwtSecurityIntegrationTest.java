package com.viktor.booking.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viktor.booking.api.BookingApiApplication;
import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.application.security.PasswordHasher;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BookingApiApplication.class,
        properties = {
                "security.jwt.secret="
                        + "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "security.jwt.expiration-ms=3600000"
        }
)
@AutoConfigureMockMvc
@Testcontainers
class JwtSecurityIntegrationTest {

    private static final String USER_EMAIL =
            "integration-user@example.com";

    private static final String USER_PASSWORD =
            "UserPassword123!";

    private static final String ADMIN_EMAIL =
            "integration-admin@example.com";

    private static final String ADMIN_PASSWORD =
            "AdminPassword123!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void shouldCompleteFullJwtSecurityFlow()
            throws Exception {

        String userToken =
                registerUserAndExtractToken();

        verifyAnonymousCannotAccessBookings();

        verifyUserCanAccessBookings(userToken);

        verifyUserCannotCreateService(userToken);

        createAdmin();

        String adminToken =
                loginAndExtractToken(
                        ADMIN_EMAIL,
                        ADMIN_PASSWORD
                );

        verifyAdminCanCreateService(adminToken);
    }

    private String registerUserAndExtractToken()
            throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                USER_EMAIL,
                                "password",
                                USER_PASSWORD
                        )
                );

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/register")
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_JSON
                                        )
                                        .content(requestBody)
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath("$.accessToken")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.tokenType")
                                        .value("Bearer")
                        )
                        .andExpect(
                                jsonPath("$.user.email")
                                        .value(USER_EMAIL)
                        )
                        .andExpect(
                                jsonPath("$.user.role")
                                        .value("USER")
                        )
                        .andReturn();

        return extractAccessToken(result);
    }

    private void verifyAnonymousCannotAccessBookings()
            throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    private void verifyUserCanAccessBookings(
            String userToken
    ) throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    private void verifyUserCannotCreateService(
            String userToken
    ) throws Exception {

        String requestBody =
                createServiceRequestBody(
                        "User forbidden service"
                );

        mockMvc.perform(
                        post("/api/services")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    private void createAdmin() {
        User admin = new User(
                null,
                ADMIN_EMAIL,
                passwordHasher.hash(
                        ADMIN_PASSWORD
                ),
                UserRole.ADMIN
        );

        userRepository.save(admin);
    }

    private String loginAndExtractToken(
            String email,
            String password
    ) throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                email,
                                "password",
                                password
                        )
                );

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_JSON
                                        )
                                        .content(requestBody)
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.accessToken")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.tokenType")
                                        .value("Bearer")
                        )
                        .andExpect(
                                jsonPath("$.user.email")
                                        .value(email)
                        )
                        .andExpect(
                                jsonPath("$.user.role")
                                        .value("ADMIN")
                        )
                        .andReturn();

        return extractAccessToken(result);
    }

    private void verifyAdminCanCreateService(
            String adminToken
    ) throws Exception {

        String requestBody =
                createServiceRequestBody(
                        "Admin integration service"
                );

        mockMvc.perform(
                        post("/api/services")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Admin integration service"
                                )
                );
    }

    private String createServiceRequestBody(
            String name
    ) throws Exception {

        return objectMapper.writeValueAsString(
                Map.of(
                        "name",
                        name,
                        "description",
                        "Created by JWT integration test",
                        "durationMinutes",
                        60
                )
        );
    }

    private String extractAccessToken(
            MvcResult result
    ) throws Exception {

        String responseBody =
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8
                        );

        JsonNode responseJson =
                objectMapper.readTree(responseBody);

        return responseJson
                .get("accessToken")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
