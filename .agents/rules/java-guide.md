---
trigger: always_on
---

1.优先考虑使用lombok的@Data,@Getter,@Setter,@AllArgsConstructor,@NoArgsConstructor，而不是手写getter、setter、toString和构造函数

2.优先考虑使用lombok的@Slf4j声明logger

3.应该在imort中导入路径，在代码中直接使用对应类
严禁直接用全限定名（除非有类名冲突的情况）来引用类
正确的写法
```java
import com.a.b.classA;

classA.function();
```
错误的写法
```sql
com.a.b.classA.function();
```

4.项目应该采用分层架构：
command类型：controller/job --> (facade) --> service --> dao --> mapper
read类型：controller/job --> (service) --> dao --> mapper
1.针对command，如果需要对service编排，则生成一层facade，否则可以直接调用service
2.针对read，如果存在可复用的数据加工逻辑或者权限校验等，则可以在dao上包装一层service，否则可以直接调用dao
3.不管什么类型，service都不能直接调用mapper，必须先调用dao，然后调用mapper
4.对象类型转换，一般为
controller: VO --> (DTO) --> DO
job: Job(本质也是个DO) --> (DTO) --> DO
如果service层涉及到多表计算，则需要单独定义DTO
5.统一使用 MapStruct 进行对象转换（VO <--> DTO <--> DO）。禁止在业务代码中出现大量的 setter/getter 赋值代码。禁止使用 BeanUtils.copyProperties（性能差且无法在编译期检查字段映射）。