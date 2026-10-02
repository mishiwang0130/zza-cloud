package com.wxy.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 树形结构组装测试。
 *
 * @author wxy
 * @date 2026/10/02
 */
class TreeUtilTest {

    /** 根节点的父 ID */
    private static final Long ROOT_PARENT_ID = 0L;

    @Test
    @DisplayName("按父子关系组装树，叶子节点回填空列表")
    void shouldBuildTree() {
        List<Node> nodes = List.of(
                new Node(1L, ROOT_PARENT_ID),
                new Node(2L, 1L),
                new Node(3L, 1L),
                new Node(4L, 2L));

        List<Node> roots = TreeUtil.build(nodes, Node::getId, Node::getParentId, Node::setChildren, ROOT_PARENT_ID);

        assertThat(roots).hasSize(1);
        Node root = roots.get(0);
        assertThat(root.getChildren()).hasSize(2);
        assertThat(root.getChildren().get(0).getChildren()).hasSize(1);
        assertThat(root.getChildren().get(1).getChildren()).isEmpty();
    }

    @Test
    @DisplayName("父节点不存在的节点按根节点处理，不会凭空消失")
    void shouldTreatOrphanAsRoot() {
        List<Node> nodes = List.of(new Node(1L, ROOT_PARENT_ID), new Node(2L, 99L));

        List<Node> roots = TreeUtil.build(nodes, Node::getId, Node::getParentId, Node::setChildren, ROOT_PARENT_ID);

        assertThat(roots).hasSize(2);
    }

    @Test
    @DisplayName("入参为空时返回空列表")
    void shouldReturnEmptyWhenNodesIsEmpty() {
        assertThat(TreeUtil.build(List.of(), Node::getId, Node::getParentId, Node::setChildren, ROOT_PARENT_ID))
                .isEmpty();
        assertThat(TreeUtil.build(null, Node::getId, Node::getParentId, Node::setChildren, ROOT_PARENT_ID))
                .isEmpty();
    }

    /**
     * 测试用节点：只保留组装树需要的最小字段
     *
     * @author wxy
     * @date 2026/10/02
     */
    private static class Node {

        /** 节点 ID */
        private final Long id;

        /** 父节点 ID */
        private final Long parentId;

        /** 子节点列表 */
        private List<Node> children;

        /**
         * 构造节点
         *
         * @param id       节点 ID
         * @param parentId 父节点 ID
         */
        Node(Long id, Long parentId) {
            this.id = id;
            this.parentId = parentId;
        }

        /**
         * 获取节点 ID
         *
         * @return 节点 ID
         */
        Long getId() {
            return id;
        }

        /**
         * 获取父节点 ID
         *
         * @return 父节点 ID
         */
        Long getParentId() {
            return parentId;
        }

        /**
         * 获取子节点列表
         *
         * @return 子节点列表
         */
        List<Node> getChildren() {
            return children;
        }

        /**
         * 回填子节点列表
         *
         * @param children 子节点列表
         */
        void setChildren(List<Node> children) {
            this.children = children;
        }
    }
}
