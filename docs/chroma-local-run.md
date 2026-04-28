# Chroma 本地联调

## 启动 Chroma

```powershell
cd D:\work\java\spring-rag
docker compose up -d
```

默认会在 `http://localhost:8000` 暴露 Chroma 服务。

## 启动后端的 Chroma 模式

```powershell
$env:JAVA_HOME='D:\Scoop\apps\openjdk17\current'
$env:Path='D:\Scoop\apps\openjdk17\current\bin;' + $env:Path
cd D:\work\java\spring-rag\backend
mvn spring-boot:run "-Dspring-boot.run.profiles=chroma"
```

## 查看当前运行模式

启动后可访问：

- `GET http://localhost:8089/api/rag/status`

如果返回 `storeMode=chroma`，说明当前已经切到 Chroma 向量存储。
