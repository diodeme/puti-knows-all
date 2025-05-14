我已经使用代码图谱解析了mall项目的代码，现在我需要你实现一个自检机制
即开发-->运行-->评估-->反馈的循环


1.图谱解析位于puti-code目录的code-graph-app下的projectHandler，现在我
  需要你先在puti-code中实现一个可以查询图谱的调试接口，方便你进行验证（注意，code-graph-app不是spring应用，你就生成
  一个main函数，然后每次实时用gradle运行吧，举个例子.\gradlew run -PmyMain="com.puti.code.app.ProjectHandler"）2.然后我需要你按照claude code 的skill规范，在_docs目录下生成一个图谱调
  试skill，skill里主要是先声明图谱生成了哪些关系（位于puti_code的_docs/nebula_ddl_latest.sql），然后声明claude
  code怎么选取mall项目的代表性节点查找节点的上下游链路，并与图谱中查到的链路做正确性比对，最后给出比对结果，正确性
  比对的内容不应该在skill中定义，因为不同需求的正确性比对内容不一致，你应该在skill中声明调用该skill时需要用户给出正
  确性评判因素，注意：这里用户只需要给出评判因素，claude code应该根据评判因素自动选择多个节点进行比对评估，自动选取节点必须尽可能详细完善，覆盖到各种情况，不能数量太少。先给我一个完整的方案，方案生成在_docs目录下