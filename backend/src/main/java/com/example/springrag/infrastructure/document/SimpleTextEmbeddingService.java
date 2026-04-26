package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.embedding.Embedding;

import java.util.Locale;

public class SimpleTextEmbeddingService implements TextEmbeddingService {

    private static final int DIMENSION = 128;

    @Override
    public Embedding embed(String text) {
        float[] vector = new float[DIMENSION];
        String normalized = normalize(text);

        // 这里保留一个纯本地的兜底实现，避免模型初始化失败时整个检索链路不可用。
        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) {
                accumulate(vector, "tok:" + token, 2.0f);
            }
        }

        String compact = normalized.replace(" ", "");
        for (int index = 0; index < compact.length(); index++) {
            accumulate(vector, "chr:" + compact.charAt(index), 0.5f);
            if (index < compact.length() - 1) {
                accumulate(vector, "bi:" + compact.substring(index, index + 2), 1.0f);
            }
        }

        Embedding embedding = Embedding.from(vector);
        embedding.normalize();
        return embedding;
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace("，", " ")
                .replace("。", " ")
                .replace("？", " ")
                .replace("！", " ")
                .replace(",", " ")
                .replace(".", " ")
                .replace("?", " ")
                .replace("!", " ")
                .trim();
    }

    private void accumulate(float[] vector, String feature, float weight) {
        int index = Math.floorMod(feature.hashCode(), DIMENSION);
        vector[index] += weight;
    }
}
