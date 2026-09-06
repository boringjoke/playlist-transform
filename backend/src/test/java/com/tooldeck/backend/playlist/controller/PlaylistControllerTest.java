package com.tooldeck.backend.playlist.controller;

import com.tooldeck.backend.common.exception.GlobalExceptionHandler;
import com.tooldeck.backend.playlist.application.PlaylistParseApplicationService;
import com.tooldeck.backend.playlist.parser.ParserProperties;
import com.tooldeck.backend.playlist.parser.PlaylistParser;
import com.tooldeck.backend.playlist.parser.TextNormalizer;
import com.tooldeck.backend.playlist.parser.VersionDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaylistControllerTest {

    private MockMvc mockMvc;
    private ParserProperties properties;

    /**
     * 初始化歌单解析控制器的独立 MockMvc 环境。
     */
    @BeforeEach
    void setUp() {
        properties = new ParserProperties();
        PlaylistParser parser = new PlaylistParser(properties, new TextNormalizer(), new VersionDetector());
        PlaylistParseApplicationService service = new PlaylistParseApplicationService(parser);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaylistController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证正常解析响应包含规则版本、临时客户端标识和状态汇总。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void shouldReturnUnifiedParseResponseWithTemporaryClientId() throws Exception {
        mockMvc.perform(post("/api/playlist/parseText")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"1、龙卷风 周杰伦\\n2、夜曲\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.parserRuleVersion").value("v1"))
                .andExpect(jsonPath("$.data.tracks[0].clientId").isString())
                .andExpect(jsonPath("$.data.tracks[0].title").value("龙卷风"))
                .andExpect(jsonPath("$.data.tracks[0].artists[0]").value("周杰伦"))
                .andExpect(jsonPath("$.data.tracks[0].status").value("PARSED"))
                .andExpect(jsonPath("$.data.tracks[1].status").value("INCOMPLETE"))
                .andExpect(jsonPath("$.data.summary.total").value(2))
                .andExpect(jsonPath("$.data.summary.parsed").value(1))
                .andExpect(jsonPath("$.data.summary.incomplete").value(1));
    }

    /**
     * 验证超过配置条目上限时返回统一的 400 错误响应。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void shouldReturnBadRequestForOverLimitInput() throws Exception {
        properties.setMaxTracks(1);

        mockMvc.perform(post("/api/playlist/parseText")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"一 - 甲\\n二 - 乙\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 验证空文本请求返回统一的 400 错误响应。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void shouldReturnBadRequestForBlankText() throws Exception {
        mockMvc.perform(post("/api/playlist/parseText")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
