package com.bruce.durpc.core.provider;

import com.bruce.durpc.core.api.RpcException;
import com.bruce.durpc.core.api.RpcRequest;
import com.bruce.durpc.core.api.RpcResponse;
import com.bruce.durpc.core.meta.ProviderMeta;
import com.bruce.durpc.core.util.TypeUtils;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * @date 2024/3/20
 */
@Data
public class ProviderInvoker {


    private MultiValueMap<String, ProviderMeta> skeleton;

    public ProviderInvoker(ProviderBootstrp providerBootstrp) {
        this.skeleton = providerBootstrp.getSkeleton();
    }

    public RpcResponse invoke(RpcRequest request) {
        RpcResponse rpcResponse = new RpcResponse();
        List<ProviderMeta> providerMetaList = skeleton.get(request.getService());
        try {
            ProviderMeta providerMeta = findProviderMeta(providerMetaList,request.getMethodSign());
            if(providerMeta != null){
                Method method = providerMeta.getMethod();
                Object[] args = processArgs(request.getArgs(),method.getParameterTypes());
                Object result = method.invoke(providerMeta.getServiceImpl(),args);
                rpcResponse.setStatus(true);
                rpcResponse.setData(result);
                return rpcResponse;
            }
        } catch (InvocationTargetException e) {
            rpcResponse.setEx(new RpcException(e.getTargetException().getMessage()));
        } catch (IllegalAccessException e) {
            rpcResponse.setEx(new RpcException(e.getMessage()));
        }
        return rpcResponse;
    }

    private Object[] processArgs(Object[] args, Class<?>[] parameterTypes) {
        if(args == null || args.length == 0){
            return args;
        }
        Object[] actualArgs = new Object[args.length];
        for (int i = 0; i < actualArgs.length; i++) {
            actualArgs[i] = TypeUtils.cast(args[i],parameterTypes[i]);
        }
        return actualArgs;
    }

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetaList, String methodSign) {
        Optional<ProviderMeta> optional = providerMetaList.stream().filter(x -> x.getMethodSign().equals(methodSign)).findFirst();
        return optional.orElse(null);
    }

}
