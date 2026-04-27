package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.embedding.Embedding;

/**
 * 文本向量化服务接口。
 * <p>
 * 它定义的能力非常简单：给我一段文本，我返回一个向量。
 * <p>
 * 之所以还要单独抽成接口，是因为底层实现可能很多：
 * 本地模型、远程模型、简化模型都可以复用这个统一入口。
 */
public interface TextEmbeddingService {

    /**
     * 把一段文本转换为向量。
     *
     * @param text 待向量化文本
     * @return 文本对应的向量表示
     */
    Embedding embed(String text);
}
