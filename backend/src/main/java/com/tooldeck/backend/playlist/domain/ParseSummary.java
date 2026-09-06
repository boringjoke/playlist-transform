package com.tooldeck.backend.playlist.domain;

import java.util.List;

/**
 * 一次解析结果的状态汇总。
 */
public final class ParseSummary {

    /** 非空输入条目总数。 */
    private final int total;

    /** PARSED 条目数。 */
    private final int parsed;

    /** INCOMPLETE 条目数。 */
    private final int incomplete;

    /** AMBIGUOUS 条目数。 */
    private final int ambiguous;

    /** DUPLICATE 条目数。 */
    private final int duplicate;

    /** INVALID 条目数。 */
    private final int invalid;

    /**
     * 创建解析汇总。
     *
     * @param total 条目总数
     * @param parsed 已解析条目数
     * @param incomplete 信息不完整条目数
     * @param ambiguous 存在歧义条目数
     * @param duplicate 疑似重复条目数
     * @param invalid 无效条目数
     */
    private ParseSummary(int total, int parsed, int incomplete, int ambiguous, int duplicate, int invalid) {
        this.total = total;
        this.parsed = parsed;
        this.incomplete = incomplete;
        this.ambiguous = ambiguous;
        this.duplicate = duplicate;
        this.invalid = invalid;
    }

    /**
     * 根据最终条目状态生成汇总。
     *
     * @param tracks 最终条目
     * @return 状态汇总
     */
    public static ParseSummary from(List<ParsedTrack> tracks) {
        int parsed = 0;
        int incomplete = 0;
        int ambiguous = 0;
        int duplicate = 0;
        int invalid = 0;
        for (ParsedTrack track : tracks) {
            switch (track.getStatus()) {
                case PARSED -> parsed++;
                case INCOMPLETE -> incomplete++;
                case AMBIGUOUS -> ambiguous++;
                case DUPLICATE -> duplicate++;
                case INVALID -> invalid++;
            }
        }
        return new ParseSummary(tracks.size(), parsed, incomplete, ambiguous, duplicate, invalid);
    }

    /**
     * 返回非空输入条目总数。
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
     * 返回存在字段歧义的条目数。
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
