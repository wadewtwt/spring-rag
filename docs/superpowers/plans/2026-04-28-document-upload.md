# Document Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a document upload section to the existing chat page and connect it to the backend upload endpoint.

**Architecture:** Extend the existing `ChatWindow` component with a small upload state machine and keep network code in a dedicated document service. Verify behavior with frontend tests first, then implement the minimum UI and request handling to satisfy those tests.

**Tech Stack:** React 18, TypeScript, Vite, Vitest, Testing Library

---

### Task 1: Add upload behavior tests

**Files:**
- Modify: `frontend/src/components/ChatWindow.test.tsx`
- Test: `frontend/src/components/ChatWindow.test.tsx`

- [ ] **Step 1: Write the failing test**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Write minimal implementation**
- [ ] **Step 4: Run test to verify it passes**

### Task 2: Add upload service

**Files:**
- Create: `frontend/src/services/document.ts`

- [ ] **Step 1: Add multipart upload helper**
- [ ] **Step 2: Verify tests still fail only for missing UI behavior**

### Task 3: Extend chat page UI

**Files:**
- Modify: `frontend/src/components/ChatWindow.tsx`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add upload card state and submit handler**
- [ ] **Step 2: Render upload form, status, and uploaded file list**
- [ ] **Step 3: Run tests to verify green**

### Task 4: Final verification

**Files:**
- Modify: `frontend/src/components/ChatWindow.test.tsx`
- Modify: `frontend/src/services/document.ts`
- Modify: `frontend/src/components/ChatWindow.tsx`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Run frontend test suite**
- [ ] **Step 2: Run frontend build**
