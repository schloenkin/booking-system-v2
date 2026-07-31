package com.viktor.booking.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viktor.booking.api.BookingApiApplication;
import com.viktor.booking.application.repository.BookableServiceRepository;
import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.application.security.PasswordHasher;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.BookableService;
import com.viktor.booking.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

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

    private static final String OPERATION_USER_EMAIL =
            "integration-operation-user@example.com";

    private static final String OPERATION_USER_PASSWORD =
            "OperationUserPassword123!";

    private static final String APPLICATION_JWT_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static final String ALTERNATIVE_JWT_SECRET =
            "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=";

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

    @Autowired
    private BookableServiceRepository serviceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    bookings,
                    bookable_services,
                    users
                RESTART IDENTITY CASCADE
                """
        );
    }

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
    void shouldReturn401ForJwtWithInvalidSignature()
            throws Exception {

        Instant now = Instant.now();

        String token = createJwt(
                ALTERNATIVE_JWT_SECRET,
                "invalid-signature-user@example.com",
                now.minusSeconds(60),
                now.plusSeconds(3600)
        );

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturn401ForExpiredJwt()
            throws Exception {

        Instant now = Instant.now();

        String token = createJwt(
                APPLICATION_JWT_SECRET,
                "expired-token-user@example.com",
                now.minusSeconds(7200),
                now.minusSeconds(3600)
        );

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderDoesNotUseBearerScheme()
            throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Token some-token-value"
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

    @Test
    void shouldEnforceUserBookingOperationPermissions()
            throws Exception {

        RegisteredUser user =
                registerUserAndExtractIdentity(
                        OPERATION_USER_EMAIL,
                        OPERATION_USER_PASSWORD
                );

        BookableService savedService =
                serviceRepository.save(
                        new BookableService(
                                null,
                                "User operation integration service",
                                "Service for USER operation tests",
                                60,
                                true
                        )
                );

        Long cancellationBookingId =
                createBookingWithForgedUserId(
                        user.accessToken(),
                        user.userId(),
                        user.userId(),
                        savedService.getId(),
                        "2031-02-10T10:00:00",
                        "2031-02-10T11:00:00"
                );

        Long confirmationBookingId =
                createBookingWithForgedUserId(
                        user.accessToken(),
                        user.userId(),
                        user.userId(),
                        savedService.getId(),
                        "2031-02-10T12:00:00",
                        "2031-02-10T13:00:00"
                );

        Long deletionBookingId =
                createBookingWithForgedUserId(
                        user.accessToken(),
                        user.userId(),
                        user.userId(),
                        savedService.getId(),
                        "2031-02-10T14:00:00",
                        "2031-02-10T15:00:00"
                );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/cancel",
                                cancellationBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(user.accessToken())
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(cancellationBookingId)
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(user.userId())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/confirm",
                                confirmationBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(user.accessToken())
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verifyBookingStatus(
                user.accessToken(),
                confirmationBookingId,
                "PENDING"
        );

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                deletionBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(user.accessToken())
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verifyBookingStatus(
                user.accessToken(),
                deletionBookingId,
                "PENDING"
        );
    }

    @Test
    void shouldAllowAdminToCancelAndDeleteBookings()
            throws Exception {

        RegisteredUser user =
                registerUserAndExtractIdentity(
                        OPERATION_USER_EMAIL,
                        OPERATION_USER_PASSWORD
                );

        createAdmin();

        String adminToken =
                loginAdminAndExtractToken();

        Long serviceId =
                createServiceAndExtractId(
                        adminToken
                );

        Long cancellationBookingId =
                createBookingWithForgedUserId(
                        user.accessToken(),
                        user.userId(),
                        user.userId(),
                        serviceId,
                        "2031-03-10T10:00:00",
                        "2031-03-10T11:00:00"
                );

        Long deletionBookingId =
                createBookingWithForgedUserId(
                        user.accessToken(),
                        user.userId(),
                        user.userId(),
                        serviceId,
                        "2031-03-10T12:00:00",
                        "2031-03-10T13:00:00"
                );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/cancel",
                                cancellationBookingId
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
                                .value(cancellationBookingId)
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(user.userId())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/cancel",
                                deletionBookingId
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
                                .value(deletionBookingId)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                );

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                deletionBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        mockMvc.perform(
                        get(
                                "/api/bookings/{id}",
                                deletionBookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    void shouldReturnUnifiedSecurityErrorResponses()
            throws Exception {

        assertSecurityErrorContract(
                mockMvc.perform(
                        get("/api/bookings")
                ),
                401,
                "Unauthorized",
                "AUTHENTICATION_REQUIRED",
                "Authentication is required to access this resource",
                "/api/bookings"
        );

        assertSecurityErrorContract(
                mockMvc.perform(
                        get("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer malformed-token"
                                )
                ),
                401,
                "Unauthorized",
                "AUTHENTICATION_REQUIRED",
                "Authentication is required to access this resource",
                "/api/bookings"
        );

        RegisteredUser user =
                registerUserAndExtractIdentity(
                        USER_ONE_EMAIL,
                        USER_ONE_PASSWORD
                );

        String requestBody =
                createServiceRequestBody(
                        "Forbidden security contract service"
                );

        assertSecurityErrorContract(
                mockMvc.perform(
                        post("/api/services")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                user.accessToken()
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                ),
                403,
                "Forbidden",
                "ACCESS_DENIED",
                "Access is denied",
                "/api/services"
        );
    }

    @Test
    void shouldReturnUnifiedValidationError()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "validation@example.com"
                                        }
                                        """
                                )
                );

        assertApiErrorContract(
                result,
                400,
                "Bad Request",
                "VALIDATION_FAILED",
                "/api/auth/register"
        );

        result
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Request validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.violations.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.violations[0].field")
                                .value("password")
                )
                .andExpect(
                        jsonPath("$.violations[0].message")
                                .value(
                                        "Password must not be blank"
                                )
                );
    }

    @Test
    void shouldReturnUnifiedMalformedJsonError()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "broken@example.com"
                                        """
                                )
                );

        assertApiErrorContract(
                result,
                400,
                "Bad Request",
                "MALFORMED_REQUEST",
                "/api/auth/register"
        );

        result.andExpect(
                jsonPath("$.message")
                        .value(
                                "Request body contains malformed JSON"
                        )
        );
    }

    @Test
    void shouldReturnUnifiedErrorForMissingService()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                        get(
                                "/api/services/{id}",
                                999999L
                        )
                );

        assertApiErrorContract(
                result,
                404,
                "Not Found",
                "SERVICE_NOT_FOUND",
                "/api/services/999999"
        );
    }

    @Test
    void shouldReturnUnifiedErrorForDuplicateRegistration()
            throws Exception {

        String email =
                "duplicate-contract@example.com";

        String password =
                "DuplicatePassword123!";

        registerUserAndExtractIdentity(
                email,
                password
        );

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                email,
                                "password",
                                password
                        )
                );

        ResultActions result =
                mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                );

        assertApiErrorContract(
                result,
                409,
                "Conflict",
                "USER_ALREADY_EXISTS",
                "/api/auth/register"
        );
    }

    @Test
    void shouldExposeOpenApiDocumentationWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.openapi")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.info")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.paths")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.info.title")
                                .value("Booking System API")
                )
                .andExpect(
                        jsonPath("$.info.description")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.info.version")
                                .value("1.0.0")
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes.bearerAuth.type"
                        ).value("http")
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes.bearerAuth.scheme"
                        ).value("bearer")
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes.bearerAuth.bearerFormat"
                        ).value("JWT")
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/auth/register']['post']['summary']"
                        ).value("Register a new user")
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/auth/register']['post']['responses']['201']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/auth/register']['post']['responses']['400']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/auth/register']['post']['responses']['409']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/auth/login']['post']['summary']"
                        ).value("Authenticate an existing user")
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/auth/login']['post']['responses']['200']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/auth/login']['post']['responses']['401']"
                        ).exists()
                )
                .andExpect(
                        jsonPath("$.components.schemas.RegisterRequest")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.components.schemas.LoginRequest")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.components.schemas.AuthResponse")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.components.schemas.ErrorResponse")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.components.schemas.ValidationViolation")
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['get']['summary']"
                        ).value("List all bookable services")
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['get']['responses']['200']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['get']['security']"
                        ).doesNotExist()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['post']['summary']"
                        ).value("Create a bookable service")
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['post']['security'][0]['bearerAuth']"
                        ).isArray()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['post']['responses']['201']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['post']['responses']['400']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['post']['responses']['401']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services']['post']['responses']['403']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services/{id}']['get']['summary']"
                        ).value("Get a bookable service")
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services/{id}']['get']['responses']['404']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services/{id}/activate']['put']['security'][0]['bearerAuth']"
                        ).isArray()
                )
                .andExpect(
                        jsonPath(
                                "$['paths']['/api/services/{id}/deactivate']['put']['security'][0]['bearerAuth']"
                        ).isArray()
                )
                .andExpect(
                        jsonPath("$.components.schemas.BookableServiceCreateRequest")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.components.schemas.BookableServiceResponse")
                                .exists()
                );
    }

    @Test
    void shouldExposeSwaggerUiWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/swagger-ui.html")
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/swagger-ui/index.html"
                        )
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

    private String createJwt(
            String base64Secret,
            String subject,
            Instant issuedAt,
            Instant expiration
    ) {
        SecretKey signingKey =
                Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                base64Secret
                        )
                );

        return Jwts.builder()
                .subject(subject)
                .issuedAt(
                        Date.from(issuedAt)
                )
                .expiration(
                        Date.from(expiration)
                )
                .signWith(signingKey)
                .compact();
    }

    private void verifyBookingStatus(
            String accessToken,
            Long bookingId,
            String expectedStatus
    ) throws Exception {

        mockMvc.perform(
                        get(
                                "/api/bookings/{id}",
                                bookingId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
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
                        jsonPath("$.status")
                                .value(expectedStatus)
                );
    }

    private void assertSecurityErrorContract(
            ResultActions result,
            int expectedStatus,
            String expectedError,
            String expectedCode,
            String expectedMessage,
            String expectedPath
    ) throws Exception {

        result
                .andExpect(
                        status().is(expectedStatus)
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(expectedStatus)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(expectedError)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(expectedCode)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(expectedMessage)
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(expectedPath)
                )
                .andExpect(
                        jsonPath("$.violations")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.violations.length()")
                                .value(0)
                );
    }

    private void assertApiErrorContract(
            ResultActions result,
            int expectedStatus,
            String expectedError,
            String expectedCode,
            String expectedPath
    ) throws Exception {

        result
                .andExpect(
                        status().is(expectedStatus)
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(expectedStatus)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(expectedError)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(expectedCode)
                )
                .andExpect(
                        jsonPath("$.message")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(expectedPath)
                )
                .andExpect(
                        jsonPath("$.violations")
                                .isArray()
                );
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