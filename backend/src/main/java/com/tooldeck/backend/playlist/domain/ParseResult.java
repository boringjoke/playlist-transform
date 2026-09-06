package com.tooldeck.backend.playlist.domain;

import java.util.List;

/**
 * 解析核心输出，供应用层转换为接口响应。
 */
public final class ParseResult {

    /** 最终解析条目。 */
    private final List<ParsedTrack> tracks;

    /** 条目状态汇总。 */
    private final ParseSummary summary;

    /**
     * 创建解析结果。
     *
     * @param tracks 最终解析条目
     * @param summary 条目状态汇总
     */
    public ParseResult(List<ParsedTrack> tracks, ParseSummary summary) {
        this.tracks = List.copyOf(tracks);
        this.summary = summary;
    }

    /**
     * 返回最终解析条目。
     *
     * @return 解析条目
     */
    public List<ParsedTrack> getTracks() {
        return tracks;
    }

    /**
     * 返回条目状态汇总。
     *
     * @return 状态汇总
     */
    public ParseSummary getSummary() {
        return summary;
    }
}
