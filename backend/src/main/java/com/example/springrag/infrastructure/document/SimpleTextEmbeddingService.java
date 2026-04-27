package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.embedding.Embedding;

import java.util.Locale;

/**
 * 一个非常轻量的文本向量化实现。
 * <p>
 * 它不是严格意义上的真实语义模型，
 * 更像一个“能跑通流程的简化向量方案”：
 * 根据 token、字符、双字符组合，把特征累加到固定维度向量里。
 * <p>
 * 这种实现的优点是完全本地、无需模型初始化、稳定好测；
 * 缺点是语义效果远不如真正的 embedding 模型。
 */
public class SimpleTextEmbeddingService implements TextEmbeddingService {

    /**
     * 向量维度。
     * 这里固定为 128，方便快速构造一个轻量向量。
     */
    private static final int DIMENSION = 128;

    /**
     * 把文本转成一个简单向量。
     *
     * @param text 原始文本
     * @return 归一化后的向量
     */
    @Override
    public Embedding embed(String text) {
        float[] vector = new float[DIMENSION];
        String normalized = normalize(text);

        // 第一轮特征：按空格切词后的 token 特征。
        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) {
                accumulate(vector, "tok:" + token, 2.0f);
            }
        }

        // 第二轮特征：字符特征和双字符特征。
        // 这样可以让向量不只依赖空格分词，对中文或短文本也稍微更稳一点。
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

    /**
     * 对文本做最基础的归一化处理：
     * 转小写、去掉常见标点、压成更容易提取特征的格式。
     *
     * @param text 原始文本
     * @return 归一化后的文本
     */
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

    /**
     * 把一个特征按哈希映射到固定维度向量中的某个位置，并累加权重。
     *
     * @param vector 目标向量
     * @param feature 特征名
     * @param weight 当前特征要加上的权重
     */
    private void accumulate(float[] vector, String feature, float weight) {
        int index = Math.floorMod(feature.hashCode(), DIMENSION);
        vector[index] += weight;
    }
}
