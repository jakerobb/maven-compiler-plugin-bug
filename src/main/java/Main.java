import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.manager.CompilerManager;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        PlexusContainer container = new DefaultPlexusContainer();

        cleanOutputDirectory();

        CompilerManager compilerManager = container.lookup(CompilerManager.class);
        var compiler = compilerManager.getCompiler("javac");
        var config = new CompilerConfiguration();
        config.setFork(true);
        config.setSourceVersion("12");
        config.setTargetVersion("12");
        config.setWorkingDirectory(new File("."));
        config.addSourceLocation("project-to-compile/src");
        config.setOutputLocation("project-to-compile/out");
        CompilerResult compilerResult = compiler.performCompile(config);
        if (compilerResult.isSuccess()) {
            System.out.println("Compilation succeeded!");
        } else {
            System.out.printf("Compilation failed with %d messages%n", compilerResult.getCompilerMessages().size());
            for (CompilerMessage compilerMessage : compilerResult.getCompilerMessages()) {
                System.out.println(compilerMessage);
            }
        }
        container.dispose();
    }

    private static void cleanOutputDirectory() {
        File outputDirectory = new File("project-to-compile/out");
        deleteRecursively(outputDirectory);
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
