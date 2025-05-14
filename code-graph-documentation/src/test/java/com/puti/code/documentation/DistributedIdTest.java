package com.puti.code.documentation;

import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.documentation.global.DistributedIdGenerator;
import com.puti.code.documentation.global.AutoKeyInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 分布式ID生成测试
 * 
 * @author diodehe
 */
@Slf4j
public class DistributedIdTest {

    @Test
    public void testIdGeneration() {
        DistributedIdGenerator idGenerator = new DistributedIdGenerator();

        // 测试雪花算法ID生成
        String snowflakeId = idGenerator.generateId();
        log.info("生成的雪花算法ID: {}", snowflakeId);
        
        // 测试UUID生成
        String uuidId = idGenerator.generateId(com.puti.code.base.annotation.AutoKey.KeyStrategy.UUID);
        log.info("生成的UUID: {}", uuidId);
        
        // 验证ID的唯一性
        for (int i = 0; i < 10; i++) {
            String id1 = idGenerator.generateId();
            String id2 = idGenerator.generateId();
            assertNotEquals(id1, id2, "生成的ID不应该重复");
            log.info("第{}次生成的ID: {}, {}", i + 1, id1, id2);
        }
    }
    
    @Test
    public void testAutoKeyAnnotation() {
        DistributedIdGenerator idGenerator = new DistributedIdGenerator();
        AutoKeyInterceptor interceptor = new AutoKeyInterceptor();
        ReflectionTestUtils.setField(interceptor, "idGenerator", idGenerator);

        // 创建一个Documentation对象，不设置ID
        Documentation doc = Documentation.builder()
                .entryPointId("test-entry-point")
                .entryPointName("测试入口点")
                .title("测试说明书")
                .summary("这是一个测试说明书")
                .content("测试内容")
                .level(1)
                .status(Documentation.DocumentationStatus.PENDING)
                .version(1)
                .isFinalVersion(false)
                .projectId("test-project")
                .branchName("main")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        log.info("插入前的Documentation ID: {}", doc.getId());

        ReflectionTestUtils.invokeMethod(interceptor, "processAutoKey", doc);
        log.info("自动填充后的Documentation ID: {}", doc.getId());

        // 验证ID已经被自动生成
        assertNotNull(doc.getId(), "ID应该被自动生成");
        assertFalse(doc.getId().isEmpty(), "ID不应该为空");
    }
}
