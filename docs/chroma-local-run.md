# Chroma Local Run

## Start Chroma

```powershell
cd D:\work\java\spring-rag
docker compose up -d
```

This exposes Chroma on `http://localhost:8000`.

## Run The Backend In Chroma Mode

```powershell
$env:JAVA_HOME='D:\Scoop\apps\openjdk17\current'
$env:Path='D:\Scoop\apps\openjdk17\current\bin;' + $env:Path
cd D:\work\java\spring-rag\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=chroma"
```

## Check Current Mode

After startup, open:

- `GET http://localhost:8089/api/rag/status`

If the response shows `storeMode=chroma`, the profile is active.

## Run The Chroma Integration Test

The repository includes `ChatRagChromaIntegrationTest`.

```powershell
$env:JAVA_HOME='D:\Scoop\apps\openjdk17\current'
$env:Path='D:\Scoop\apps\openjdk17\current\bin;' + $env:Path
cd D:\work\java\spring-rag\backend
.\mvnw.cmd -Dtest=ChatRagChromaIntegrationTest test
```

The test auto-skips when nothing is listening on `localhost:8000`.
