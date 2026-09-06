package com.tooldeck.backend.common.health;

import com.tooldeck.backend.common.response.ApiResponse;
import com.tooldeck.backend.common.response.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供阶段 1 的基础服务健康检查，不处理任何用户歌单内容。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 返回后端基础可用状态。
     *
     * @return 统一格式的健康检查响应
     */
    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("playlist-transform-backend", "UP"));
    }
}
