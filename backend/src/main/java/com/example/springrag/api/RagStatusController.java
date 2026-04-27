package com.example.springrag.api;

import com.example.springrag.config.RagProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用来查看当前 RAG 运行配置的接口。
 * <p>
 * 对初学者来说，这个接口非常适合拿来理解“配置是怎么进入业务代码的”：
 * 配置文件中的 {@code app.rag.*} 会先绑定到 {@link RagProperties}，
 * 然后 Controller 再把这些配置整理成一个更适合前端或调试查看的响应对象。
 * <p>
 * 这个接口不负责真正执行检索或问答，只负责“把当前系统打算怎么运行”展示出来。
 */
@RestController
@RequestMapping("/api/rag")
public class RagStatusController {

    /**
     * 保存从配置文件绑定过来的 RAG 参数。
     */
    private final RagProperties ragProperties;

    public RagStatusController(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * 返回当前启用的 embedding 模式、向量存储模式以及 Chroma 连接参数。
     * <p>
     * 这个接口的主要价值在于联调阶段排查问题，例如：
     * “当前到底走的是 local embedding 还是 simple embedding？”
     * “当前到底连的是内存向量库还是 Chroma？”
     *
     * @return 供前端或调试工具查看的 RAG 状态信息
     */
    @GetMapping("/status")
    public RagStatusResponse status() {
        return new RagStatusResponse(
                ragProperties.getEmbedding().getMode(),
                ragProperties.getStore().getMode(),
                new ChromaStatus(
                        ragProperties.getStore().getChroma().getBaseUrl(),
                        ragProperties.getStore().getChroma().getTenant(),
                        ragProperties.getStore().getChroma().getDatabase(),
                        ragProperties.getStore().getChroma().getCollection()
                )
        );
    }

    /**
     * 返回给调用方的外层状态对象。
     *
     * @param embeddingMode 当前 embedding 的实现模式
     * @param storeMode 当前向量存储的实现模式
     * @param chroma Chroma 相关连接信息
     */
    public record RagStatusResponse(String embeddingMode, String storeMode, ChromaStatus chroma) {
    }

    /**
     * Chroma 连接配置的只读展示对象。
     *
     * @param baseUrl Chroma 服务地址
     * @param tenant Chroma tenant
     * @param database Chroma database
     * @param collection Chroma collection 名称
     */
    public record ChromaStatus(String baseUrl, String tenant, String database, String collection) {
    }
}
