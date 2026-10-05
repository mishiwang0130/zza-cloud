package com.wxy.ai.agent.biz.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 费用试算工具单元测试：纯本地算术，不依赖模型与中间件。
 *
 * @author wxy
 * @date 2026/10/05
 */
class RentCalculatorToolsTest {

    /** 被测工具 */
    private final RentCalculatorTools tools = new RentCalculatorTools();

    /**
     * 默认押一付一：押金与首期租金都等于一个月租金
     */
    @Test
    @DisplayName("试算：默认押一付一")
    void shouldCalculateDefaultDepositAndFirstRent() {
        String result = tools.calculateFirstPayment(new BigDecimal("2000"), null, null);

        assertThat(result).contains("押金 2000").contains("首期租金 2000").contains("合计约 4000");
    }

    /**
     * 押二付三：按传入月数放大
     */
    @Test
    @DisplayName("试算：押二付三")
    void shouldCalculateWithGivenMonths() {
        String result = tools.calculateFirstPayment(new BigDecimal("1500.50"), 2, 3);

        assertThat(result).contains("押金 3001.00").contains("首期租金 4501.50").contains("合计约 7502.50");
    }

    /**
     * 租金不合法时给出提示而不是抛异常：工具抛异常会让整轮回答失败
     */
    @Test
    @DisplayName("试算：租金非法时返回提示")
    void shouldRejectInvalidRent() {
        assertThat(tools.calculateFirstPayment(null, 1, 1)).contains("月租金不合法");
        assertThat(tools.calculateFirstPayment(BigDecimal.ZERO, 1, 1)).contains("月租金不合法");
    }
}
