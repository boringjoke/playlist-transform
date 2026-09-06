package com.tooldeck.backend.common.health;

import com.tooldeck.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    private MockMvc mockMvc;

    /**
     * 初始化健康检查控制器的独立 MockMvc 环境。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证健康检查接口使用统一响应结构返回服务状态。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void shouldReturnUnifiedHealthResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.service").value("playlist-transform-backend"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
