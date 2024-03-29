package com.bruce.durpc.core.consumer;

import com.bruce.durpc.core.api.RpcException;
import com.bruce.durpc.core.api.Filter;
import com.bruce.durpc.core.api.RpcContext;
import com.bruce.durpc.core.api.RpcRequest;
import com.bruce.durpc.core.api.RpcResponse;
import com.bruce.durpc.core.consumer.http.OkHttpInvoker;
import com.bruce.durpc.core.meta.InstanceMeta;
import com.bruce.durpc.core.util.MethodUtils;
import com.bruce.durpc.core.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * 消费端动态代理处理类
 *
 * @date 2024/3/10
 */
@Slf4j
public class DuInvocationHandler implements InvocationHandler {

    Class<?> service;
    RpcContext context;
    List<InstanceMeta> providers;

    HttpInvoker httpInvoker;

    public DuInvocationHandler(Class<?> service, RpcContext context, List<InstanceMeta> providers) {
        this.service = service;
        this.context = context;
        this.providers = providers;
        int timeout = Integer.parseInt(context.getParameters().getOrDefault("app.timeout","1000"));
        this.httpInvoker = new OkHttpInvoker(timeout);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理本地方法
        if(MethodUtils.checkLocalMethod(method)){
            return null;
        }

        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethodSign(MethodUtils.methodSign(method));
        request.setArgs(args);

        int retries = Integer.parseInt(context.getParameters().getOrDefault("app.retries","2"));
        while (retries -- > 0) {
            log.info("====> retries: " + retries);
            try {
                List<Filter> filters = context.getFilters();
                for (Filter filter : filters) {
                    Object result = filter.preFilter(request);
                    if (result != null) {
                        log.info(filter.getClass().getName() + " preFilter: " + result);
                        return result;
                    }
                }

                List<InstanceMeta> instances = context.getRouter().route(this.providers);
                InstanceMeta instance = context.getLoadBalancer().choose(instances);
                log.debug("loadBalancer.choose(instances) ======= " + instance);

                RpcResponse response = httpInvoker.post(request, instance.toUrl());
                Object result = castResponseToResult(method, response);
                for (Filter filter : filters) {
                    result = filter.postFilter(request, response, result);
                }

                return result;
            } catch (RuntimeException e) {
                if (!(e.getCause() instanceof SocketTimeoutException)) {
                    break;
                }

            }
        }
        return null;
    }

    @Nullable
    private static Object castResponseToResult(Method method, RpcResponse response) {
        if(response.isStatus()){
            return TypeUtils.castMethodResult(method, response.getData());
        }else {
            Exception exception = response.getEx();
            if(exception instanceof RpcException ex){
                throw ex;
            }
            throw new RuntimeException(exception);
        }
    }

}
