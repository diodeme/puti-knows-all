package com.puti.code.base.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * 内容压缩工具类
 * 用于压缩和解压缩节点内容
 */
@Slf4j
public class ContentCompressor {
    
    /**
     * 压缩文本内容并进行Base64编码
     * 
     * @param content 原始文本内容
     * @param compressionLevel 压缩级别 (1-9)，越高压缩率越高但速度越慢
     * @return 压缩并编码后的内容
     */
    public static String compress(String content, int compressionLevel) {
        try {
            if (content == null || content.isEmpty()) {
                return "";
            }
            
            // 将文本转换为字节
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            
            // 使用Deflater进行压缩
            Deflater deflater = new Deflater(compressionLevel);
            deflater.setInput(contentBytes);
            deflater.finish();
            
            // 创建输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(contentBytes.length);
            byte[] buffer = new byte[1024];
            
            // 压缩数据
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            
            // 关闭deflater和输出流
            deflater.end();
            outputStream.close();
            
            // 获取压缩后的字节数组
            byte[] compressedBytes = outputStream.toByteArray();
            
            // 使用Base64编码为字符串
            String encoded = Base64.getEncoder().encodeToString(compressedBytes);
            
            // 记录压缩率
            int originalSize = contentBytes.length;
            int compressedSize = encoded.length();
            double ratio = originalSize > 0 ? (double)(originalSize - compressedSize) / originalSize * 100 : 0;
            log.debug("内容压缩: 原始大小={}字节, 压缩后={}字节, 压缩率={}", originalSize, compressedSize, ratio);
            
            return encoded;
        } catch (IOException e) {
            log.error("压缩内容失败: {}", e.getMessage());
            // 如果压缩失败，返回空字符串
            return "";
        }
    }
    
    /**
     * 使用默认压缩级别(9)压缩文本内容
     * 
     * @param content 原始文本内容
     * @return 压缩并编码后的内容
     */
    public static String compress(String content) {
        return compress(content, 9);
    }
    
    /**
     * 解压缩Base64编码的压缩内容
     * 
     * @param compressedContent Base64编码的压缩内容
     * @return 解压缩后的原始内容
     */
    public static String decompress(String compressedContent) {
        try {
            if (compressedContent == null || compressedContent.isEmpty()) {
                return "";
            }
            
            // Base64解码
            byte[] compressedBytes = Base64.getDecoder().decode(compressedContent);
            
            // 使用Inflater解压缩
            Inflater inflater = new Inflater();
            inflater.setInput(compressedBytes);
            
            // 创建输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedBytes.length * 2);
            byte[] buffer = new byte[1024];
            
            // 解压缩数据
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            
            // 关闭inflater和输出流
            inflater.end();
            outputStream.close();
            
            // 将解压缩后的字节数组转换为字符串
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解压缩内容失败: {}", e.getMessage());
            // 如果解压缩失败，返回空字符串
            return "";
        }
    }
    
    /**
     * 判断内容是否已经被压缩
     * 
     * @param content 内容
     * @return 是否已压缩
     */
    public static boolean isCompressed(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        try {
            // 尝试进行Base64解码，如果成功且解码后的内容看起来像压缩数据，则认为是压缩过的
            byte[] decoded = Base64.getDecoder().decode(content);
            // 检查解码后的前两个字节是否符合zlib压缩格式的标记
            return decoded.length >= 2 && 
                   ((decoded[0] & 0xFF) == 0x78 && 
                   ((decoded[1] & 0xFF) == 0x01 || 
                    (decoded[1] & 0xFF) == 0x9C || 
                    (decoded[1] & 0xFF) == 0xDA));
        } catch (IllegalArgumentException e) {
            // 如果解码失败，说明不是有效的Base64编码，因此不是压缩过的
            return false;
        }
    }
}
