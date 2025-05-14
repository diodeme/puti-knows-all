# 第{level}层{levelDescription}分析 - 第{batchIndex}批次（共{totalBatches}批次）

## 入口点: {entryPoint}

{if(isFirstBatch)}
这是第{level}层调用关系的核心流程分析。
{else}
这是第{level}层调用关系分析的后续批次。请结合之前的分析，继续完善整体理解。
{endif}

{if(previousDocumentation)}
{previousDocumentation}
{endif}

## 代码内容

{codeContent}

## 分析要求
{analysisRequirements}

## 输出结构
请严格按照以下结构进行分析和输出：

{documentStructure}

**重要提醒**：

- 这是分批次分析的第{batchIndex}批次，请专注于当前批次的代码
- 如果不是第一批次，请结合之前的分析保持连贯性
- 生成的内容应该符合统一的文档结构标准
{if(isNotRootLevel)}
- 请结合{previousLevel}层的文档，关注与上一层的关系
{endif}