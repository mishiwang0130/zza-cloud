package com.wxy.common.webmvc.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局异常处理器的测试：验证各类异常对应的 HTTP 状态码与响应体结构。
 *
 * @author wxy
 * @date 2026/10/02
 */
class GlobalExceptionHandlerTest {

    /** 不启动 Spring 容器，直接用独立 MockMvc 验证异常映射 */
    private MockMvc mockMvc;

    /**
     * 构造独立 MockMvc，并挂上被测的全局异常处理器
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("业务异常返回 HTTP 200，错误码原样返回")
    void shouldReturn200WhenBizException() throws Exception {
        mockMvc.perform(get("/test/biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonErrorConstant.NOT_FOUND.code()))
                .andExpect(jsonPath("$.msg").value(CommonErrorConstant.NOT_FOUND.msg()));
    }

    @Test
    @DisplayName("未登录异常返回 HTTP 401")
    void shouldReturn401WhenUnauthorized() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorConstant.UNAUTHORIZED.code()));
    }

    @Test
    @DisplayName("参数校验失败返回 HTTP 400，并带上字段提示")
    void shouldReturn400WhenValidationFailed() throws Exception {
        mockMvc.perform(post("/test/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorConstant.PARAM_ERROR.code()))
                .andExpect(jsonPath("$.msg").value("名称不能为空"));
    }

    @Test
    @DisplayName("未捕获异常返回 HTTP 500，响应体不暴露内部细节")
    void shouldReturn500WhenUncaughtException() throws Exception {
        mockMvc.perform(get("/test/system"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CommonErrorConstant.SYSTEM_ERROR.code()))
                .andExpect(jsonPath("$.msg").value(CommonErrorConstant.SYSTEM_ERROR.msg()));
    }

    /**
     * 测试用控制器：只负责抛出各类异常
     *
     * @author wxy
     * @date 2026/10/02
     */
    @RestController
    static class TestController {

        /**
         * 抛出业务异常
         *
         * @return 不会返回，直接抛异常
         */
        @GetMapping("/test/biz")
        public Result<Void> biz() {
            throw new BizException(CommonErrorConstant.NOT_FOUND);
        }

        /**
         * 抛出未登录异常
         *
         * @return 不会返回，直接抛异常
         */
        @GetMapping("/test/unauthorized")
        public Result<Void> unauthorized() {
            throw new UnauthorizedException();
        }

        /**
         * 抛出未捕获异常
         *
         * @return 不会返回，直接抛异常
         */
        @GetMapping("/test/system")
        public Result<Void> system() {
            throw new IllegalStateException("模拟系统异常");
        }

        /**
         * 接收带校验的请求体
         *
         * @param reqVO 请求体
         * @return 成功响应
         */
        @PostMapping("/test/create")
        public Result<Void> create(@RequestBody @Valid CreateReqVO reqVO) {
            return Result.success();
        }
    }

    /**
     * 测试用请求体
     *
     * @author wxy
     * @date 2026/10/02
     */
    static class CreateReqVO {

        /** 名称，必填 */
        @NotBlank(message = "名称不能为空")
        private String name;

        /**
         * 获取名称
         *
         * @return 名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置名称
         *
         * @param name 名称
         */
        public void setName(String name) {
            this.name = name;
        }
    }
}
