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

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final KnowledgeBaseService knowledgeBaseService;

    public DocumentController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentRecord upload(@RequestPart("file") MultipartFile file) throws IOException {
        // 一期先做本地内存索引，保证上传后的内容可以立刻参与检索。
        return knowledgeBaseService.store(file.getOriginalFilename(), file.getBytes());
    }
}
