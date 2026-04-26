package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;

public class LangChain4jLocalEmbeddingService implements TextEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public LangChain4jLocalEmbeddingService() {
        this(new BgeSmallEnV15QuantizedEmbeddingModel());
    }

    public LangChain4jLocalEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public Embedding embed(String text) {
        // 真实向量由 LangChain4j 的本地 ONNX 模型生成，不依赖外部 API key。
        return embeddingModel.embed(text).content();
    }
}
