# Conversation State P1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist chat session state in the database and make follow-up chat requests use stored conversation history and stable failure-state rules.

**Architecture:** Keep `SessionStateRepository` as the application boundary and replace the in-memory adapter with a JPA-backed repository that stores one JSON snapshot per `threadId`. Extend the chat orchestration flow so `StubChatService` restores and saves snapshots around each request, while `RagWorkflowService` accepts recent history for rewrite and answer generation.

**Tech Stack:** Spring Boot 3, Spring Data JPA, H2, Jackson, Maven, JUnit 5, AssertJ, MockMvc

---

### Task 1: Add persistence-focused regression coverage

**Files:**
- Modify: `backend/src/test/java/com/example/springrag/application/chat/InMemorySessionStateRepositoryTest.java`
- Create: `backend/src/test/java/com/example/springrag/application/chat/JpaSessionStateRepositoryTest.java`
- Modify: `backend/src/test/java/com/example/springrag/api/chat/ChatRagIntegrationTest.java`

- [ ] **Step 1: Write the failing tests**
- [ ] **Step 2: Run the focused tests and verify they fail**
- [ ] **Step 3: Add minimal persistence implementation**
- [ ] **Step 4: Re-run the focused tests and verify they pass**

### Task 2: Add database-backed session persistence

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/springrag/infrastructure/chat/SessionStateEntity.java`
- Create: `backend/src/main/java/com/example/springrag/infrastructure/chat/SpringDataSessionStateRepository.java`
- Create: `backend/src/main/java/com/example/springrag/infrastructure/chat/JpaSessionStateRepository.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application.yml`

- [ ] **Step 1: Add the failing persistence test for save/load round trip**
- [ ] **Step 2: Run `.\mvnw.cmd -Dtest=JpaSessionStateRepositoryTest test` and verify it fails**
- [ ] **Step 3: Add JPA, H2, entity mapping, and JSON snapshot serialization**
- [ ] **Step 4: Re-run `.\mvnw.cmd -Dtest=JpaSessionStateRepositoryTest test` and verify it passes**

### Task 3: Make `SessionState` restorable

**Files:**
- Modify: `backend/src/main/java/com/example/springrag/domain/chat/SessionState.java`
- Modify: `backend/src/main/java/com/example/springrag/domain/chat/ChatMessage.java`
- Modify: `backend/src/main/java/com/example/springrag/domain/chat/SourceReference.java`

- [ ] **Step 1: Add the failing test for restoring a snapshot with messages, sources, handoff state, and failure count**
- [ ] **Step 2: Run the focused repository tests and verify they fail for missing restore behavior**
- [ ] **Step 3: Add snapshot/restore support with stable serialization fields**
- [ ] **Step 4: Re-run the focused tests and verify they pass**

### Task 4: Feed recent history into the workflow

**Files:**
- Modify: `backend/src/main/java/com/example/springrag/application/chat/RagWorkflowService.java`
- Modify: `backend/src/main/java/com/example/springrag/application/chat/RagWorkflowResult.java`
- Modify: `backend/src/test/java/com/example/springrag/application/chat/RagWorkflowServiceTest.java`
- Modify: `backend/src/test/java/com/example/springrag/application/chat/support/FakeWorkflowLlmGateway.java`

- [ ] **Step 1: Add a failing test for follow-up questions using prior turns**
- [ ] **Step 2: Run `.\mvnw.cmd -Dtest=RagWorkflowServiceTest test` and verify it fails**
- [ ] **Step 3: Add minimal recent-history plumbing into workflow execution**
- [ ] **Step 4: Re-run `.\mvnw.cmd -Dtest=RagWorkflowServiceTest test` and verify it passes**

### Task 5: Apply failure counter rules in chat orchestration

**Files:**
- Modify: `backend/src/main/java/com/example/springrag/infrastructure/chat/StubChatService.java`
- Modify: `backend/src/test/java/com/example/springrag/api/chat/ChatRagIntegrationTest.java`
- Modify: `backend/src/test/java/com/example/springrag/api/chat/ChatControllerTest.java`

- [ ] **Step 1: Add failing tests for failure increment and success reset**
- [ ] **Step 2: Run `.\mvnw.cmd -Dtest=ChatRagIntegrationTest,ChatControllerTest test` and verify they fail**
- [ ] **Step 3: Implement counter update rules around workflow success, fallback, and exception**
- [ ] **Step 4: Re-run `.\mvnw.cmd -Dtest=ChatRagIntegrationTest,ChatControllerTest test` and verify they pass**

### Task 6: Run the backend verification slice

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Update `TODO.md` to mark the completed P1 conversation/state items**
- [ ] **Step 2: Run `.\mvnw.cmd test`**
- [ ] **Step 3: Confirm focused chat and repository coverage stays green**
