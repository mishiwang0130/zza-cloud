package com.wxy.common.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树形结构工具：把扁平列表按 parentId 组装成树，用于菜单、组织、分类等场景。
 *
 * <p>用函数式参数取值与回填，因此不要求节点实现任何接口，VO 与 DTO 都能直接用。
 * 两次遍历完成：先建 ID 索引，再挂子节点，时间复杂度 O(n)。
 *
 * <p><b>已知限制</b>：父子互相引用的环（A 的父是 B、B 的父是 A）不会被检测，
 * 这类节点不会出现在结果里，也不会报错；数据源头应保证父子关系是棵树。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class TreeUtil {

    /**
     * 工具类，禁止实例化
     */
    private TreeUtil() {
    }

    /**
     * 组装树形结构
     *
     * @param nodes          扁平节点列表，可以为 null
     * @param idGetter       取节点 ID
     * @param parentIdGetter 取父节点 ID
     * @param childrenSetter 回填子节点列表；叶子节点会被回填成空列表，避免前端拿到 null
     * @param rootParentId   根节点的父 ID，通常是 {@code 0}
     * @param <T>            节点类型
     * @param <K>            节点 ID 类型
     * @return 根节点列表；入参为空时返回空列表
     */
    public static <T, K> List<T> build(List<T> nodes,
                                       Function<T, K> idGetter,
                                       Function<T, K> parentIdGetter,
                                       BiConsumer<T, List<T>> childrenSetter,
                                       K rootParentId) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        Map<K, T> idMap = new HashMap<>(Math.max((int) (nodes.size() / 0.75f) + 1, 16));
        for (T node : nodes) {
            K id = idGetter.apply(node);
            if (id != null) {
                idMap.put(id, node);
            }
        }
        List<T> roots = new ArrayList<>();
        Map<K, List<T>> childrenMap = new LinkedHashMap<>();
        for (T node : nodes) {
            K parentId = parentIdGetter.apply(node);
            T parent = parentId == null ? null : idMap.get(parentId);
            // 找不到父节点、父节点是自己、或父 ID 等于根标识时按根处理，避免节点凭空消失
            if (parent == null || parent == node || Objects.equals(parentId, rootParentId)) {
                roots.add(node);
            } else {
                childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
            }
        }
        for (T node : nodes) {
            List<T> children = childrenMap.get(idGetter.apply(node));
            childrenSetter.accept(node, children == null ? new ArrayList<>() : children);
        }
        return roots;
    }
}
