package org.twiglau.lease.web.admin.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;
import org.twiglau.lease.model.enums.BaseEnum;

@Component
public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    /**
     * 为指定的枚举类型生成转换器示例
     * 转换逻辑基于枚举值的代码code<->给定字符串进行匹配
     * @param targetType 转换目标的枚举类型Class对象
     * @return  字符串到指定枚举类型的转换器实例
     * @param <T> 目标枚举类型的泛型参数
     */
    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {

        return code -> {
            // 遍历目标枚举类型的所有实例，寻找代码匹配的枚举值
            for (T enumType : targetType.getEnumConstants()) {
                if(enumType.getCode().toString().equals(code)) {
                    return enumType;
                }
            }
            // 若无匹配项，抛出异常指示错误的代码参数
            throw new IllegalArgumentException("未找到匹配的枚举值, code: " + code);
        };
    }
}
