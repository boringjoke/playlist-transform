package com.tooldeck.backend.playlist.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 识别设计文档中明确支持的版本标记，并将其从字段中移出。
 */
@Component
public class VersionDetector {

    private static final Pattern VERSION_MARKER = Pattern.compile(
            "(?i)(?<![A-Za-z0-9])(Live|Remix|DJ)(?![A-Za-z0-9])|(伴奏|纯音乐|翻唱)",
            Pattern.UNICODE_CASE);

    private static final Pattern EMPTY_BRACKETS =
            Pattern.compile("[\\(\\[【（]\\s*[\\)\\]】）]");

    /**
     * 从一个字段中识别版本标记。
     *
     * @param value 原始字段
     * @return 去除版本标记后的文本和按输入顺序排列的版本
     */
    public Detection detect(String value) {
        String source = value == null ? "" : value;
        Matcher matcher = VERSION_MARKER.matcher(source);
        StringBuffer remaining = new StringBuffer();
        List<String> versions = new ArrayList<>();
        while (matcher.find()) {
            String marker = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            versions.add(canonicalMarker(marker));
            matcher.appendReplacement(remaining, " ");
        }
        matcher.appendTail(remaining);
        String text = EMPTY_BRACKETS.matcher(remaining.toString()).replaceAll(" ");
        return new Detection(text, versions);
    }

    /**
     * 将大小写不一致的英文版本标记转换为稳定写法。
     *
     * @param marker 原始版本标记
     * @return 标准版本标记
     */
    private String canonicalMarker(String marker) {
        if (marker.equalsIgnoreCase("live")) {
            return "Live";
        }
        if (marker.equalsIgnoreCase("remix")) {
            return "Remix";
        }
        if (marker.equalsIgnoreCase("dj")) {
            return "DJ";
        }
        return marker;
    }

    /**
     * 版本识别结果。
     *
     * @param text 移除版本标记后的字段
     * @param versions 识别到的版本标记
     */
    public record Detection(String text, List<String> versions) {

        /**
         * 创建版本识别结果并保护版本列表不可变。
         *
         * @param text 移除版本标记后的字段
         * @param versions 识别到的版本标记
         */
        public Detection {
            versions = List.copyOf(versions);
        }

        /**
         * 返回移除版本标记后的字段。
         *
         * @return 清理前的剩余字段
         */
        @Override
        public String text() {
            return text;
        }

        /**
         * 返回按输入顺序排列的版本标记。
         *
         * @return 版本标记列表
         */
        @Override
        public List<String> versions() {
            return versions;
        }
    }
}
