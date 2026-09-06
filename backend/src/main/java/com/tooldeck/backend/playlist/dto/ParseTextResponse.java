package com.tooldeck.backend.playlist.dto;

import com.tooldeck.backend.playlist.domain.ParseResult;

import java.util.List;

/**
 * 歌单文本解析业务响应。
 */
public final class ParseTextResponse {

    /** 解析规则版本。 */
    private final String parserRuleVersion;

    /** 解析条目。 */
    private final List<ParsedTrackResponse> tracks;

    /** 条目状态汇总。 */
    private final ParseSummaryResponse summary;

    /**
     * 根据解析结果创建接口响应。
     *
     * @param parserRuleVersion 解析规则版本
     * @param result 解析领域结果
     */
    private ParseTextResponse(String parserRuleVersion, ParseResult result) {
        this.parserRuleVersion = parserRuleVersion;
        this.tracks = result.getTracks().stream()
                .map(ParsedTrackResponse::from)
                .toList();
        this.summary = ParseSummaryResponse.from(result.getSummary());
    }

    /**
     * 将解析领域结果转换为接口响应。
     *
     * @param parserRuleVersion 解析规则版本
     * @param result 解析领域结果
     * @return 解析业务响应
     */
    public static ParseTextResponse from(String parserRuleVersion, ParseResult result) {
        return new ParseTextResponse(parserRuleVersion, result);
    }

    /**
     * 返回解析规则版本。
     *
     * @return 解析规则版本
     */
    public String getParserRuleVersion() {
        return parserRuleVersion;
    }

    /**
     * 返回解析条目。
     *
     * @return 解析条目
     */
    public List<ParsedTrackResponse> getTracks() {
        return tracks;
    }

    /**
     * 返回条目状态汇总。
     *
     * @return 状态汇总
     */
    public ParseSummaryResponse getSummary() {
        return summary;
    }
}
