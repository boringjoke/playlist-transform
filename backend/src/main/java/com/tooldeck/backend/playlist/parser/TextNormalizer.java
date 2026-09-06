package com.tooldeck.backend.playlist.parser;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 负责解析前的 Unicode 和字段标准化，不执行业务猜测。
 */
@Component
public class TextNormalizer {

    private static final Pattern PUNCTUATED_SEQUENCE =
            Pattern.compile("^\\s*\\d+\\s*[、.)）]\\s*");

    private static final Pattern SPACE_SEQUENCE =
            Pattern.compile("^\\s*\\d+\\s+(?=\\S)");

    private static final Pattern NON_MEANINGFUL =
            Pattern.compile("[\\p{P}\\p{S}\\s]");

    /**
     * 对单行执行 NFKC、空格和等价短横线统一。
     *
     * @param rawText 原始单行
     * @return 供解析器使用的标准化单行
     */
    public String normalizeForParsing(String rawText) {
        String normalized = Normalizer.normalize(rawText, Normalizer.Form.NFKC);
        return normalized
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .replace('\u3000', ' ')
                .replace('\uFF0D', '-')
                .replace('\uFE63', '-')
                .replace('\u2010', '-')
                .replace('\u2011', '-')
                .replace('\u2012', '-')
                .replace('\u2013', '—');
    }

    /**
     * 仅移除行首序号，不影响歌名内部数字。
     *
     * @param normalizedText 已完成基础标准化的文本
     * @return 移除序号后的文本
     */
    public String removeLeadingSequence(String normalizedText) {
        String withoutPunctuation = PUNCTUATED_SEQUENCE.matcher(normalizedText).replaceFirst("");
        if (!withoutPunctuation.equals(normalizedText)) {
            return withoutPunctuation;
        }
        return SPACE_SEQUENCE.matcher(normalizedText).replaceFirst("");
    }

    /**
     * 清理字段首尾和内部连续空白。
     *
     * @param value 原始字段
     * @return 清理后的字段
     */
    public String cleanField(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    /**
     * 判断清理后的文本是否至少含有一个非标点、非符号字符。
     *
     * @param value 待判断文本
     * @return 是否可以作为有效歌名
     */
    public boolean hasMeaningfulContent(String value) {
        return value != null && !NON_MEANINGFUL.matcher(value).replaceAll("").isEmpty();
    }

    /**
     * 生成去重比较用的字段键。
     *
     * @param value 字段
     * @return 标准化比较键
     */
    public String normalizeKey(String value) {
        return cleanField(Normalizer.normalize(value, Normalizer.Form.NFKC))
                .toLowerCase(Locale.ROOT);
    }
}
