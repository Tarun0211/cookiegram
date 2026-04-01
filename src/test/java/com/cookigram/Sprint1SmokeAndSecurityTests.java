package com.cookigram;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class Sprint1SmokeAndSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------
    // Public Pages (PermitAll)
    // ------------------------------

    @Test
    @DisplayName("Landing page loads and shows hero + promotions section headings (Cookiegram wording)")
    void landingPageLoadsShowsHeroAndPromotionsHeading() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                // Your template says "Welcome to Cookiegram!" (NOT Cookigram)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome to Cookiegram")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current Promotions")));
    }

    @Test
    @DisplayName("Landing page includes the 'How It Works' section")
    void landingPageIncludesHowItWorks() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("How It Works")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose Your Cookies")));
    }

    @Test
    @DisplayName("Login page loads and shows main heading + form labels (Cookiegram wording)")
    void loginPageLoadsShowsHeadingAndFields() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                // Your template says "Login to Cookiegram"
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Login to Cookiegram")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Username")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Password")));
    }

    @Test
    @DisplayName("Register page loads")
    void registerPageLoads() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create Account")));
    }

    // ------------------------------
    // REST endpoint is PermitAll (/api/promotions)
    // ------------------------------

    @Test
    @DisplayName("GET /api/promotions returns JSON array (at least 1 promo seeded by DataInitializer)")
    void promotionsApiReturnsArray() throws Exception {
        mockMvc.perform(get("/api/promotions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].description").exists());
    }

    // ------------------------------
    // Unauthenticated behavior
    // ------------------------------

    @Test
    @DisplayName("Unauthenticated user is redirected to login for protected dashboards")
    void unauthenticatedRedirectedToLoginForProtectedDashboards() throws Exception {
        mockMvc.perform(get("/customer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ------------------------------
    // Role-based dashboards
    // ------------------------------

    @Test
    @DisplayName("Customer dashboard shows expected sections/cards")
    void customerDashboardShowsActionCards() throws Exception {
        mockMvc.perform(get("/customer/dashboard")
                        .with(user("customer1").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Customer Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Start New Order")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("View My Orders")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account Settings")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Order History")));
    }

    @Test
    @DisplayName("Employee dashboard shows expected table headings/sections")
    void employeeDashboardShowsOrdersAndDeliveries() throws Exception {
        mockMvc.perform(get("/employee/dashboard")
                        .with(user("employee1").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Employee Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Orders to Prepare")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upcoming Deliveries")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Delivery Date")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Actions")));
    }

    @Test
    @DisplayName("Admin dashboard shows expected sections")
    void adminDashboardShowsUserManagementAndSystemOverview() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User Management")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("System Overview")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reports Overview")));
    }

    // ------------------------------
    // Access Denied behavior (robust)
    // ------------------------------

    @Test
    @DisplayName("Customer cannot access employee/admin dashboards")
    void customerCannotAccessEmployeeOrAdmin() throws Exception {
        MvcResult result1 = mockMvc.perform(get("/employee/dashboard")
                        .with(user("customer1").roles("CUSTOMER")))
                .andReturn();
        assertAccessDenied(result1);

        MvcResult result2 = mockMvc.perform(get("/admin/dashboard")
                        .with(user("customer1").roles("CUSTOMER")))
                .andReturn();
        assertAccessDenied(result2);
    }

    @Test
    @DisplayName("Employee cannot access admin dashboard")
    void employeeCannotAccessAdmin() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/dashboard")
                        .with(user("employee1").roles("EMPLOYEE")))
                .andReturn();
        assertAccessDenied(result);
    }

    @Test
    @DisplayName("Access denied page loads and shows Access Denied message")
    void accessDeniedPageLoads() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Access Denied")));
    }

    // ------------------------------
    // Real login flow (DataInitializer users)
    // ------------------------------

    @Test
    @DisplayName("Form login redirects by role: admin1 -> /admin/dashboard")
    void adminLoginRedirectsToAdminDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Form login redirects by role: employee1 -> /employee/dashboard")
    void employeeLoginRedirectsToEmployeeDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("employee1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));
    }

    @Test
    @DisplayName("Form login redirects by role: customer1 -> /customer/dashboard")
    void customerLoginRedirectsToCustomerDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("customer1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
    }

    // ------------------------------
    // Registration happy path (creates customer, then can login)
    // ------------------------------

    @Test
    @DisplayName("Register a new customer and login successfully")
    void registerThenLoginWorks() throws Exception {
        String uniqueUsername = "cust_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "secret12";
        String email = uniqueUsername + "@example.com";

        // Register
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", uniqueUsername)
                        .param("fullName", "Test Customer")
                        .param("email", email)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        // Login with the newly registered user -> should go to customer dashboard
        mockMvc.perform(formLogin("/login").user(uniqueUsername).password(password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
    }

    // ------------------------------
    // Helper: accept common "denied" behaviors
    // ------------------------------
    private void assertAccessDenied(MvcResult result) throws Exception {
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        String location = result.getResponse().getHeader("Location");

        // Accept any of these valid security behaviors:
        // 1) 403 Forbidden
        // 2) 200 OK but renders Access Denied page (your Thymeleaf template)
        // 3) redirect to an access denied route/page
        boolean isForbidden = (status == 403);
        boolean isOkWithDeniedPage = (status == 200 && body.contains("Access Denied"));
        boolean isRedirectToDenied = (status >= 300 && status < 400
                && location != null
                && (location.contains("access-denied") || location.contains("denied")));

        assertTrue(isForbidden || isOkWithDeniedPage || isRedirectToDenied,
                "Expected Access Denied behavior, but got status=" + status
                        + ", location=" + location
                        + ", bodyContainsAccessDenied=" + body.contains("Access Denied"));
    }
}