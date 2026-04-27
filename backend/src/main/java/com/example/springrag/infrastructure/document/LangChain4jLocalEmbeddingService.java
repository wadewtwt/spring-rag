package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;

/**
 * 基于 LangChain4j 本地模型的文本向量化实现。
 * <p>
 * 这个类的价值在于：不依赖外部 API key，也不需要远程服务，
 * 就能在本地把文本转成向量。
 * <p>
 * 对学习项目来说，这样可以更容易跑通完整 RAG 链路。
 */
public class LangChain4jLocalEmbeddingService implements TextEmbeddingService {

    /**
     * 真正负责计算向量的底层模型对象。
     */
    private final EmbeddingModel embeddingModel;

    /**
     * 默认构造方法，使用 BGE small 的量化本地模型。
     */
    public LangChain4jLocalEmbeddingService() {
        this(new BgeSmallEnV15QuantizedEmbeddingModel());
    }

    /**
     * 允许外部传入具体模型实现，便于测试或替换。
     *
     * @param embeddingModel 底层 embedding 模型
     */
    public LangChain4jLocalEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 使用本地模型把文本转成向量。
     *
     * @param text 待向量化的文本
     * @return 生成后的向量对象
     */
    @Override
    public Embedding embed(String text) {
        return embeddingModel.embed(text).content();
    }
}
