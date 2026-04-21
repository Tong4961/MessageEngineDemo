package com.me.demo.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * RPC调用工具类
 * 生产者发送消息后等待消费者返回结果
 */
@Slf4j
public class RpcSyncContext {

    private static final Map<String, CompletableFuture<ResponseResult>> FUTURE_MAP = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 30000; // 30秒超时

    /**
     * 注册一个请求的future，用于接收响应
     */
    public static CompletableFuture<ResponseResult> createRequest(String requestId) {
        CompletableFuture<ResponseResult> future = new CompletableFuture<>();
        FUTURE_MAP.put(requestId, future);
        return future;
    }

    /**
     * 等待响应结果
     */
    public static ResponseResult getResponse(String requestId) {
        CompletableFuture<ResponseResult> future = FUTURE_MAP.get(requestId);
        if (future == null) {
            return ResponseResult.error("request not found: " + requestId);
        }
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("rpc timeout, requestId: {}", requestId);
            return ResponseResult.error("rpc timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseResult.error("rpc interrupted");
        } catch (ExecutionException e) {
            log.error("rpc execution error, requestId: {}", requestId, e);
            return ResponseResult.error(e.getMessage());
        } finally {
            FUTURE_MAP.remove(requestId);
        }
    }

    /**
     * 异步获取响应
     */
    public static CompletableFuture<ResponseResult> getResponseAsync(String requestId) {
        CompletableFuture<ResponseResult> future = FUTURE_MAP.get(requestId);
        if (future == null) {
            return CompletableFuture.completedFuture(ResponseResult.error("request not found: " + requestId));
        }
        return future;
    }

    /**
     * 消费者处理完成后回调，唤醒生产者
     */
    public static void onResponse(String requestId, ResponseResult response) {
        CompletableFuture<ResponseResult> future = FUTURE_MAP.remove(requestId);
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("response callback but future not found, requestId: {}", requestId);
        }
    }

    /**
     * 取消请求
     */
    public static void cancelRequest(String requestId) {
        CompletableFuture<ResponseResult> future = FUTURE_MAP.remove(requestId);
        if (future != null) {
            future.cancel(false);
        }
    }
}