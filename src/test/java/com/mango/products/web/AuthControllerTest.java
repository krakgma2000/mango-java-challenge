package com.mango.products.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mango.products.business.AuthBusiness;
import com.mango.products.domain.LoginResponseDTO;
import com.mango.products.exceptions.UnauthorizedException;
import com.mango.products.exceptions.ValidationException;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthBusiness authBusiness;

    @Test
    void loginReturns200WithTheToken() throws Exception {
        when(authBusiness.login(any())).thenReturn(new LoginResponseDTO("the-token", "Bearer", 3600));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"mango_test\", \"password\": \"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"accessToken\": \"the-token\", \"tokenType\": \"Bearer\", \"expiresIn\": 3600}", true));
    }

    @Test
    void wrongCredentialsReturn401WithWwwAuthenticate() throws Exception {
        when(authBusiness.login(any())).thenThrow(new UnauthorizedException("Invalid username or password"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"mango_test\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        when(authBusiness.login(any())).thenThrow(new ValidationException("username cannot be null or blank"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("username cannot be null or blank"));
    }
}
