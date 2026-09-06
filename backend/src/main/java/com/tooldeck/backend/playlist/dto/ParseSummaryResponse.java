package com.tooldeck.backend.playlist.dto;

import com.tooldeck.backend.playlist.domain.ParseSummary;

/**
 * 解析状态汇总响应。
 */
public final class ParseSummaryResponse {

    /** 非空条目总数。 */
    private final int total;

    /** 已解析条目数。 */
    private final int parsed;

    /** 歌手缺失条目数。 */
    private final int incomplete;

    /** 字段边界歧义条目数。 */
    private final int ambiguous;

    /** 疑似重复条目数。 */
    private final int duplicate;

    /** 无效条目数。 */
    private final int invalid;

    /**
     * 根据领域汇总创建接口响应。
     *
     * @param summary 解析领域汇总
     */
    private ParseSummaryResponse(ParseSummary summary) {
        this.total = summary.getTotal();
        this.parsed = summary.getParsed();
        this.incomplete = summary.getIncomplete();
        this.ambiguous = summary.getAmbiguous();
        this.duplicate = summary.getDuplicate();
        this.invalid = summary.getInvalid();
    }

    /**
     * 将领域汇总转换为接口响应。
     *
     * @param summary 解析领域汇总
     * @return 状态汇总响应
     */
    public static ParseSummaryResponse from(ParseSummary summary) {
        return new ParseSummaryResponse(summary);
    }

    /**
     * 返回条目总数。
     *
     * @return 条目总数
     */
    public int getTotal() {
        return total;
    }

    /**
     * 返回已解析条目数。
     *
     * @return 已解析条目数
     */
    public int getParsed() {
        return parsed;
    }

    /**
     * 返回信息不完整条目数。
     *
     * @return 信息不完整条目数
     */
    public int getIncomplete() {
        return incomplete;
    }

    /**
     * 返回存在歧义的条目数。
     *
     * @return 歧义条目数
     */
    public int getAmbiguous() {
        return ambiguous;
    }

    /**
     * 返回疑似重复条目数。
     *
     * @return 重复条目数
     */
    public int getDuplicate() {
        return duplicate;
    }

    /**
     * 返回无效条目数。
     *
     * @return 无效条目数
     */
    public int getInvalid() {
        return invalid;
    }
}
