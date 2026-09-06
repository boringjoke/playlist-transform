package com.tooldeck.backend.playlist.application;

import com.tooldeck.backend.playlist.domain.ParseResult;
import com.tooldeck.backend.playlist.dto.ParseTextResponse;
import com.tooldeck.backend.playlist.parser.PlaylistParser;
import org.springframework.stereotype.Service;

/**
 * 解析接口的应用层编排服务。
 */
@Service
public class PlaylistParseApplicationService {

    private final PlaylistParser parser;

    /**
     * 创建歌单解析应用服务。
     *
     * @param parser 无状态歌单文本解析器
     */
    public PlaylistParseApplicationService(PlaylistParser parser) {
        this.parser = parser;
    }

    /**
     * 解析文本并转换为接口响应模型。
     *
     * @param text 原始歌单文本
     * @return 解析业务响应
     */
    public ParseTextResponse parseText(String text) {
        ParseResult result = parser.parse(text);
        return ParseTextResponse.from(parser.getRuleVersion(), result);
    }
}
