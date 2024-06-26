package com.bruce.durpc.core.consumer.http;

import com.alibaba.fastjson.JSON;
import com.bruce.durpc.core.api.RpcException;
import com.bruce.durpc.core.api.RpcRequest;
import com.bruce.durpc.core.api.RpcResponse;
import com.bruce.durpc.core.consumer.HttpInvoker;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OKHttp client
 *
 * @date 2024/3/20
 */
@Slf4j
public class OkHttpInvoker implements HttpInvoker {

    OkHttpClient client;

    final static MediaType JSONTYPE = MediaType.get("application/json; charset=utf-8");

    public OkHttpInvoker(int timeout) {
        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(16,60, TimeUnit.SECONDS))
                .readTimeout(timeout,TimeUnit.MILLISECONDS)
                .writeTimeout(timeout,TimeUnit.MILLISECONDS)
                .connectTimeout(timeout,TimeUnit.MILLISECONDS)
                .build();
    }

    public RpcResponse post(RpcRequest rpcRequest, String url){
        String reqJson = JSON.toJSONString(rpcRequest);
        log.debug("reqJson ====== " + reqJson);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(reqJson,JSONTYPE))
                .build();
        String respJson = null;
        try {
            respJson = client.newCall(request).execute().body().string();
            log.debug("respJson ====== " + respJson);
            RpcResponse response = JSON.parseObject(respJson,RpcResponse.class);
            return response;
        } catch (IOException e) {
            throw new RpcException(e, RpcException.UnknownEx);
        }
    }

    public String post(String requestString, String url) {
        log.debug(" ===> post  url = {}, requestString = {}", requestString, url);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestString, JSONTYPE))
                .build();
        try {
            String respJson = client.newCall(request).execute().body().string();
            log.debug(" ===> respJson = " + respJson);
            return respJson;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String url) {
        log.debug(" ===> get url = " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            String respJson = client.newCall(request).execute().body().string();
            log.debug(" ===> respJson = " + respJson);
            return respJson;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
