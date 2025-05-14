package com.puti.code.analyzer.java.support;

import spoon.reflect.reference.CtTypeReference;

import java.util.Set;

/**
 * 一个用于检查Spoon类型引用是否属于JDK或相关标准库的工具类。
 */
public final class JdkClassChecker {

    /**
     * 核心JDK包 + JRE捆绑的标准API包前缀。
     * 这些通常被认为是“JDK自带的”。
     */
    private static final Set<String> JDK_STANDARD_PREFIXES = Set.of(
            // 1. 核心 Java API
            "java.",

            // 2. Java 标准扩展 API (Swing, JNDI, SQL, XML, etc.)
            "javax.",

            // 3. W3C DOM 和 SAX API (随JDK捆绑)
            "org.w3c.dom.",
            "org.xml.sax.",

            // 4. CORBA API (随JDK捆绑, 现已废弃但仍存在)
            "org.omg.",

            // 5. Sun/Oracle 的内部非公开API (通常不建议直接使用，但确实是JDK的一部分)
            "com.sun.",
            "sun.",
            
            // 6. Java 9+ 模块化后的内部API
            "jdk.internal.",

            "jakarta."
    );


    /**
     * 私有构造函数，防止实例化。
     */
    private JdkClassChecker() {}

    /**
     * 检查给定的类型引用是否指向一个标准的JDK类。
     *
     * @param typeRef Spoon 的类型引用，可以为 null。
     * @return 如果是JDK类则返回 true，否则返回 false。
     */
    public static boolean isJdkClass(CtTypeReference<?> typeRef) {
        if (typeRef == null) {
            return false;
        }
        String qualifiedName = typeRef.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }

        return JDK_STANDARD_PREFIXES.stream().anyMatch(qualifiedName::startsWith);
    }
}