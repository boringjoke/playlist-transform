package com.tooldeck.backend.playlist.domain;

/**
 * 解析条目的业务状态。
 */
public enum TrackStatus {
    /** 歌名和歌手均已识别。 */
    PARSED,
    /** 歌名有效，但没有可靠的歌手字段。 */
    INCOMPLETE,
    /** 存在不能可靠判断的字段边界。 */
    AMBIGUOUS,
    /** 与更早的有效条目重复。 */
    DUPLICATE,
    /** 清理后没有有效歌名。 */
    INVALID
}
