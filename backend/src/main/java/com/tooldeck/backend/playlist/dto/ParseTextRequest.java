package com.tooldeck.backend.playlist.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 歌单文本解析请求。
 */
public class ParseTextRequest {

    /** 用户粘贴的原始歌单文本。 */
    @NotBlank(message = "请提供歌曲清单文本")
    private String text;

    /**
     * 返回用户提交的原始文本。
     *
     * @return 原始歌单文本
     */
    public String getText() {
        return text;
    }

    /**
     * 设置用户提交的原始文本。
     *
     * @param text 原始歌单文本
     */
    public void setText(String text) {
        this.text = text;
    }
}
