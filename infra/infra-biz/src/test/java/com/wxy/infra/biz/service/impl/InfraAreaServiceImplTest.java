package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.wxy.infra.biz.convert.InfraAreaConvert;
import com.wxy.infra.biz.mapper.InfraAreaMapper;
import com.wxy.infra.biz.po.InfraArea;
import com.wxy.infra.biz.vo.admin.AreaRespVO;
import com.wxy.infra.biz.vo.app.AreaAppRespVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 行政区划服务单元测试：子级查询与树形组装。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraAreaServiceImplTest {

    /** 区划 Mapper */
    @Mock
    private InfraAreaMapper infraAreaMapper;

    /** 区划转换器 */
    @Mock
    private InfraAreaConvert infraAreaConvert;

    /** 被测服务 */
    private InfraAreaServiceImpl areaService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        areaService = new InfraAreaServiceImpl();
        ReflectionTestUtils.setField(areaService, "infraAreaMapper", infraAreaMapper);
        ReflectionTestUtils.setField(areaService, "infraAreaConvert", infraAreaConvert);
    }

    /**
     * parentId 为 null 时按 0 处理，返回全部省级
     */
    @Test
    @DisplayName("listChildren：parentId 为 null 时按省级查询")
    void listChildrenShouldTreatNullAsRoot() {
        when(infraAreaMapper.selectList(any())).thenReturn(List.of(new InfraArea()));
        when(infraAreaConvert.toRespVOList(any())).thenReturn(List.of(buildNode(1L, 0L, 1)));

        List<AreaRespVO> children = areaService.listChildren(null);

        assertThat(children).hasSize(1);
        assertThat(children.get(0).getLevel()).isEqualTo(1);
    }

    /**
     * 整棵树按 parentId 组装出三级层级
     */
    @Test
    @DisplayName("listTree：按 parentId 组装出省市区三级")
    void listTreeShouldBuildHierarchy() {
        when(infraAreaMapper.selectList(any())).thenReturn(List.of(new InfraArea(), new InfraArea(), new InfraArea()));
        when(infraAreaConvert.toRespVOList(any())).thenReturn(List.of(
                buildNode(1L, 0L, 1),
                buildNode(2L, 1L, 2),
                buildNode(3L, 2L, 3)));

        List<AreaRespVO> tree = areaService.listTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getLevel()).isEqualTo(3);
    }

    /**
     * 用户端子级查询：parentId 为 null 时按省级查询，子级为空列表
     */
    @Test
    @DisplayName("listAppChildren：parentId 为 null 时按省级查询")
    void listAppChildrenShouldTreatNullAsRoot() {
        when(infraAreaMapper.selectList(any())).thenReturn(List.of(new InfraArea()));
        when(infraAreaConvert.toAppRespVOList(any())).thenReturn(List.of(buildAppNode(1L, 0L, 1)));

        List<AreaAppRespVO> children = areaService.listAppChildren(null);

        assertThat(children).hasSize(1);
        assertThat(children.get(0).getLevel()).isEqualTo(1);
        assertThat(children.get(0).getChildren()).isEmpty();
    }

    /**
     * 用户端树查询：按 parentId 组装出省市区三级
     */
    @Test
    @DisplayName("listAppTree：按 parentId 组装出省市区三级")
    void listAppTreeShouldBuildHierarchy() {
        when(infraAreaMapper.selectList(any())).thenReturn(List.of(new InfraArea(), new InfraArea(), new InfraArea()));
        when(infraAreaConvert.toAppRespVOList(any())).thenReturn(List.of(
                buildAppNode(1L, 0L, 1),
                buildAppNode(2L, 1L, 2),
                buildAppNode(3L, 2L, 3)));

        List<AreaAppRespVO> tree = areaService.listAppTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getLevel()).isEqualTo(3);
    }

    /**
     * 构造区划节点
     *
     * @param id       区划 ID
     * @param parentId 上级区划 ID
     * @param level    层级
     * @return 区划节点
     */
    private AreaRespVO buildNode(Long id, Long parentId, Integer level) {
        AreaRespVO node = new AreaRespVO();
        node.setId(id);
        node.setParentId(parentId);
        node.setName("节点" + id);
        node.setCode(String.valueOf(id));
        node.setLevel(level);
        return node;
    }

    /**
     * 构造用户端区划节点
     *
     * @param id       区划 ID
     * @param parentId 上级区划 ID
     * @param level    层级
     * @return 用户端区划节点
     */
    private AreaAppRespVO buildAppNode(Long id, Long parentId, Integer level) {
        AreaAppRespVO node = new AreaAppRespVO();
        node.setId(id);
        node.setParentId(parentId);
        node.setName("节点" + id);
        node.setCode(String.valueOf(id));
        node.setLevel(level);
        return node;
    }
}
