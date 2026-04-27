package com.example.springrag.domain.document;

/**
 * 文档入库后的结果对象。
 * <p>
 * 当前它只保留最基础的三个字段，
 * 用来告诉调用方“哪份文档被处理了、名字是什么、当前状态如何”。
 *
 * @param documentId 文档唯一 ID
 * @param fileName 原始文件名
 * @param status 当前处理状态，例如 INDEXED
 */
public record DocumentRecord(String documentId, String fileName, String status) {
}
