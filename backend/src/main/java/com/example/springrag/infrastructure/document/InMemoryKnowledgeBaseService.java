package com.example.springrag.infrastructure.document;

import com.example.springrag.application.document.KnowledgeBaseService;
import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.domain.document.DocumentRecord;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于 EmbeddingStore 的知识库服务实现。
 * <p>
 * 虽然类名里有 {@code InMemory}，但这里真正的关键点不是“内存”两个字，
 * 而是它已经把知识库最核心的流程串起来了：
 * 文档转文本 -> 文本切片 -> 片段向量化 -> 写入向量库 -> 查询时再做向量检索。
 * <p>
 * 这个实现非常适合初学者理解一个最小 RAG 知识库后端是怎么工作的。
 */
@Service
public class InMemoryKnowledgeBaseService implements KnowledgeBaseService {

    /**
     * 向量存储，负责保存片段向量和原始片段。
     */
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 文本向量化服务，负责把文本转成向量。
     */
    private final TextEmbeddingService embeddingService;

    public InMemoryKnowledgeBaseService(EmbeddingStore<TextSegment> embeddingStore,
                                        TextEmbeddingService embeddingService) {
        this.embeddingStore = embeddingStore;
        this.embeddingService = embeddingService;
    }

    /**
     * 把上传文档写入知识库。
     * <p>
     * 当前流程非常朴素：
     * 1. 生成文档 ID
     * 2. 把字节转成 UTF-8 文本
     * 3. 按行切片
     * 4. 给每个切片生成 metadata 和向量
     * 5. 写入 embedding store
     *
     * @param fileName 文件名
     * @param content 文件字节内容
     * @return 文档写入结果
     */
    @Override
    public DocumentRecord store(String fileName, byte[] content) {
        String documentId = UUID.randomUUID().toString();
        String text = new String(content, StandardCharsets.UTF_8);
        List<String> chunks = splitIntoChunks(text);

        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);

            // metadata 用来给切片补充额外信息，后续做来源展示和过滤时很有用。
            Metadata metadata = new Metadata()
                    .put("documentId", documentId)
                    .put("fileName", fileName)
                    .put("chunkIndex", index);

            /**
             * 这一行的作用是：用当前的文本片段 chunk 和补充信息 metadata 构建一个 TextSegment 实例。
             * TextSegment（来自 langchain4j）封装了片段文本和相关元数据（这里有 documentId、fileName、chunkIndex）。
             * 这样做的目的是把片段和它的来源信息一起保存到向量库，便于检索时展示来源或做过滤。
             * 在本代码中，随后会把该 segment 与对应的向量一起写入 embeddingStore。
             */
            TextSegment segment = TextSegment.from(chunk, metadata);

            // 这里已经切到 LangChain4j 的标准结构：
            // 文本片段先向量化，再连同片段本身一起写入向量库。
            embeddingStore.add(embeddingService.embed(chunk), segment);
        }

        return new DocumentRecord(documentId, fileName, "INDEXED");
    }

    /**
     * 根据用户查询检索相关来源片段。
     * <p>
     * 当前流程是：
     * 1. 把 query 向量化
     * 2. 构造检索请求
     * 3. 从向量库取回匹配片段
     * 4. 转成前端更容易展示的 SourceReference
     *
     * @param query 用户查询
     * @return 最多 3 条相关来源
     */
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

    /**
     * 把整篇文本切成多个片段。
     * <p>
     * 当前策略很简单：按行切片，并跳过空行。
     * 这是一个最小实现，后续可以升级成更合理的分块策略。
     *
     * @param text 原始全文文本
     * @return 非空文本片段列表
     */
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
