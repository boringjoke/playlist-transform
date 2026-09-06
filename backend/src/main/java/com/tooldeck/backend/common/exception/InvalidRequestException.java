package com.tooldeck.backend.common.exception;

/**
 * 表示调用方提交的数据不符合当前接口约束。
 */
public class InvalidRequestException extends RuntimeException {

    /**
     * 创建请求校验异常。
     *
     * @param message 面向调用方的校验说明
     */
    public InvalidRequestException(String message) {
        super(message);
    }
}
