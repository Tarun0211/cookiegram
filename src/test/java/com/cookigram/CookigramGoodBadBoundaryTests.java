package com.cookigram;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CookigramGoodBadBoundaryTests
 *
 * One-file test suite demonstrating Good / Bad / Boundary testing
 * for Sprint 1 requirements: login, redirect-by-role, access control,
 * and landing page server-driven promotions.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CookigramGoodBadBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    // ============================================================
    // LANDING PAGE / PROMOTIONS
    // ============================================================

    @Test
    @DisplayName("GOOD: Landing page loads and contains Promotions section")
    void good_landingPageLoads_andShowsPromotionsSection() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Current Promotions")));
    }

    @Test
    @DisplayName("GOOD: Landing page shows seeded promotion title from server (DataInitializer)")
    void good_landingPageShowsSeededPromotionTitle_fromServer() throws Exception {
        // Based on your DataInitializer seeding: "Spring Special"
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Spring Special")));
    }

    // ============================================================
    // LOGIN (GOOD / BAD / BOUNDARY)
    // ============================================================

    @Test
    @DisplayName("GOOD: Customer login redirects to /customer/dashboard")
    void good_customerLoginRedirectsToCustomerDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("customer1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
    }

    @Test
    @DisplayName("GOOD: Employee login redirects to /employee/dashboard")
    void good_employeeLoginRedirectsToEmployeeDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("employee1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));
    }

    @Test
    @DisplayName("GOOD: Admin login redirects to /admin/dashboard")
    void good_adminLoginRedirectsToAdminDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("BAD: Wrong password keeps user on login error flow")
    void bad_wrongPasswordShowsLoginError() throws Exception {
        mockMvc.perform(formLogin("/login").user("customer1").password("wrongpassword"))
                .andExpect(status().is3xxRedirection())
                // Spring Security default failureUrl in your config: /login?error=true
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("BOUNDARY: Empty username/password should fail login and redirect to /login?error=true")
    void boundary_emptyUsernamePasswordFailsLogin() throws Exception {
        // Even though UI uses required fields, this tests server-side boundary behavior.
        mockMvc.perform(formLogin("/login").user("").password(""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("BOUNDARY: Extremely long username should fail login (no crash)")
    void boundary_extremelyLongUsernameFailsLogin() throws Exception {
        String longUsername = "x".repeat(300);

        mockMvc.perform(formLogin("/login").user(longUsername).password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    // ============================================================
    // AUTHORIZATION / ACCESS CONTROL (GOOD / BAD)
    // ============================================================

    @Test
    @DisplayName("GOOD: Unauthenticated access to /customer/dashboard redirects to login")
    void good_unauthenticatedCustomerDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/customer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("BAD: Customer cannot access Admin or Employee dashboards")
    @WithMockUser(username = "customer1", roles = {"CUSTOMER"})
    void bad_customerBlockedFromAdminAndEmployeeDashboards() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BAD: Employee cannot access Admin dashboard")
    @WithMockUser(username = "employee1", roles = {"EMPLOYEE"})
    void bad_employeeBlockedFromAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GOOD: Admin can access Admin dashboard")
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void good_adminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GOOD: Employee can access Employee dashboard")
    @WithMockUser(username = "employee1", roles = {"EMPLOYEE"})
    void good_employeeCanAccessEmployeeDashboard() throws Exception {
        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GOOD: Customer can access Customer dashboard")
    @WithMockUser(username = "customer1", roles = {"CUSTOMER"})
    void good_customerCanAccessCustomerDashboard() throws Exception {
        mockMvc.perform(get("/customer/dashboard"))
                .andExpect(status().isOk());
    }
}