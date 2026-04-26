package com.example.springrag.infrastructure.document;

import com.example.springrag.application.document.KnowledgeBaseService;
import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.domain.document.DocumentRecord;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InMemoryKnowledgeBaseService implements KnowledgeBaseService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final TextEmbeddingService embeddingService;

    public InMemoryKnowledgeBaseService(EmbeddingStore<TextSegment> embeddingStore,
                                        TextEmbeddingService embeddingService) {
        this.embeddingStore = embeddingStore;
        this.embeddingService = embeddingService;
    }

    @Override
    public DocumentRecord store(String fileName, byte[] content) {
        String documentId = UUID.randomUUID().toString();
        String text = new String(content, StandardCharsets.UTF_8);
        List<String> chunks = splitIntoChunks(text);

        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            Metadata metadata = new Metadata()
                    .put("documentId", documentId)
                    .put("fileName", fileName)
                    .put("chunkIndex", index);
            TextSegment segment = TextSegment.from(chunk, metadata);

            // 这里已经切到 LangChain4j 的标准结构，后续替换真实 EmbeddingModel 和向量库时，业务接口不用改。
            embeddingStore.add(embeddingService.embed(chunk), segment);
        }

        return new DocumentRecord(documentId, fileName, "INDEXED");
    }

    @Override
    public List<SourceReference> search(String query) {
        Embedding queryEmbedding = embeddingService.embed(query);
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .minScore(0.15)
                .build();

        return embeddingStore.search(request).matches().stream()
                .map(match -> {
                    TextSegment segment = match.embedded();
                    String fileName = segment.metadata().getString("fileName");
                    return new SourceReference(fileName == null ? "unknown" : fileName, segment.text());
                })
                .toList();
    }

    private List<String> splitIntoChunks(String text) {
        List<String> result = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }
}
