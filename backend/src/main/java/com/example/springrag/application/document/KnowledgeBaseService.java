package com.example.springrag.application.document;

import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.domain.document.DocumentRecord;

import java.util.List;

public interface KnowledgeBaseService {

    DocumentRecord store(String fileName, byte[] content);

    List<SourceReference> search(String query);
}
