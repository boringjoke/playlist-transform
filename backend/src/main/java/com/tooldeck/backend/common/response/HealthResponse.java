package com.tooldeck.backend.common.response;

/**
 * 基础健康检查响应数据。
 */
public final class HealthResponse {

    /** 服务名称。 */
    private final String service;

    /** 服务状态。 */
    private final String status;

    /**
     * 创建健康检查响应数据。
     *
     * @param service 服务名称
     * @param status 服务状态
     */
    public HealthResponse(String service, String status) {
        this.service = service;
        this.status = status;
    }

    /**
     * 返回服务名称。
     *
     * @return 服务名称
     */
    public String getService() {
        return service;
    }

    /**
     * 返回服务状态。
     *
     * @return 服务状态
     */
    public String getStatus() {
        return status;
    }
}
