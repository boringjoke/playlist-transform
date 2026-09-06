package com.tooldeck.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 歌单清单转换工具后端应用入口。
 */
@SpringBootApplication
public class BackendApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
