package com.puti.code.app.decompiler;

import org.benf.cfr.reader.Main;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * CFR 反编译器封装。将 JAR 文件反编译为 .java 源文件。
 */
public class CFRDecompiler {

    public void decompile(String inputPath, String outputPath, String[] classpath) {
        if (classpath != null && classpath.length > 0) {
            Main.main(new String[]{inputPath, "--outputdir", outputPath, "--extraclasspath",
                    Arrays.stream(classpath).collect(Collectors.joining(":"))});
        } else {
            Main.main(new String[]{inputPath, "--outputdir", outputPath});
        }
    }
}
