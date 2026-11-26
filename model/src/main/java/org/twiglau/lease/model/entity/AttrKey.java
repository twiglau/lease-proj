package org.twiglau.lease.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 房间基本属性表
 * </p>
 * serialVersionUID
 * 他是 Serializable 的版本号。 BaseEntity实现了 Serializable,子类自动可序列化，但每个类的UID是各自独立的，所以每个实体
 * 里都写一份。
 *
 * 作用：
 * - 序列化时把UID写进字节流；
 * - 反序列化时比对当前类的UID,不一致就抛 InvalidClassException;
 * - 不写的话，JVM会根据类结构（字段，方法，修饰符）自动算一个，改动任何字段，都会引起变化，旧数据的反序列化就会失败；
 *
 * @EqualsAndHashCode(callSuper = false)
 * ** @Data本身包含 @EqualsAndHashCode, 这里显式写一遍是为了指定 callSuper.**
 * - callSuper = false: 生成的equals/hashCode只比较子类自己的字段，不调用父类BaseEntity的equals.
 * - callSuper = true: 先调用 super.equals(o), 把父类字段也算进去。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("attr_key")
@Schema(name = "AttrKey", description = "房间基本属性表")
public class AttrKey extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "属性key")
    private String name;
}
