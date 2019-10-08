import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReproducerTest {
    private static PlexusContainer container;
    private static CompilerManager compilerManager;
    private static Compiler compiler;
    private static PrintStream originalSystemOut;
    private static ByteArrayOutputStream capturedSystemOut = new ByteArrayOutputStream();
    private static ByteArrayOutputStream capturedSystemErr = new ByteArrayOutputStream();


    private static final Matcher<CompilerMessage> KIND_IS_ERROR = new TypeSafeMatcher<>() {
        @Override
        protected boolean matchesSafely(CompilerMessage compilerMessage) {
            return compilerMessage.getKind() == Kind.ERROR;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a compilerMessage with kind=").appendValue(Kind.ERROR);
        }
    };

    @BeforeAll
    static void setupClass() throws Exception {
        container = new DefaultPlexusContainer();
        compilerManager = container.lookup(CompilerManager.class);
        compiler = compilerManager.getCompiler("javac");

        originalSystemOut = System.out;
        System.setOut(new PrintStream(capturedSystemOut));
        System.setErr(new PrintStream(capturedSystemErr));
    }

    @AfterAll
    static void tearDown() {
        container.dispose();
    }

    @Test
    void testSuccessfulWhenCompilingOutOfProcess_BugIsResolvedIfThisPasses() throws Exception {
        tryCompilation(true);
    }

    @Test
    void testSuccessfulWhenCompilingInProcess() throws Exception {
        tryCompilation(false);
    }

    private static void tryCompilation(boolean fork) throws Exception {
        cleanOutputDirectory();

        var config = new CompilerConfiguration();
        config.setFork(fork);
        config.setSourceVersion("12");
        config.setTargetVersion("12");
        config.setWorkingDirectory(new File("."));
        config.addSourceLocation("project-to-compile/src");
        config.setOutputLocation("project-to-compile/out");
        var compilerResult = compiler.performCompile(config);
        if (compilerResult.isSuccess()) {
            originalSystemOut.println("Compilation succeeded. This is unexpected, but helpful! It means that the underlying compiler issue has been resolved.");
            return;
        } else {
            originalSystemOut.printf("Compilation failed with %d messages:%n", compilerResult.getCompilerMessages().size());

            boolean foundSOE = false;
            for (CompilerMessage compilerMessage : compilerResult.getCompilerMessages()) {
                originalSystemOut.printf("* %s%n", compilerMessage);
                if (compilerMessage.getMessage().contains("StackOverflowError")) {
                    foundSOE = true;
                }
            }

            if (foundSOE) {
                originalSystemOut.println("Found 'StackOverflowError' in a compiler message.");
            }

            assertThat("There should be a compiler error.", compilerResult.getCompilerMessages(), hasItem(isCompilerErrorMessage()));


            if (!foundSOE) {
                String systemOut = capturedSystemOut.toString();
                if (systemOut.contains("StackOverflowError")) {
                    originalSystemOut.println("Found 'StackOverflowError' in javac's stdout.");
                    foundSOE = true;
                }
            }

            if (!foundSOE) {
                String systemErr = capturedSystemErr.toString();
                if (systemErr.contains("StackOverflowError")) {
                    originalSystemOut.println("Found 'StackOverflowError' in javac's stderr.");
                    foundSOE = true;
                }
            }

            assertTrue(foundSOE);
        }
        container.dispose();
    }

    private static Matcher<CompilerMessage> isCompilerErrorMessage() {
        return KIND_IS_ERROR;
    }

    private static void cleanOutputDirectory() {
        var outputDirectory = new File("project-to-compile/out");
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
