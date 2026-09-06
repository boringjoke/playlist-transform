package com.tooldeck.backend.playlist.parser;

import com.tooldeck.backend.common.exception.InvalidRequestException;
import com.tooldeck.backend.playlist.domain.ParseResult;
import com.tooldeck.backend.playlist.domain.ParseSummary;
import com.tooldeck.backend.playlist.domain.ParsedTrack;
import com.tooldeck.backend.playlist.domain.TrackStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 阶段 2 的无状态歌单文本解析器。
 */
@Component
public class PlaylistParser {

    private static final Pattern SPACED_DASH = Pattern.compile("\\s+[-—]\\s+");
    private static final Pattern EM_DASH = Pattern.compile("—");
    private static final Pattern TAB = Pattern.compile("\\t+");
    private static final Pattern MULTI_SPACE = Pattern.compile(" {2,}");
    private static final Pattern ARTIST_SEPARATOR = Pattern.compile("[、,，/／&＆]");

    private final ParserProperties properties;
    private final TextNormalizer normalizer;
    private final VersionDetector versionDetector;

    /**
     * 创建无状态歌单解析器。
     *
     * @param properties 解析限制和规则版本配置
     * @param normalizer 文本标准化器
     * @param versionDetector 版本标记识别器
     */
    public PlaylistParser(
            ParserProperties properties,
            TextNormalizer normalizer,
            VersionDetector versionDetector) {
        this.properties = properties;
        this.normalizer = normalizer;
        this.versionDetector = versionDetector;
    }

    /**
     * 解析整段用户文本，并在返回前完成疑似重复标记。
     *
     * @param text 用户提交的原始文本
     * @return 解析结果
     */
    public ParseResult parse(String text) {
        validateRequest(text);
        List<String> nonEmptyLines = splitNonEmptyLines(text);
        if (nonEmptyLines.size() > properties.getMaxTracks()) {
            throw new InvalidRequestException("歌曲条目不能超过 " + properties.getMaxTracks() + " 条");
        }

        for (int index = 0; index < nonEmptyLines.size(); index++) {
            String line = nonEmptyLines.get(index);
            int codePointCount = line.codePointCount(0, line.length());
            if (codePointCount > properties.getMaxLineLength()) {
                throw new InvalidRequestException("第 " + (index + 1) + " 行超过单行长度限制");
            }
        }

        List<ParsedTrack> tracks = new ArrayList<>(nonEmptyLines.size());
        for (int index = 0; index < nonEmptyLines.size(); index++) {
            String rawText = nonEmptyLines.get(index);
            tracks.add(parseLine(rawText, index + 1));
        }

        List<ParsedTrack> deduplicatedTracks = markDuplicates(tracks);
        return new ParseResult(deduplicatedTracks, ParseSummary.from(deduplicatedTracks));
    }

    /**
     * 返回当前规则版本，供响应顶层字段使用。
     *
     * @return 规则版本
     */
    public String getRuleVersion() {
        return properties.getRuleVersion();
    }

