# spring-rag

This repository contains a Spring Boot backend and a frontend client for a local RAG demo.

## Repository Layout

- `backend`: Spring Boot 3 Maven project
- `frontend`: Vite React frontend
- `docs`: project notes and design docs

## Java Version

The backend requires JDK 17.

This project is intended to coexist with machines that still use JDK 8 as the global default. Do not change your whole machine just for this repository. Instead, set JDK 17 only for this project in IDEA.

## IDEA Setup For backend

Open `D:\work\java\spring-rag\backend` as the Maven project, then check these three places:

1. `File -> Project Structure -> Project -> SDK`
   Set to `D:\Scoop\apps\openjdk17\current`
2. `File -> Project Structure -> Modules -> backend -> Dependencies -> Module SDK`
   Set to the same JDK 17
3. `Settings -> Build, Execution, Deployment -> Build Tools -> Maven -> Runner -> JRE`
   Set to the same JDK 17

After that, click `Reload All Maven Projects`.

## Maven Wrapper

Use the wrapper inside `backend` so this project does not depend on whichever Maven version is installed globally.

From `D:\work\java\spring-rag\backend`:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd -DskipTests compile
```

## Daily Rule For Multi-JDK Work

- Keep your global default JDK 8 for old projects
- Set JDK 17 only inside this project
- Read `backend/pom.xml` first when a new Java project imports badly

If IDEA shows many unresolved Spring or `jakarta.*` imports, the first thing to check is whether the project accidentally opened with JDK 8.
