package com.wxy.ai.agent.biz.tool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 租赁费用试算工具（本地实现，不需要远程调用）。
 *
 * <p>用户常问「这套房第一次要准备多少钱」，答案就是押金加首期租金。这里只做算术，
 * 具体金额仍以合同为准——所以返回文本里带上这句话，避免模型把它说成最终结论。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class RentCalculatorTools {

    /**
     * 试算押金与首期支出
     *
     * @param rentMonthly   月租金（元）
     * @param depositMonths 押金月数，可空（默认 1 个月）
     * @param rentMonths    首期付几个月租金，可空（默认 1 个月）
     * @return 给模型看的试算结果文本
     */
    @Tool(name = "calculateFirstPayment",
            description = "根据月租金、押金月数、首期付租月数试算押金与首期总支出。"
                    + "当用户问「第一次要交多少钱 / 押金多少 / 首期多少钱」时调用。")
    public String calculateFirstPayment(
            @ToolParam(description = "月租金，单位元") BigDecimal rentMonthly,
            @ToolParam(required = false, description = "押金月数，默认 1") Integer depositMonths,
            @ToolParam(required = false, description = "首期付几个月租金，默认 1") Integer rentMonths) {
        if (rentMonthly == null || rentMonthly.signum() <= 0) {
            return "月租金不合法，请确认真实租金后再试算。";
        }
        int deposit = depositMonths == null || depositMonths < 0 ? 1 : depositMonths;
        int firstRentMonths = rentMonths == null || rentMonths < 1 ? 1 : rentMonths;
        BigDecimal depositAmount = rentMonthly.multiply(BigDecimal.valueOf(deposit))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal firstRent = rentMonthly.multiply(BigDecimal.valueOf(firstRentMonths))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = depositAmount.add(firstRent).setScale(2, RoundingMode.HALF_UP);
        return "按押金 %d 个月、首期付 %d 个月租金试算：押金 %s 元，首期租金 %s 元，合计约 %s 元。"
                .formatted(deposit, firstRentMonths, depositAmount, firstRent, total)
                + "\n（试算结果仅供参考，最终金额以合同与公寓管家确认为准。）";
    }
}
