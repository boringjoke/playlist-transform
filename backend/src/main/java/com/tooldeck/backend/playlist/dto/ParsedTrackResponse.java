package com.tooldeck.backend.playlist.dto;

import com.tooldeck.backend.playlist.domain.ParsedTrack;
import com.tooldeck.backend.playlist.domain.TrackStatus;

import java.util.List;

/**
 * 解析条目的接口响应字段。
 */
public final class ParsedTrackResponse {

    /** 本次无状态处理的临时客户端标识，不代表平台歌曲 ID。 */
    private final String clientId;

    /** 非空原始行序号。 */
    private final int sourceIndex;

    /** 未包含换行符的原始输入行。 */
    private final String rawText;

    /** 清理后的歌名。 */
    private final String title;

    /** 按输入顺序排列的歌手。 */
    private final List<String> artists;

    /** 版本标记，无版本时为空。 */
    private final String version;

    /** 条目状态。 */
    private final TrackStatus status;

    /** 字段边界置信度。 */
    private final double confidence;

    /** 稳定警告代码。 */
    private final List<String> warnings;

    /**
     * 根据领域条目创建接口响应。
     *
     * @param track 解析领域条目
     */
    private ParsedTrackResponse(ParsedTrack track) {
        this.clientId = track.getClientId();
        this.sourceIndex = track.getSourceIndex();
        this.rawText = track.getRawText();
        this.title = track.getTitle();
        this.artists = track.getArtists();
        this.version = track.getVersion();
        this.status = track.getStatus();
        this.confidence = track.getConfidence();
        this.warnings = track.getWarnings();
    }

    /**
     * 将领域条目转换为接口响应。
     *
     * @param track 解析领域条目
     * @return 解析条目响应
     */
    public static ParsedTrackResponse from(ParsedTrack track) {
        return new ParsedTrackResponse(track);
    }

    /**
     * 返回临时客户端标识。
     *
     * @return 临时客户端标识
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 返回原始行序号。
     *
     * @return 原始行序号
     */
    public int getSourceIndex() {
        return sourceIndex;
    }

    /**
     * 返回原始输入行。
     *
     * @return 原始输入行
     */
    public String getRawText() {
        return rawText;
    }

    /**
     * 返回清理后的歌名。
     *
     * @return 歌名
     */
    public String getTitle() {
        return title;
    }

    /**
     * 返回按输入顺序排列的歌手列表。
     *
     * @return 歌手列表
     */
    public List<String> getArtists() {
        return artists;
    }

    /**
     * 返回版本标记。
     *
     * @return 版本标记，没有版本时为空
     */
    public String getVersion() {
        return version;
    }

    /**
     * 返回条目状态。
     *
     * @return 条目状态
     */
    public TrackStatus getStatus() {
        return status;
    }

    /**
     * 返回字段边界置信度。
     *
     * @return 0 到 1 之间的置信度
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * 返回稳定警告代码列表。
     *
     * @return 警告代码列表
     */
    public List<String> getWarnings() {
        return warnings;
    }
}
