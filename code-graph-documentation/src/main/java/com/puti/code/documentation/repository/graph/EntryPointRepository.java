package com.puti.code.documentation.repository.graph;

import com.puti.code.base.model.MethodInfo;
import com.puti.code.documentation.dto.DocumentationGenerationDto;
import com.puti.code.documentation.repository.graph.support.GraphSupport;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 入口点服务
 * 负责查询和管理代码图谱中的入口节点
 *
 * @author diodehe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntryPointRepository {

    private final GraphQueryRepository graphQueryRepository;

    /**
     * 获取所有入口点
     *
     * @return 入口点列表
     */
    public List<MethodInfo> getAllEntryPoints(DocumentationGenerationDto dto) {
        try {
            log.info("开始查询所有入口点");
            List<MethodInfo> entryPoints = graphQueryRepository.getEntryPoints(dto.getProjectId(), dto.getBranchName()).stream()
                    .map(GraphSupport::parseGraphQueryNodeToMethodInfo)
                    .filter(methodInfo -> methodInfo != null)
                    .toList();

            log.info("成功查询到 {} 个入口点", entryPoints.size());
            return entryPoints;

        } catch (Exception e) {
            log.error("查询所有入口点时发生错误", e);
            return List.of();
        }
    }

    /**
     * 根据ID获取入口点信息
     *
     * @param entryPointId 入口点ID
     * @return 入口点信息，如果不存在返回null
     */
    public MethodInfo getEntryPointById(String entryPointId) {
        try {
            log.debug("查询入口点: {}", entryPointId);
            MethodInfo methodInfo = graphQueryRepository.findNodeById(entryPointId)
                    .filter(node -> graphQueryRepository.isEntryPoint(node.getId()))
                    .map(GraphSupport::parseGraphQueryNodeToMethodInfo)
                    .orElse(null);
            log.debug("成功获取入口点信息: {}", methodInfo != null ? methodInfo.getMethodName() : "null");
            return methodInfo;

        } catch (Exception e) {
            log.error("根据ID查询入口点时发生错误: {}", entryPointId, e);
            return null;
        }
    }

    /**
     * 获取入口点总数
     *
     * @return 入口点总数
     */
    public int getTotalEntryPointsCount() {
        try {
            int count = (int) graphQueryRepository.countEntryPoints();
            log.debug("入口点总数: {}", count);
            return count;

        } catch (Exception e) {
            log.error("获取入口点总数时发生错误", e);
            return 0;
        }
    }

    /**
     * 检查指定方法是否为入口点
     *
     * @param methodId 方法ID
     * @return 是否为入口点
     */
    public boolean isEntryPoint(String methodId) {
        try {
            return graphQueryRepository.isEntryPoint(methodId);

        } catch (Exception e) {
            log.error("检查方法 {} 是否为入口点时发生错误", methodId, e);
            return false;
        }
    }
}
