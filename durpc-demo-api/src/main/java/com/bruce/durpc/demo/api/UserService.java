package com.bruce.durpc.demo.api;

import java.util.List;
import java.util.Map;

/**
 * @date 2024/3/7
 */
public interface UserService {

    User findById(int id);

    User findById(int id,String name);

    long getId(long id);

    long getId(float id);

    long getId(User user);

    String getName();

    String getName(int id);

    int[] getIds();

    int[] getIds(int[] ids);

    long[] getLongIds();

    User[] findUsers(User[] users);

    List<User> getList(List<User> userList);

    Map<String, User> getMap(Map<String, User> userMap);

    Boolean getFlag(boolean flag);

    User findById(long id);

    User ex(boolean flag);

    User find(int timeout);

    void setTimeoutPorts(String timeoutPorts);
}
