package com.tooldeck.backend.playlist.parser;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 歌单文本解析的运行时限制和规则版本。
 */
@Component
@ConfigurationProperties(prefix = "playlist.parser")
public class ParserProperties {

    /** 当前解析规则版本。 */
    private String ruleVersion = "v1";

    /** 单次允许解析的非空条目数。 */
    private int maxTracks = 500;

    /** 单行允许的最大 Unicode code point 数。 */
    private int maxLineLength = 1000;

    /** text 字段允许的最大 UTF-8 字节数。 */
    private int maxRequestBytes = 262144;

    /**
     * 返回当前解析规则版本。
     *
     * @return 规则版本
     */
    public String getRuleVersion() {
        return ruleVersion;
    }

    /**
     * 设置当前解析规则版本。
     *
     * @param ruleVersion 规则版本
     */
    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    /**
     * 返回单次允许解析的最大条目数。
     *
     * @return 最大条目数
     */
    public int getMaxTracks() {
        return maxTracks;
    }

    /**
     * 设置单次允许解析的最大条目数。
     *
     * @param maxTracks 最大条目数
     */
    public void setMaxTracks(int maxTracks) {
        this.maxTracks = maxTracks;
    }

    /**
     * 返回单行最大 Unicode code point 数。
     *
     * @return 单行长度上限
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * 设置单行最大 Unicode code point 数。
     *
     * @param maxLineLength 单行长度上限
     */
    public void setMaxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
    }

    /**
     * 返回 text 字段最大 UTF-8 字节数。
     *
     * @return 请求字节数上限
     */
    public int getMaxRequestBytes() {
        return maxRequestBytes;
    }

    /**
     * 设置 text 字段最大 UTF-8 字节数。
     *
     * @param maxRequestBytes 请求字节数上限
     */
    public void setMaxRequestBytes(int maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }
}
