package com.puti.code.documentation.global;

import com.puti.code.base.annotation.AutoKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;

/**
 * 分布式ID生成器
 * 支持雪花算法和UUID两种生成策略
 * 
 * @author diodehe
 */
@Slf4j
@Component
public class DistributedIdGenerator {
    
    /**
     * 雪花算法生成器
     */
    private final SnowflakeIdGenerator snowflakeGenerator;
    
    public DistributedIdGenerator() {
        // 获取机器ID（基于MAC地址）
        long workerId = getWorkerId();
        // 数据中心ID（可以通过配置文件配置，这里简化处理）
        long datacenterId = 1L;
        
        this.snowflakeGenerator = new SnowflakeIdGenerator(workerId, datacenterId);
        log.info("分布式ID生成器初始化完成，workerId: {}, datacenterId: {}", workerId, datacenterId);
    }
    
    /**
     * 根据策略生成ID
     * 
     * @param strategy ID生成策略
     * @return 生成的ID字符串
     */
    public String generateId(AutoKey.KeyStrategy strategy) {
        return switch (strategy) {
            case SNOWFLAKE -> String.valueOf(snowflakeGenerator.nextId());
            case UUID -> UUID.randomUUID().toString().replace("-", "");
        };
    }
    
    /**
     * 默认使用雪花算法生成ID
     * 
     * @return 生成的ID字符串
     */
    public String generateId() {
        return generateId(AutoKey.KeyStrategy.SNOWFLAKE);
    }
    
    /**
     * 获取机器ID（基于MAC地址的哈希值）
     * 
     * @return 机器ID
     */
    private long getWorkerId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            
            if (network == null) {
                return 1L;
            }
            
            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return 1L;
            }
            
            long id = 0L;
            for (byte b : mac) {
                id = (id << 8) | (b & 0xFF);
            }
            
            // 取低5位作为workerId（0-31）
            return id & 0x1F;
            
        } catch (Exception e) {
            log.warn("获取机器ID失败，使用默认值", e);
            return 1L;
        }
    }
    
    /**
     * 雪花算法ID生成器
     * Twitter的分布式自增ID算法snowflake
     */
    private static class SnowflakeIdGenerator {
        
        /**
         * 开始时间截 (2024-01-01)
         */
        private static final long TWEPOCH = 1704067200000L;
        
        /**
         * 机器id所占的位数
         */
        private static final long WORKER_ID_BITS = 5L;
        
        /**
         * 数据标识id所占的位数
         */
        private static final long DATACENTER_ID_BITS = 5L;
        
        /**
         * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
         */
        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        
        /**
         * 支持的最大数据标识id，结果是31
         */
        private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
        
        /**
         * 序列在id中占的位数
         */
        private static final long SEQUENCE_BITS = 12L;
        
        /**
         * 机器ID向左移12位
         */
        private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
        
        /**
         * 数据标识id向左移17位(12+5)
         */
        private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
        
        /**
         * 时间截向左移22位(5+5+12)
         */
        private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
        
        /**
         * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
         */
        private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
        
        /**
         * 工作机器ID(0~31)
         */
        private final long workerId;
        
        /**
         * 数据中心ID(0~31)
         */
        private final long datacenterId;
        
        /**
         * 毫秒内序列(0~4095)
         */
        private long sequence = 0L;
        
        /**
         * 上次生成ID的时间截
         */
        private long lastTimestamp = -1L;
        
        /**
         * 构造函数
         * 
         * @param workerId     工作ID (0~31)
         * @param datacenterId 数据中心ID (0~31)
         */
        public SnowflakeIdGenerator(long workerId, long datacenterId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
            }
            if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
                throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }
        
        /**
         * 获得下一个ID (该方法是线程安全的)
         * 
         * @return SnowflakeId
         */
        public synchronized long nextId() {
            long timestamp = timeGen();
            
            // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
            if (timestamp < lastTimestamp) {
                throw new RuntimeException(
                        String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
            }
            
            // 如果是同一时间生成的，则进行毫秒内序列
            if (lastTimestamp == timestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                // 毫秒内序列溢出
                if (sequence == 0) {
                    // 阻塞到下一个毫秒,获得新的时间戳
                    timestamp = tilNextMillis(lastTimestamp);
                }
            }
            // 时间戳改变，毫秒内序列重置
            else {
                sequence = 0L;
            }
            
            // 上次生成ID的时间截
            lastTimestamp = timestamp;
            
            // 移位并通过或运算拼到一起组成64位的ID
            return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                    | (datacenterId << DATACENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }
        
        /**
         * 阻塞到下一个毫秒，直到获得新的时间戳
         * 
         * @param lastTimestamp 上次生成ID的时间截
         * @return 当前时间戳
         */
        protected long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen();
            }
            return timestamp;
        }
        
        /**
         * 返回以毫秒为单位的当前时间
         * 
         * @return 当前时间(毫秒)
         */
        protected long timeGen() {
            return System.currentTimeMillis();
        }
    }
}
