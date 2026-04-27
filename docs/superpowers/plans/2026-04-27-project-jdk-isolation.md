# Project JDK Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `spring-rag` stable on machines whose global Java default remains JDK 8 by documenting project-local JDK 17 usage and adding a Maven wrapper.

**Architecture:** Keep the operating system and other legacy projects unchanged. Isolate the backend's tooling expectations inside the repository by using Maven Wrapper and explicit IDE instructions.

**Tech Stack:** Maven Wrapper, Spring Boot 3, IntelliJ IDEA, JDK 17

---

### Task 1: Add Maven Wrapper

**Files:**
- Create: `backend/mvnw`
- Create: `backend/mvnw.cmd`
- Create: `backend/.mvn/wrapper/maven-wrapper.properties`

- [x] Generate the Maven wrapper from the `backend` directory
- [x] Verify wrapper scripts and wrapper properties are created

### Task 2: Document Project-Specific JDK Usage

**Files:**
- Create: `README.md`

- [x] Document the repository layout
- [x] Document the requirement that `backend` uses JDK 17
- [x] Document the three IDEA settings that must point to JDK 17
- [x] Document using `backend\\mvnw.cmd` instead of global Maven

### Task 3: Record The Chosen Approach

**Files:**
- Create: `docs/superpowers/plans/2026-04-27-project-jdk-isolation.md`

- [x] Record the minimal coexistence strategy
- [x] Keep the plan focused on project-level isolation rather than machine-wide changes
