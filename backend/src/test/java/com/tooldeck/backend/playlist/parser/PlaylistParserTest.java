package com.tooldeck.backend.playlist.parser;

import com.tooldeck.backend.common.exception.InvalidRequestException;
import com.tooldeck.backend.playlist.domain.ParseResult;
import com.tooldeck.backend.playlist.domain.ParsedTrack;
import com.tooldeck.backend.playlist.domain.TrackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistParserTest {

    private ParserProperties properties;
    private PlaylistParser parser;

    /**
     * 初始化使用默认解析规则的解析器。
     */
    @BeforeEach
    void setUp() {
        properties = new ParserProperties();
        parser = new PlaylistParser(properties, new TextNormalizer(), new VersionDetector());
    }

    /**
     * 验证序号和各类明确分隔符可以被正确解析。
     */
    @Test
    void shouldParseSupportedSeparatorsAndSequences() {
        ParseResult result = parser.parse("""
                1、龙卷风 周杰伦
                2. 演员—薛之谦
                3) 七里香\t周杰伦
                4）晴天  周杰伦
                5、夜曲 - 周杰伦
                """);

        assertEquals(5, result.getSummary().getTotal());
        assertEquals(5, result.getSummary().getParsed());
        assertEquals("龙卷风", result.getTracks().get(0).getTitle());
        assertEquals("周杰伦", result.getTracks().get(0).getArtists().get(0));
        assertEquals(0.82, result.getTracks().get(0).getConfidence());
        assertEquals(0.97, result.getTracks().get(1).getConfidence());
        assertEquals(0.96, result.getTracks().get(2).getConfidence());
        assertEquals(0.94, result.getTracks().get(3).getConfidence());
        assertEquals(0.98, result.getTracks().get(4).getConfidence());
    }

    /**
     * 验证版本标记识别和多歌手顺序保留。
     */
    @Test
    void shouldDetectVersionsAndKeepArtistOrder() {
        ParseResult result = parser.parse("歌曲 Live Remix DJ 伴奏 纯音乐 翻唱 - 周杰伦、方文山/黄雨勋＆林迈可");
        ParsedTrack track = result.getTracks().get(0);

        assertEquals(TrackStatus.PARSED, track.getStatus());
        assertEquals("歌曲", track.getTitle());
        assertEquals("Live / Remix / DJ / 伴奏 / 纯音乐 / 翻唱", track.getVersion());
        assertEquals(4, track.getArtists().size());
        assertEquals("周杰伦", track.getArtists().get(0));
        assertEquals("林迈可", track.getArtists().get(3));
        assertTrue(track.getWarnings().contains("VERSION_DETECTED"));
    }

    /**
     * 验证原始文本保留、临时客户端标识稳定性和重复标记。
     */
    @Test
    void shouldKeepRawTextAndGenerateStableTemporaryClientIds() {
        String input = " 1、夜曲 - 周杰伦\r\n\r\n2、夜曲 - 周杰伦";
        ParseResult first = parser.parse(input);
        ParseResult second = parser.parse(input);

        assertEquals(" 1、夜曲 - 周杰伦", first.getTracks().get(0).getRawText());
        assertEquals(1, first.getTracks().get(0).getSourceIndex());
        assertEquals(2, first.getTracks().get(1).getSourceIndex());
        assertEquals(first.getTracks().get(0).getClientId(), second.getTracks().get(0).getClientId());
        assertEquals(first.getTracks().get(1).getClientId(), second.getTracks().get(1).getClientId());
        assertNotEquals(first.getTracks().get(0).getClientId(), first.getTracks().get(1).getClientId());
        assertEquals(TrackStatus.DUPLICATE, first.getTracks().get(1).getStatus());
        assertTrue(first.getTracks().get(1).getWarnings().contains("DUPLICATE_TRACK"));
    }

    /**
     * 验证信息不完整、字段歧义和无效条目的状态区分。
     */
    @Test
    void shouldSeparateIncompleteAmbiguousAndInvalidTracks() {
        ParseResult result = parser.parse("夜曲\nA Sky Full of Stars\n!!!");

        assertEquals(TrackStatus.INCOMPLETE, result.getTracks().get(0).getStatus());
        assertEquals(0.68, result.getTracks().get(0).getConfidence());
        assertTrue(result.getTracks().get(0).getWarnings().contains("MISSING_ARTIST"));

        assertEquals(TrackStatus.AMBIGUOUS, result.getTracks().get(1).getStatus());
        assertEquals("A Sky Full of Stars", result.getTracks().get(1).getTitle());
        assertTrue(result.getTracks().get(1).getWarnings().contains("AMBIGUOUS_SEPARATOR"));

        assertEquals(TrackStatus.INVALID, result.getTracks().get(2).getStatus());
        assertEquals(0.0, result.getTracks().get(2).getConfidence());
        assertTrue(result.getTracks().get(2).getWarnings().contains("INVALID_EMPTY_TITLE"));
        assertEquals(1, result.getSummary().getIncomplete());
        assertEquals(1, result.getSummary().getAmbiguous());
        assertEquals(1, result.getSummary().getInvalid());
    }

    /**
     * 验证全角文本标准化以及脚本文本按普通文本处理。
     */
    @Test
    void shouldNormalizeFullWidthTextAndTreatMarkupAsPlainText() {
        ParseResult result = parser.parse("１、ＡＢＣ　—　歌手\n<script>alert(1)</script> - 歌手");

        assertEquals("ABC", result.getTracks().get(0).getTitle());
        assertEquals("歌手", result.getTracks().get(0).getArtists().get(0));
        assertEquals("<script>alert(1)</script>", result.getTracks().get(1).getTitle());
        assertEquals(TrackStatus.PARSED, result.getTracks().get(1).getStatus());
    }

    /**
     * 验证条目数、单行长度和请求字节数限制拒绝超限请求。
     */
    @Test
    void shouldRejectTrackLineAndRequestLimitsWithoutPartialResult() {
        properties.setMaxTracks(2);
        assertThrows(InvalidRequestException.class, () -> parser.parse("一 - 甲\n二 - 乙\n三 - 丙"));

        properties.setMaxTracks(500);
        properties.setMaxLineLength(3);
        assertThrows(InvalidRequestException.class, () -> parser.parse("abcd - 歌手"));

        properties.setMaxLineLength(1000);
        properties.setMaxRequestBytes(2);
        assertThrows(InvalidRequestException.class, () -> parser.parse("歌曲"));
    }

    /**
     * 验证 500 条合法文本满足纯规则解析性能目标。
     */
    @Test
    void shouldParseFiveHundredTracksWithinOneSecond() {
        StringBuilder input = new StringBuilder();
        for (int index = 1; index <= 500; index++) {
            if (index > 1) {
                input.append('\n');
            }
            input.append("歌曲").append(index).append(" - 歌手").append(index);
        }

        ParseResult result = assertTimeout(Duration.ofSeconds(1), () -> parser.parse(input.toString()));

        assertEquals(500, result.getSummary().getTotal());
        assertEquals(500, result.getSummary().getParsed());
    }
}
