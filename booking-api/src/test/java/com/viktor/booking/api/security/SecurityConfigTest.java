package com.viktor.booking.api.security;

import com.viktor.booking.application.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(
        classes = SecurityConfigTest.TestConfiguration.class
)
@WebAppConfiguration
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldAllowRegistrationWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/register")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowLoginWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowHealthEndpointWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowServiceListWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/services")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowSingleServiceWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/services/5")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401ForBookingsWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedUserToAccessBookings()
            throws Exception {

        mockMvc.perform(
                        get("/api/bookings")
                                .with(
                                        user("user@example.com")
                                                .roles("USER")
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserCreatesService()
            throws Exception {

        mockMvc.perform(
                        post("/api/services")
                                .with(
                                        user("user@example.com")
                                                .roles("USER")
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToCreateService()
            throws Exception {

        mockMvc.perform(
                        post("/api/services")
                                .with(
                                        user("admin@example.com")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserActivatesService()
            throws Exception {

        mockMvc.perform(
                        put("/api/services/5/activate")
                                .with(
                                        user("user@example.com")
                                                .roles("USER")
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToActivateService()
            throws Exception {

        mockMvc.perform(
                        put("/api/services/5/activate")
                                .with(
                                        user("admin@example.com")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserAccessesUsers()
            throws Exception {

        mockMvc.perform(
                        get("/api/users/1")
                                .with(
                                        user("user@example.com")
                                                .roles("USER")
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToAccessUsers()
            throws Exception {

        mockMvc.perform(
                        get("/api/users/1")
                                .with(
                                        user("admin@example.com")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyUnlistedEndpointEvenForAdmin()
            throws Exception {

        mockMvc.perform(
                        get("/internal/test")
                                .with(
                                        user("admin@example.com")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @Import({
            SecurityConfig.class,
            SecurityErrorResponseWriter.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean
        TokenService tokenService() {
            return mock(TokenService.class);
        }

        @Bean
        CustomUserDetailsService userDetailsService() {
            return mock(
                    CustomUserDetailsService.class
            );
        }

        @Bean
        TestController testController() {
            return new TestController();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .findAndRegisterModules();
        }
    }

    @RestController
    static class TestController {

        @PostMapping("/api/auth/register")
        void register() {
        }

        @PostMapping("/api/auth/login")
        void login() {
        }

        @GetMapping("/api/health")
        void health() {
        }

        @GetMapping("/api/services")
        void getServices() {
        }

        @GetMapping("/api/services/{id}")
        void getService(
                @PathVariable("id") Long id
        ) {
        }

        @PostMapping("/api/services")
        void createService() {
        }

        @PutMapping("/api/services/{id}/activate")
        void activateService(
                @PathVariable("id") Long id
        ) {
        }

        @PutMapping("/api/services/{id}/deactivate")
        void deactivateService(
                @PathVariable("id") Long id
        ) {
        }

        @GetMapping("/api/users/{id}")
        void getUser(
                @PathVariable("id") Long id
        ) {
        }

        @GetMapping("/api/bookings")
        void getBookings() {
        }

        @GetMapping("/internal/test")
        void internalEndpoint() {
        }
    }
}
