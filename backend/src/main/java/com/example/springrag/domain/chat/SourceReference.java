package com.example.springrag.domain.chat;

import java.io.Serializable;

/**
 * 回答时引用的来源片段。
 * <p>
 * 当系统检索到知识库内容后，通常不会把整篇文档都返回给前端，
 * 而是只返回“标题 + 片段”这类更轻量的引用信息。
 *
 * @param title 来源标题，当前通常是文件名
 * @param snippet 片段内容，用来展示命中的文本
 */
public record SourceReference(String title, String snippet) implements Serializable {
}
