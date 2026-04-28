package com.example.springrag.application.chat.support;

import com.example.springrag.application.document.KnowledgeBaseService;
import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.domain.document.DocumentRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeWorkflowKnowledgeBaseService implements KnowledgeBaseService {

    private final Map<String, List<SourceReference>> resultsByQuery = new HashMap<>();

    public FakeWorkflowKnowledgeBaseService whenQuery(String query, List<SourceReference> sources) {
        resultsByQuery.put(query, sources);
        return this;
    }

    @Override
    public DocumentRecord store(String fileName, byte[] content) {
        throw new UnsupportedOperationException("Not needed in workflow tests");
    }

    @Override
    public List<SourceReference> search(String query) {
        return resultsByQuery.getOrDefault(query, List.of());
    }
}
