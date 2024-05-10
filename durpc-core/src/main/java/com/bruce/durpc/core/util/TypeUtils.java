package com.bruce.durpc.core.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型转换工具类
 * @date 2024/3/15
 */
@Slf4j
public class TypeUtils {

    public static Object cast(Object origin,Class<?> type){
        if(origin == null){
            return null;
        }
        Class<?> aClass = origin.getClass();
        if(type.isAssignableFrom(aClass)){
            return origin;
        }

        if(type.isArray()){
            if(origin instanceof List list) {
                origin = list.toArray();
            }
            int length = Array.getLength(origin);
            Class<?> componentType = type.getComponentType();
            Object resultArray = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                if (componentType.isPrimitive() || componentType.getPackageName().startsWith("java")) {
                    Array.set(resultArray, i, Array.get(origin, i));
                } else {
                    Object castObject = cast(Array.get(origin, i), componentType);
                    Array.set(resultArray, i, castObject);
                }
            }
            return resultArray;
        }

        if(origin instanceof HashMap map){
            JSONObject jsonObject = new JSONObject(map);
            return jsonObject.toJavaObject(type);
        }

        if (origin instanceof JSONObject jsonObject) {
            return jsonObject.toJavaObject(type);
        }

        // 处理基本类型
        if(type.equals(Long.class) || type.equals(Long.TYPE)){
            return Long.valueOf(origin.toString());
        } else if(type.equals(Integer.class) || type.equals(Integer.TYPE)){
            return Integer.valueOf(origin.toString());
        } else if(type.equals(Float.class) || type.equals(Float.TYPE)){
            return Float.valueOf(origin.toString());
        } else if(type.equals(Double.class) || type.equals(Double.TYPE)){
            return Double.valueOf(origin.toString());
        } else if(type.equals(Short.class) || type.equals(Short.TYPE)){
            return Short.valueOf(origin.toString());
        } else if(type.equals(Byte.class) || type.equals(Byte.TYPE)){
            return Byte.valueOf(origin.toString());
        } else if(type.equals(Character.class) || type.equals(Character.TYPE)){
            return Character.valueOf(origin.toString().charAt(0));
        } else if(type.equals(Boolean.class) || type.equals(Boolean.TYPE)){
            return Boolean.valueOf(origin.toString());
        }

        return null;
    }

    public static Object castMethodResult(Method method, Object data) {
        log.debug("castMethodResult: method = " + method);
        log.debug("castMethodResult: data = " + data);
        Class<?> type = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        return castGeneric(data, type, genericReturnType);
    }

    public static Object castGeneric(Object data, Class<?> type, Type genericReturnType) {
        log.debug("castGeneric: method.getReturnType() = " + type);
        log.debug("castGeneric: method.getGenericReturnType() = " + genericReturnType);
        if(data instanceof Map map) { // data是map的情况包括两种，一种是HashMap，一种是JSONObject
            if(Map.class.isAssignableFrom(type)){ // 目标类型是 Map，此时data可能是map也可能是JO
                Map resultMap = new HashMap();
                log.debug(genericReturnType.toString());
                if(genericReturnType instanceof ParameterizedType parameterizedType){
                    Class<?> keyType = (Class<?>)parameterizedType.getActualTypeArguments()[0];
                    Class<?> valueType = (Class<?>)parameterizedType.getActualTypeArguments()[1];
                    map.forEach(
                            (k,v) -> {
                                Object key = TypeUtils.cast(k,keyType);
                                Object value = TypeUtils.cast(v,valueType);
                                resultMap.putIfAbsent(key,value);
                            }
                    );

                }
                return resultMap;
            }
            if(data instanceof JSONObject jsonObject) {// 此时是Pojo，且数据是JO
                og.debug(" ======> JSONObject -> Pojo");
                return jsonObject.toJavaObject(type);
            }else if(!Map.class.isAssignableFrom(type)){ // 此时是Pojo类型，数据是Map
                log.debug(" ======> map -> Pojo");
                return new JSONObject(map).toJavaObject(type);
            }else {
                log.debug(" ======> map -> ?");
                return data;
            }
        }else if(data instanceof JSONArray jsonArray){
            Object[] array = jsonArray.toArray();
            if(type.isArray()){
                Class<?> componentType = type.getComponentType();
                Object resultArray = Array.newInstance(componentType, array.length);
                for (int i = 0; i < array.length; i++) {
                    if (componentType.isPrimitive() || componentType.getPackageName().startsWith("java")) {
                        Array.set(resultArray, i, array[i]);
                    } else {
                        Object castObject = TypeUtils.cast(array[i], componentType);
                        Array.set(resultArray, i, castObject);
                    }
                }
                return resultArray;
            } else if(List.class.isAssignableFrom(type)){
                List resultList = new ArrayList<>();
                if(genericReturnType instanceof ParameterizedType parameterizedType){
                    Type actualType = parameterizedType.getActualTypeArguments()[0];
                    log.debug(actualType.toString());
                    for (Object o : array) {
                        resultList.add(TypeUtils.cast(o, (Class<?>) actualType));
                    }
                }else{
                    resultList.addAll(Arrays.asList(array));
                }
                return resultList;
            }else {
                return null;
            }
        }else{
            return cast(data, type);
        }

    }
}
