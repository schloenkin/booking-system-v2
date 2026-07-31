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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private static final String USER_ONE_EMAIL =
            "integration-user-one@example.com";

    private static final String USER_ONE_PASSWORD =
            "UserOnePassword123!";

    private static final String USER_TWO_EMAIL =
            "integration-user-two@example.com";

    private static final String USER_TWO_PASSWORD =
            "UserTwoPassword123!";

    private static final String ADMIN_EMAIL =
            "integration-admin@example.com";

    private static final String ADMIN_PASSWORD =
            "AdminPassword123!";

    private static final String LOGIN_USER_EMAIL =
            "integration-login-user@example.com";

    private static final String LOGIN_USER_PASSWORD =
            "LoginUserPassword123!";

    private static final String INVALID_PASSWORD_USER_EMAIL =
            "integration-invalid-password@example.com";

    private static final String INVALID_PASSWORD_USER_PASSWORD =
            "ValidPassword123!";

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
    void shouldLoginRegisteredUser()
            throws Exception {

        registerUserAndExtractIdentity(
                LOGIN_USER_EMAIL,
                LOGIN_USER_PASSWORD
        );

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                LOGIN_USER_EMAIL,
                                "password",
                                LOGIN_USER_PASSWORD
                        )
                );

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
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
                                        .value(LOGIN_USER_EMAIL)
                        )
                        .andExpect(
                                jsonPath("$.user.role")
                                        .value("USER")
                        )
                        .andReturn();

        String accessToken =
                extractAccessToken(result);

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldReturn401ForIncorrectPassword()
            throws Exception {

        registerUserAndExtractIdentity(
                INVALID_PASSWORD_USER_EMAIL,
                INVALID_PASSWORD_USER_PASSWORD
        );

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                INVALID_PASSWORD_USER_EMAIL,
                                "password",
                                "IncorrectPassword123!"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturn401ForUnknownEmail()
            throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                "unknown-integration-user@example.com",
                                "password",
                                "UnknownPassword123!"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturn401ForMalformedJwt()
            throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer definitely-not-a-valid-jwt"
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldCompleteFullJwtSecurityAndBookingOwnershipFlow()
            throws Exception {

        RegisteredUser userOne =
                registerUserAndExtractIdentity(
                        USER_ONE_EMAIL,
                        USER_ONE_PASSWORD
                );

        RegisteredUser userTwo =
                registerUserAndExtractIdentity(
                        USER_TWO_EMAIL,
                        USER_TWO_PASSWORD
                );

        verifyAnonymousCannotAccessBookings();

        verifyUserCanAccessBookings(
                userOne.accessToken()
        );

        verifyUserCannotCreateService(
                userOne.accessToken()
        );

        createAdmin();

        String adminToken =
                loginAdminAndExtractToken();

        Long serviceId =
                createServiceAndExtractId(
                        adminToken
                );

        Long userOneBookingId =
                createBookingWithForgedUserId(
                        userOne.accessToken(),
                        userTwo.userId(),
                        userOne.userId(),
                        serviceId,
                        "2030-01-15T10:00:00",
                        "2030-01-15T11:00:00"
                );

        Long userTwoBookingId =
                createBookingWithForgedUserId(
                        userTwo.accessToken(),
                        userOne.userId(),
                        userTwo.userId(),
                        serviceId,
                        "2030-01-15T12:00:00",
                        "2030-01-15T13:00:00"
                );

        verifyUserSearchIsRestrictedToOwnBookings(
                userOne,
                userTwo.userId(),
                userOneBookingId
        );

        verifyUserCannotAccessAnotherUsersBooking(
                userOne.accessToken(),
                userTwoBookingId
        );

        verifyBookingRemainsPendingForOwner(
                userTwo,
                userTwoBookingId
        );

        verifyAdminCanSeeAllBookings(
                adminToken,
                userOne,
                userTwo,
                userOneBookingId,
                userTwoBookingId
        );

        verifyAdminCanAccessAndConfirmAnyBooking(
                adminToken,
                userTwo,
                userTwoBookingId
        );

        verifyRemovedUserCreationEndpointReturns404(
                adminToken
        );
    }

    private RegisteredUser registerUserAndExtractIdentity(
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
                                jsonPath("$.user.id")
                                        .isNumber()
                        )
                        .andExpect(
                                jsonPath("$.user.email")
                                        .value(email)
                        )
                        .andExpect(
                                jsonPath("$.user.role")
                                        .value("USER")
                        )
                        .andReturn();

        JsonNode responseJson =
                readResponseJson(result);

        Long userId =
                responseJson
                        .get("user")
                        .get("id")
                        .asLong();

        String accessToken =
                responseJson
                        .get("accessToken")
                        .asText();

        return new RegisteredUser(
                userId,
                accessToken
        );
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

    private String loginAdminAndExtractToken()
            throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                ADMIN_EMAIL,
                                "password",
                                ADMIN_PASSWORD
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
                                        .value(ADMIN_EMAIL)
                        )
                        .andExpect(
                                jsonPath("$.user.role")
                                        .value("ADMIN")
                        )
                        .andReturn();

        return extractAccessToken(result);
    }

    private Long createServiceAndExtractId(
            String adminToken
    ) throws Exception {

        String requestBody =
                createServiceRequestBody(
                        "Ownership integration service"
                );

        MvcResult result =
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
                                jsonPath("$.id")
                                        .isNumber()
                        )
                        .andExpect(
                                jsonPath("$.name")
                                        .value(
                                                "Ownership integration service"
                                        )
                        )
                        .andExpect(
                                jsonPath("$.durationMinutes")
                                        .value(60)
                        )
                        .andExpect(
                                jsonPath("$.active")
                                        .value(true)
                        )
                        .andReturn();

        JsonNode responseJson =
                readResponseJson(result);

        return responseJson
                .get("id")
                .asLong();
    }

    private Long createBookingWithForgedUserId(
            String accessToken,
            Long forgedUserId,
            Long expectedOwnerId,
            Long serviceId,
            String startTime,
            String endTime
    ) throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "userId",
                                forgedUserId,
                                "serviceId",
                                serviceId,
                                "startTime",
                                startTime,
                                "endTime",
                                endTime
                        )
                );

        MvcResult result =
                mockMvc.perform(
                                post("/api/bookings")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearer(accessToken)
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
                                jsonPath("$.id")
                                        .isNumber()
                        )
                        .andExpect(
                                jsonPath("$.userId")
                                        .value(expectedOwnerId)
                        )
                        .andExpect(
                                jsonPath("$.serviceId")
                                        .value(serviceId)
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value("PENDING")
                        )
                        .andReturn();

        JsonNode responseJson =
                readResponseJson(result);

        return responseJson
                .get("id")
                .asLong();
    }

    private void verifyUserSearchIsRestrictedToOwnBookings(
            RegisteredUser user,
            Long requestedOtherUserId,
            Long expectedBookingId
    ) throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                user.accessToken()
                                        )
                                )
                                .param(
                                        "userId",
                                        requestedOtherUserId.toString()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(expectedBookingId)
                )
                .andExpect(
                        jsonPath("$.content[0].userId")
                                .value(user.userId())
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    private void verifyUserCannotAccessAnotherUsersBooking(
            String userToken,
            Long anotherUsersBookingId
    ) throws Exception {

        mockMvc.perform(
                        get(
                                "/api/bookings/{id}",
                                anotherUsersBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
                                )
                )
                .andExpect(
                        status().isNotFound()
                );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/cancel",
                                anotherUsersBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
                                )
                )
                .andExpect(
                        status().isNotFound()
                );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/confirm",
                                anotherUsersBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
                                )
                )
                .andExpect(
                        status().isNotFound()
                );

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                anotherUsersBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
                                )
                )
                .andExpect(
                        status().isNotFound()
                );
    }

    private void verifyBookingRemainsPendingForOwner(
            RegisteredUser owner,
            Long bookingId
    ) throws Exception {

        mockMvc.perform(
                        get(
                                "/api/bookings/{id}",
                                bookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                owner.accessToken()
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(bookingId)
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(owner.userId())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );
    }

    private void verifyAdminCanSeeAllBookings(
            String adminToken,
            RegisteredUser userOne,
            RegisteredUser userTwo,
            Long userOneBookingId,
            Long userTwoBookingId
    ) throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(userOneBookingId)
                )
                .andExpect(
                        jsonPath("$.content[0].userId")
                                .value(userOne.userId())
                )
                .andExpect(
                        jsonPath("$.content[1].id")
                                .value(userTwoBookingId)
                )
                .andExpect(
                        jsonPath("$.content[1].userId")
                                .value(userTwo.userId())
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(2)
                );
    }

    private void verifyAdminCanAccessAndConfirmAnyBooking(
            String adminToken,
            RegisteredUser owner,
            Long bookingId
    ) throws Exception {

        mockMvc.perform(
                        get(
                                "/api/bookings/{id}",
                                bookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(bookingId)
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(owner.userId())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/confirm",
                                bookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(bookingId)
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(owner.userId())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CONFIRMED")
                );
    }

    private void verifyRemovedUserCreationEndpointReturns404(
            String adminToken
    ) throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                "removed-endpoint@example.com",
                                "password",
                                "Password123!"
                        )
                );

        mockMvc.perform(
                        post("/api/users")
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
                        status().isNotFound()
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

        return readResponseJson(result)
                .get("accessToken")
                .asText();
    }

    private JsonNode readResponseJson(
            MvcResult result
    ) throws Exception {

        String responseBody =
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8
                        );

        return objectMapper.readTree(responseBody);
    }

    private String bearer(
            String token
    ) {
        return "Bearer " + token;
    }

    private record RegisteredUser(
            Long userId,
            String accessToken
    ) {
    }
}