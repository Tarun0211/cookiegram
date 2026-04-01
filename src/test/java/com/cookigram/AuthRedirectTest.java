package com.cookigram;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminLoginRedirectsToAdminDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void employeeLoginRedirectsToEmployeeDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("employee1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));
    }

    @Test
    void customerLoginRedirectsToCustomerDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("customer1").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
    }
}