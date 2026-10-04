package com.wxy.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

/**
 * 网关路由配置测试：校验 application.yml 里的路由契约。
 *
 * <p>断言的是「配置真实生效后的结果」，而不是抄一遍 YAML：
 * 路由目标必须是 {@code lb://{服务名}}（不带 {@code -biz}），
 * 断言必须是 {@code Path=/api/{服务名}/**}，过滤器必须是 {@code StripPrefix=2}。
 * 之前用 RewritePath 手写 {@code /**} 正则的写法会在这里被挡下。
 *
 * <p>测试关闭 Nacos 服务发现，不依赖任何外部环境。
 *
 * @author wxy
 * @date 2026/10/03
 */
@SpringBootTest(properties = "spring.cloud.nacos.discovery.enabled=false")
class GatewayRouteConfigTest {

    /** 路由定义加载器，读到的就是网关实际生效的路由配置 */
    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    /**
     * 三条路由都必须符合「/api/{服务名}/** → lb://{服务名}」的约定
     */
    @Test
    void routesShouldFollowServiceNameContract() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(routes).isNotNull();
        assertThat(routes).hasSize(3);

        assertRoute(routes, "infra-route", "infra");
        assertRoute(routes, "rental-route", "rental");
        assertRoute(routes, "ai-agent-route", "ai-agent");
    }

    /**
     * 断言单条路由的目标地址、路径断言与前缀剥离过滤器
     *
     * @param routes      全部路由定义
     * @param routeId     路由 id
     * @param serviceName 服务名（Nacos 注册名）
     */
    private static void assertRoute(List<RouteDefinition> routes, String routeId, String serviceName) {
        RouteDefinition route = routes.stream()
                .filter(definition -> routeId.equals(definition.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少路由配置：" + routeId));

        assertThat(route.getUri()).hasToString("lb://" + serviceName);

        assertThat(route.getPredicates()).hasSize(1);
        PredicateDefinition predicate = route.getPredicates().get(0);
        assertThat(predicate.getName()).isEqualTo("Path");
        assertThat(predicate.getArgs()).containsEntry("_genkey_0", "/api/" + serviceName + "/**");

        assertThat(route.getFilters()).hasSize(1);
        FilterDefinition filter = route.getFilters().get(0);
        assertThat(filter.getName()).isEqualTo("StripPrefix");
        assertThat(filter.getArgs()).containsEntry("_genkey_0", "2");
    }
}
