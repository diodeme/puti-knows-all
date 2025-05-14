package com.puti.code.documentation.global;

import com.puti.code.base.annotation.AutoKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * MyBatis自动填充分布式ID拦截器
 * 在执行INSERT操作时，自动为带有@AutoKey注解的字段生成分布式ID
 *
 * @author diodehe
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AutoKeyInterceptor implements Interceptor {

    @Autowired
    private DistributedIdGenerator idGenerator;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        if (Objects.isNull(parameter)) {
            return invocation.proceed();
        }

        // 只处理INSERT操作
        if (SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType())) {
            processAutoKey(parameter);
        }

        return invocation.proceed();
    }

    /**
     * 处理自动生成ID
     *
     * @param parameter 参数对象
     */
    @SuppressWarnings("rawtypes")
    private void processAutoKey(Object parameter) {
        if (parameter == null) {
            return;
        }
        if (parameter instanceof MapperMethod.ParamMap paramMap) {
            for (Object o : paramMap.values()) {
                try {
                    // 处理集合类型
                    if (o instanceof Collection<?> collection) {
                        for (Object item : collection) {
                            fillAutoKey(item);
                        }
                    } else {
                        // 处理单个对象
                        fillAutoKey(o);
                    }
                } catch (Exception e) {
                    log.error("自动填充ID时发生错误", e);
                }
            }
        } else {
            fillAutoKey(parameter);
        }
    }

    /**
     * 为对象填充自动生成的ID
     *
     * @param obj 目标对象
     */
    private void fillAutoKey(Object obj) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            AutoKey autoKey = field.getAnnotation(AutoKey.class);
            if (autoKey != null) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    // 如果字段值为空，则生成ID
                    if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
                        String id = idGenerator.generateId(autoKey.strategy());
                        field.set(obj, id);
                        log.debug("为字段 {}.{} 生成ID: {}", clazz.getSimpleName(), field.getName(), id);
                    }
                } catch (IllegalAccessException e) {
                    log.error("访问字段 {}.{} 时发生错误", clazz.getSimpleName(), field.getName(), e);
                }
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以在这里设置一些配置属性
    }
}
