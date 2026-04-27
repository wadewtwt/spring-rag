package com.example.springrag.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 最基础的健康检查接口。
 * <p>
 * 这一层属于 API 层，也就是后端对外暴露 HTTP 接口的入口。
 * 这个类的职责非常单一：告诉调用方“应用是否已经启动并且还能正常响应请求”。
 * <p>
 * 在真实项目里，健康检查通常会被前端、运维平台、容器编排系统
 * 或网关拿来判断服务是不是活着。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 返回一个最简单的健康状态。
     *
     * @return 固定返回 {@code {"status":"UP"}}，表示当前服务可用
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
