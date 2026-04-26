package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.embedding.Embedding;

public interface TextEmbeddingService {

    Embedding embed(String text);
}
