package com.example.springrag.api.document;

import com.example.springrag.application.document.KnowledgeBaseService;
import com.example.springrag.domain.document.DocumentRecord;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文档上传接口。
 * <p>
 * 它属于知识库管理的 API 入口，负责接收前端上传的文件，
 * 再把文件内容交给应用层的知识库服务处理。
 * <p>
 * 这里的 Controller 仍然保持“薄”：
 * 不直接负责切片、向量化、入库，只负责 HTTP 层的收参与转发。
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    /**
     * 应用层知识库服务。
     */
    private final KnowledgeBaseService knowledgeBaseService;

    public DocumentController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 接收用户上传的文档并写入知识库。
     * <p>
     * 当前版本先走“读取文件字节 -> 转成文本 -> 切片 -> 建索引”的最小链路，
     * 目的是保证文档上传后能立刻参与检索。
     *
     * @param file 前端上传的 multipart 文件
     * @return 文档入库后的基本结果，例如文档 ID、文件名、状态
     * @throws IOException 读取上传文件内容失败时抛出
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentRecord upload(@RequestPart("file") MultipartFile file) throws IOException {
        return knowledgeBaseService.store(file.getOriginalFilename(), file.getBytes());
    }
}
