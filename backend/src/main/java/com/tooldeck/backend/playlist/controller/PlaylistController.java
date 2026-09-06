package com.tooldeck.backend.playlist.controller;

import com.tooldeck.backend.common.response.ApiResponse;
import com.tooldeck.backend.playlist.application.PlaylistParseApplicationService;
import com.tooldeck.backend.playlist.dto.ParseTextRequest;
import com.tooldeck.backend.playlist.dto.ParseTextResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 歌单文本解析接口。
 */
@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

    private final PlaylistParseApplicationService parseService;

    /**
     * 创建歌单解析控制器。
     *
     * @param parseService 歌单解析应用服务
     */
    public PlaylistController(PlaylistParseApplicationService parseService) {
        this.parseService = parseService;
    }

    /**
     * 解析用户提交的歌曲清单文本。
     *
     * @param request 解析请求
     * @return 统一响应包装的解析结果
     */
    @PostMapping("/parseText")
    public ApiResponse<ParseTextResponse> parseText(@Valid @RequestBody ParseTextRequest request) {
        return ApiResponse.success(parseService.parseText(request.getText()));
    }
}
