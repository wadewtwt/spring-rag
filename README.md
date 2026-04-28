# spring-rag

This repository contains a Spring Boot backend and a frontend client for a local RAG demo.

## Repository Layout

- `backend`: Spring Boot 3 Maven project
- `frontend`: Vite React frontend
- `docs`: project notes, specs, and run guides

## Java Version

The backend requires JDK 17.

This project is intended to coexist with machines that still use JDK 8 as the global default. Do not change your whole machine just for this repository. Set JDK 17 only for this project or export it in the current shell before running Maven.

## Local Backend Commands

From `D:\work\java\spring-rag\backend`:

```powershell
$env:JAVA_HOME='D:\Scoop\apps\openjdk17\current'
$env:Path='D:\Scoop\apps\openjdk17\current\bin;' + $env:Path
.\mvnw.cmd -version
.\mvnw.cmd test
```

## IDEA Setup For Backend

Open `D:\work\java\spring-rag\backend` as the Maven project, then check:

1. `File -> Project Structure -> Project -> SDK`
   Set to `D:\Scoop\apps\openjdk17\current`
2. `File -> Project Structure -> Modules -> backend -> Dependencies -> Module SDK`
   Set to the same JDK 17
3. `Settings -> Build, Execution, Deployment -> Build Tools -> Maven -> Runner -> JRE`
   Set to the same JDK 17

After that, click `Reload All Maven Projects`.

## DashScope Qwen Setup

The backend now supports a real DashScope-backed RAG workflow for:

- query rewrite
- retrieval evaluation
- final answer generation

Default configuration lives in [`backend/src/main/resources/application.yml`](D:/work/java/spring-rag/.worktrees/codex-qwen-rag-p0/backend/src/main/resources/application.yml:1).

To enable the real LLM:

```powershell
$env:DASHSCOPE_API_KEY='your-api-key'
```

Then set:

```yaml
app:
  rag:
    llm:
      enabled: true
```

Current defaults:

- provider: `dashscope`
- model: `qwen-plus`
- base URL: `https://dashscope.aliyuncs.com/compatible-mode/v1`

## RAG Status Endpoint

After startup, inspect:

- `GET http://localhost:8089/api/rag/status`

It now exposes:

- embedding mode
- store mode
- Chroma connection settings
- retrieval settings
- LLM provider/model/base URL summary

## Chroma Mode

See [`docs/chroma-local-run.md`](D:/work/java/spring-rag/.worktrees/codex-qwen-rag-p0/docs/chroma-local-run.md:1) for local Chroma startup and test instructions.

Note: the `ChatRagChromaIntegrationTest` only runs when a compatible Chroma V2 API is available on `localhost:8000`. If another service is bound to that port, the test is skipped and the application falls back to the in-memory store.
