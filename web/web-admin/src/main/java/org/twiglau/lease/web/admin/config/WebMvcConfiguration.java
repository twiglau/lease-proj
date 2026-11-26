package org.twiglau.lease.web.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.twiglau.lease.web.admin.convert.StringToBaseEnumConverterFactory;

/**
 * WebMvcConfigurer的实现类，用于自定义Spring MVC的配置。
 * 通过该类，可以配置自定义的转换工厂，以支持将字符串转换为枚举类型的绑定；
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {
    /**
     * 自动注入StringToBaseEnumConverterFactory
     * - 该转换器工厂用于将字符串转换为基底枚举类型。
     * - 这种转换对于处理API请求参数或响应数据中枚举类型的序列化和反序列化非常有用。
     */
    private final StringToBaseEnumConverterFactory converterFactory;

    /**
     * 注册自定义的转换工厂
     * - 此方法重写了WebMvcConfigurer接口的 addFormatters 方法，用于向Spring MVC的格式化程序注册表中添加自定义的转换工厂。
     * - 通过这种方式，Spring MVC在处理请求和响应时，可以使用该工厂提供的转换器进行数据类型的转换。
     *
     * @param registry 格式化程序注册表，用于注册和管理转换器工厂。
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(converterFactory);
    }
}
