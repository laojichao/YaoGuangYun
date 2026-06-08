package com.yaoguangyun.network;

/**
 * TCP响应回调接口
 * 基于bbc.me应用的TCP响应处理实现
 * 
 * 用于异步处理TCP请求的响应结果
 * 支持成功和失败两种回调场景
 */
public interface TcpResponseCallback {
    
    /**
     * 成功回调
     * 
     * 当TCP请求成功时调用
     * 
     * @param response 服务器响应字符串
     */
    void onSuccess(String response);
    
    /**
     * 失败回调
     * 
     * 当TCP请求失败时调用
     * 
     * @param e 异常信息
     */
    void onError(Exception e);
}