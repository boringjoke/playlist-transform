package com.tooldeck.backend.common.response;

/**
 * 统一接口响应包装。
 *
 * @param <T> 业务数据类型
 */
public final class ApiResponse<T> {

    /** 机器可识别的结果码。 */
    private final String code;

    /** 面向调用方的简短中文说明。 */
    private final String message;

    /** 成功时的业务数据，失败时为空。 */
    private final T data;

    /**
     * 创建统一响应对象。
     *
     * @param code 机器可识别的结果码
     * @param message 面向调用方的说明
     * @param data 业务数据
     */
    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "操作成功", data);
    }

    /**
     * 创建失败响应。
     *
     * @param code 结果码
     * @param message 错误说明
     * @param <T> 业务数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 返回结果码。
     *
     * @return 结果码
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回面向调用方的说明。
     *
     * @return 操作说明
     */
    public String getMessage() {
        return message;
    }

    /**
     * 返回业务数据。
     *
     * @return 业务数据，失败时为空
     */
    public T getData() {
        return data;
    }
}