    /**
     * 校验原始请求文本是否为空或超过 UTF-8 字节限制。
     *
     * @param text 原始歌单文本
     */
    private void validateRequest(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidRequestException("请提供歌曲清单文本");
        }
        int requestBytes = text.getBytes(StandardCharsets.UTF_8).length;
        if (requestBytes > properties.getMaxRequestBytes()) {
            throw new InvalidRequestException("输入文本超过请求大小限制");
        }
    }

    /**
     * 统一换行符并移除空白行。
     *
     * @param text 原始歌单文本
     * @return 按原始顺序排列的非空行
     */
    private List<String> splitNonEmptyLines(String text) {
        String normalizedNewlines = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        for (String line : normalizedNewlines.split("\n", -1)) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * 解析单个原始输入行并生成临时客户端标识。
     *
     * @param rawText 原始输入行
     * @param sourceIndex 非空行序号
     * @return 解析后的歌单条目
     */
    private ParsedTrack parseLine(String rawText, int sourceIndex) {
        String normalized = normalizer.normalizeForParsing(rawText);
        String withoutSequence = normalizer.removeLeadingSequence(normalized).trim();
        SplitResult split = splitFields(withoutSequence);

        VersionDetector.Detection titleDetection = versionDetector.detect(split.title());
        VersionDetector.Detection artistDetection = versionDetector.detect(split.artist());
        String title = normalizer.cleanField(titleDetection.text());
        String artistText = normalizer.cleanField(artistDetection.text());

        List<String> versions = new ArrayList<>(titleDetection.versions());
        versions.addAll(artistDetection.versions());
        String version = versions.isEmpty() ? null : String.join(" / ", versions);

        List<String> artists = splitArtists(artistText);
        List<String> warnings = new ArrayList<>();
        if (split.kind() == SplitKind.AMBIGUOUS) {
            warnings.add("AMBIGUOUS_SEPARATOR");
        } else if (split.kind() == SplitKind.SINGLE_SPACE) {
            warnings.add("SINGLE_SPACE_SPLIT");
        }
        if (!versions.isEmpty()) {
            warnings.add("VERSION_DETECTED");
        }

        boolean meaningfulTitle = normalizer.hasMeaningfulContent(title);
        TrackStatus status;
        double confidence;
        if (!meaningfulTitle) {
            status = TrackStatus.INVALID;
            confidence = 0.0;
            warnings.add("INVALID_EMPTY_TITLE");
        } else if (split.kind() == SplitKind.AMBIGUOUS) {
            status = TrackStatus.AMBIGUOUS;
            confidence = 0.40;
            warnings.add("MISSING_ARTIST");
        } else if (artists.isEmpty()) {
            status = TrackStatus.INCOMPLETE;
            confidence = Math.min(split.kind().confidence(), 0.68);
            warnings.add("MISSING_ARTIST");
        } else {
            status = TrackStatus.PARSED;
            confidence = split.kind().confidence();
        }

        String clientId = UUID.nameUUIDFromBytes(
                (properties.getRuleVersion() + "\u0000" + sourceIndex + "\u0000" + rawText)
                        .getBytes(StandardCharsets.UTF_8))
                .toString();
        return new ParsedTrack(
                clientId,
                sourceIndex,
                rawText,
                title,
                artists,
                version,
                status,
                confidence,
                warnings);
    }

    /**
     * 按设计文档中的分隔符拆分歌手列表。
     *
     * @param artistText 已清理的歌手字段
     * @return 按输入顺序排列的歌手列表
     */
    private List<String> splitArtists(String artistText) {
        if (artistText.isBlank()) {
            return List.of();
        }
        return ARTIST_SEPARATOR.splitAsStream(artistText)
                .map(normalizer::cleanField)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 按标准化歌名和歌手列表标记疑似重复条目。
     *
     * @param tracks 初步解析条目
     * @return 完成重复标记的条目
     */
    private List<ParsedTrack> markDuplicates(List<ParsedTrack> tracks) {
        Set<String> seenKeys = new HashSet<>();
        List<ParsedTrack> result = new ArrayList<>(tracks.size());
        for (ParsedTrack track : tracks) {
            if (track.getStatus() == TrackStatus.INVALID
                    || track.getStatus() == TrackStatus.AMBIGUOUS
                    || track.getArtists().isEmpty()) {
                result.add(track);
                continue;
            }
            String key = duplicateKey(track);
            if (!seenKeys.add(key)) {
                result.add(track.asDuplicate());
            } else {
                result.add(track);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 生成不包含版本字段的重复比较键。
     *
     * @param track 解析条目
     * @return 重复比较键
     */
    private String duplicateKey(ParsedTrack track) {
        return normalizer.normalizeKey(track.getTitle())
                + "\u001F"
                + track.getArtists().stream()
                .map(normalizer::normalizeKey)
                .collect(Collectors.joining("\u001F"));
    }

    /**
     * 按分隔符优先级拆分歌名和歌手字段。
     *
     * @param value 移除序号后的标准化文本
     * @return 字段拆分结果
     */
    private SplitResult splitFields(String value) {
        if (value.isBlank()) {
            return new SplitResult("", "", SplitKind.TITLE_ONLY);
        }

        SplitResult spacedDash = splitAtFirst(SPACED_DASH.matcher(value), value, SplitKind.SPACED_DASH);
        if (spacedDash != null) {
            return spacedDash;
        }
        SplitResult emDash = splitAtFirst(EM_DASH.matcher(value), value, SplitKind.EM_DASH);
        if (emDash != null) {
            return emDash;
        }
        SplitResult tab = splitAtFirst(TAB.matcher(value), value, SplitKind.TAB);
        if (tab != null) {
            return tab;
        }
        SplitResult multipleSpaces = splitAtFirst(MULTI_SPACE.matcher(value), value, SplitKind.MULTI_SPACE);
        if (multipleSpaces != null) {
            return multipleSpaces;
        }

        if (value.indexOf(' ') >= 0
                && value.indexOf(' ') == value.lastIndexOf(' ')
                && value.indexOf('\t') < 0) {
            int separator = value.indexOf(' ');
            String title = value.substring(0, separator).trim();
            String artist = value.substring(separator + 1).trim();
            if (!title.isEmpty() && !artist.isEmpty()) {
                return new SplitResult(title, artist, SplitKind.SINGLE_SPACE);
            }
        }

        if (containsWhitespace(value)) {
            return new SplitResult(value, "", SplitKind.AMBIGUOUS);
        }
        return new SplitResult(value, "", SplitKind.TITLE_ONLY);
    }

    /**
     * 使用正则匹配器找到第一个分隔位置并生成拆分结果。
     *
     * @param matcher 分隔符匹配器
     * @param value 待拆分文本
     * @param kind 分隔方式
     * @return 拆分结果，未匹配时返回 null
     */
    private SplitResult splitAtFirst(Matcher matcher, String value, SplitKind kind) {
        if (!matcher.find()) {
            return null;
        }
        return new SplitResult(
                value.substring(0, matcher.start()).trim(),
                value.substring(matcher.end()).trim(),
                kind);
    }

    /**
     * 判断文本中是否存在未被明确处理的空白字符。
     *
     * @param value 待判断文本
     * @return 是否包含空白字符
     */
    private boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 保存歌名、歌手字段和分隔方式的内部结果。
     *
     * @param title 歌名字段
     * @param artist 歌手字段
     * @param kind 分隔方式
     */
    private record SplitResult(String title, String artist, SplitKind kind) {

        /**
         * 返回歌名字段。
         *
         * @return 歌名字段
         */
        @Override
        public String title() {
            return title;
        }

        /**
         * 返回歌手字段。
         *
         * @return 歌手字段
         */
        @Override
        public String artist() {
            return artist;
        }

        /**
         * 返回分隔方式。
         *
         * @return 分隔方式
         */
        @Override
        public SplitKind kind() {
            return kind;
        }
    }

    private enum SplitKind {
        SPACED_DASH(0.98),
        EM_DASH(0.97),
        TAB(0.96),
        MULTI_SPACE(0.94),
        SINGLE_SPACE(0.82),
        TITLE_ONLY(0.68),
        AMBIGUOUS(0.40);

        private final double confidence;

        /**
         * 创建分隔方式及其基础置信度。
         *
         * @param confidence 基础置信度
         */
        SplitKind(double confidence) {
            this.confidence = confidence;
        }

        /**
         * 返回该分隔方式的基础置信度。
         *
         * @return 基础置信度
         */
        public double confidence() {
            return confidence;
        }
    }
}
