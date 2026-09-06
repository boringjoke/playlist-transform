package com.tooldeck.backend.playlist.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析后的单个歌单条目。
 */
public final class ParsedTrack {

    /** 本次无状态处理使用的临时客户端标识，不代表平台歌曲 ID。 */
    private final String clientId;

    /** 原始非空行在本次输入中的序号。 */
    private final int sourceIndex;

    /** 未包含换行符的原始输入行。 */
    private final String rawText;

    /** 清理后的歌名。 */
    private final String title;

    /** 按输入顺序排列的歌手列表。 */
    private final List<String> artists;

    /** 从歌名或歌手字段中识别出的版本标记。 */
    private final String version;

    /** 当前条目的解析状态。 */
    private final TrackStatus status;

    /** 本次规则对字段边界的确定程度。 */
    private final double confidence;

    /** 稳定的警告代码列表。 */
    private final List<String> warnings;

    /**
     * 创建解析后的歌单条目。
     *
     * @param clientId 本次无状态处理的临时客户端标识
     * @param sourceIndex 原始非空行序号
     * @param rawText 原始输入行
     * @param title 清理后的歌名
     * @param artists 按输入顺序排列的歌手
     * @param version 版本标记
     * @param status 条目状态
     * @param confidence 字段边界置信度
     * @param warnings 警告代码
     */
    public ParsedTrack(
            String clientId,
            int sourceIndex,
            String rawText,
            String title,
            List<String> artists,
            String version,
            TrackStatus status,
            double confidence,
            List<String> warnings) {
        this.clientId = clientId;
        this.sourceIndex = sourceIndex;
        this.rawText = rawText;
        this.title = title;
        this.artists = List.copyOf(artists);
        this.version = version;
        this.status = status;
        this.confidence = confidence;
        this.warnings = List.copyOf(warnings);
    }

    /**
     * 将当前条目标记为重复，并保留原有字段和置信度。
     *
     * @return 重复状态的新条目
     */
    public ParsedTrack asDuplicate() {
        List<String> duplicateWarnings = new ArrayList<>(warnings);
        if (!duplicateWarnings.contains("DUPLICATE_TRACK")) {
            duplicateWarnings.add("DUPLICATE_TRACK");
        }
        return new ParsedTrack(
                clientId,
                sourceIndex,
                rawText,
                title,
                artists,
                version,
                TrackStatus.DUPLICATE,
                confidence,
                duplicateWarnings);
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
     * 返回原始非空行序号。
     *
     * @return 原始行序号
     */
    public int getSourceIndex() {
        return sourceIndex;
    }

    /**
     * 返回未包含换行符的原始输入行。
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
     * 返回稳定的警告代码列表。
     *
     * @return 警告代码列表
     */
    public List<String> getWarnings() {
        return warnings;
    }
}
