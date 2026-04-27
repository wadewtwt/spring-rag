package com.example.springrag.application.document;

import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.domain.document.DocumentRecord;

import java.util.List;

/**
 * 知识库应用服务接口。
 * <p>
 * 它抽象了知识库最核心的两个用例：
 * 1. 存文档
 * 2. 查文档
 * <p>
 * 这样 Controller 只依赖接口，不依赖具体的向量库、Embedding 模型、
 * 切片策略或存储实现。
 */
public interface KnowledgeBaseService {

    /**
     * 把一个文档写入知识库。
     *
     * @param fileName 原始文件名
     * @param content 文件字节内容
     * @return 文档写入后的记录信息
     */
    DocumentRecord store(String fileName, byte[] content);

    /**
     * 根据查询语句检索相关来源片段。
     *
     * @param query 用户查询
     * @return 检索到的来源列表
     */
    List<SourceReference> search(String query);
}
