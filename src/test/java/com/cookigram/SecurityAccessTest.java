package com.cookigram;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenNotAuthenticated_customerDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/customer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "customer1", roles = {"CUSTOMER"})
    void customerCannotAccessAdminOrEmployeeDashboards() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employee1", roles = {"EMPLOYEE"})
    void employeeCanAccessEmployeeDashboardButNotAdmin() throws Exception {
        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void adminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }
}