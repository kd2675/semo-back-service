package semo.back.service.common.exception;

import auth.common.core.context.RequirePrincipalRoleFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class HttpMethodContractIntegrationTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private FilterRegistrationBean<RequirePrincipalRoleFilter> requirePrincipalRoleFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requirePrincipalRoleFilter.getFilter())
                .build();
    }

    @Test
    void existingGetEndpoint_post_returnsWrappedMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/semo/v1/clubs/4/admin/more/join-requests")
                        .header("X-User-Key", "forged-user")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(4050000))
                .andExpect(jsonPath("$.message").value("HTTP method not allowed"));
    }

    @Test
    void missingEndpoint_get_returnsWrappedNotFound() throws Exception {
        mockMvc.perform(get("/api/semo/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(4040000))
                .andExpect(jsonPath("$.message").value("Endpoint not found"));
    }

}
