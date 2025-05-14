package com.puti.code.ai.documentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话上下文管理类
 * 用于手动管理多轮对话的上下文信息，不依赖Spring AI的自动记忆管理
 *
 * @author diodehe
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationContext {
    
    /**
     * 初始化prompt，每次对话都会携带
     */
    private String initialPrompt;
    
    /**
     * 历史对话轮次记录
     */
    private List<ConversationRound> rounds;
    
    /**
     * 文档生成上下文
     */
    private DocumentationGenerationContext docContext;
    
    /**
     * 单轮对话记录
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConversationRound {
        /**
         * 轮次编号
         */
        private int roundNumber;
        
        /**
         * 当前轮次的具体任务prompt
         */
        private String userPrompt;
        
        /**
         * AI的响应内容
         */
        private String aiResponse;
        
        /**
         * 时间戳
         */
        private long timestamp;
    }
    
    /**
     * 添加新的对话轮次
     *
     * @param roundNumber 轮次编号
     * @param userPrompt  用户prompt
     * @param aiResponse  AI响应
     */
    public void addRound(int roundNumber, String userPrompt, String aiResponse) {
        if (rounds == null) {
            rounds = new ArrayList<>();
        }
        rounds.add(ConversationRound.builder()
                .roundNumber(roundNumber)
                .userPrompt(userPrompt)
                .aiResponse(aiResponse)
                .timestamp(System.currentTimeMillis())
                .build());
    }
    
    /**
     * 获取历史对话摘要，用于构建下一轮的上下文
     *
     * @return 历史对话摘要字符串
     */
    public String getHistorySummary() {
        if (rounds == null || rounds.isEmpty()) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("\n\n## 前面轮次的分析结果：\n");
        
        for (ConversationRound round : rounds) {
            summary.append(String.format("### 第%d轮分析结果:\n", round.getRoundNumber()));
            summary.append(round.getAiResponse()).append("\n\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 获取对话轮次数量
     *
     * @return 轮次数量
     */
    public int getRoundCount() {
        return rounds != null ? rounds.size() : 0;
    }
    
    /**
     * 获取最后一轮对话
     *
     * @return 最后一轮对话，如果没有则返回null
     */
    public ConversationRound getLastRound() {
        if (rounds == null || rounds.isEmpty()) {
            return null;
        }
        return rounds.get(rounds.size() - 1);
    }
    
    /**
     * 清空历史对话记录
     */
    public void clearRounds() {
        if (rounds != null) {
            rounds.clear();
        }
    }
}
