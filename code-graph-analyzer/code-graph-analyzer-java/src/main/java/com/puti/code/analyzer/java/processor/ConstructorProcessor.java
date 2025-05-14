package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtType;

/**
 * 构造函数处理器
 */
@Slf4j
public class ConstructorProcessor extends ExecutableProcessor<CtConstructor<?>> {


    public ConstructorProcessor(GraphContext graphContext, DependencyInjectionResolutionSupport injectionSupport) {
        super(graphContext, injectionSupport);
    }

    @Override
    public void process(CtConstructor element) {
        processExecutable(element);
    }

    @Override
    protected CtType<?> getDeclaringType(CtConstructor<?> element) {
        return element.getDeclaringType();
    }

    @Override
    protected String getExecutableName(CtConstructor<?> element, CtType<?> declaringType) {
        return declaringType.getSimpleName();
    }

    @Override
    protected boolean isConstructor(CtConstructor<?> element) {
        return true;
    }

    @Override
    protected boolean isStatic(CtConstructor<?> element) {
        return false;
    }

    @Override
    protected String determineVisibility(CtConstructor<?> element) {
        String visibility = "default";
        if (element.isPublic()) {
            visibility = "public";
        } else if (element.isPrivate()) {
            visibility = "private";
        } else if (element.isProtected()) {
            visibility = "protected";
        }
        return visibility;
    }

    @Override
    protected Logger getLogger(){
        return log;
    }
}
