package com.puti.code.analyzer.java.processor;

import com.puti.code.analyzer.java.context.GraphContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

/**
 * 方法处理器
 */
@Slf4j
public class MethodProcessor extends ExecutableProcessor<CtMethod<?>> {

    public MethodProcessor(GraphContext graphContext, DependencyInjectionResolutionSupport injectionSupport) {
        super(graphContext, injectionSupport);
    }


    @Override
    public void process(CtMethod element) {
        processExecutable(element);
    }

    @Override
    protected CtType<?> getDeclaringType(CtMethod<?> element) {
        return element.getDeclaringType();
    }

    @Override
    protected String getExecutableName(CtMethod<?> element, CtType<?> declaringType) {
        return element.getSimpleName();
    }

    @Override
    protected boolean isConstructor(CtMethod<?> element) {
        return false;
    }

    @Override
    protected boolean isStatic(CtMethod<?> element) {
        return element.isStatic();
    }

    @Override
    protected String determineVisibility(CtMethod<?> element) {
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
