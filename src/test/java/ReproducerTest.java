import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReproducerTest {
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

    private static final String REQUIRED_ERROR_STRING = "StackOverflowError";
    private static final String PROBLEMATIC_FILE_NAME = "ThisWillFailToCompile.java";
    private static PlexusContainer container;
    private static CompilerManager compilerManager;
    private static Compiler compiler;
    private static PrintStream originalSystemOut;

    private ByteArrayOutputStream capturedSystemOut = new ByteArrayOutputStream();
    private ByteArrayOutputStream capturedSystemErr = new ByteArrayOutputStream();

    @BeforeAll
    static void setupClass() throws Exception {
        container = new DefaultPlexusContainer();
        compilerManager = container.lookup(CompilerManager.class);
        compiler = compilerManager.getCompiler("javac");

        originalSystemOut = System.out;
    }

    @AfterAll
    static void tearDown() {
        container.dispose();
    }

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(capturedSystemOut));
        System.setErr(new PrintStream(capturedSystemErr));
    }

    @Test
    void testSuccessfulWhenCompilingOutOfProcess_BugIsResolvedIfThisPasses() throws Exception {
        var result = tryCompilation(true);
        Assumptions.assumeFalse(result.isSuccess(), "This test is not applicable if the compilation succeeds.");
        assertTrue(outputContainsString(result, REQUIRED_ERROR_STRING), "The output must contain the text " + REQUIRED_ERROR_STRING);
    }

    @Test
    void testSuccessfulWhenCompilingInProcess() throws Exception {
        var result = tryCompilation(false);
        Assumptions.assumeFalse(result.isSuccess(), "This test is not applicable if the compilation succeeds.");
        assertTrue(outputContainsString(result, REQUIRED_ERROR_STRING), "The output must contain the text " + REQUIRED_ERROR_STRING);
    }

    @Test
    void testCompilationSucceeds() throws Exception {
        var result = tryCompilation(false);
        assertTrue(result.isSuccess(), "This test is expected to fail even after the bug is fixed; it verifies that the testing approach is valid.");
        originalSystemOut.println("Compilation succeeded. This is unexpected, but helpful! It means that the underlying compiler issue has been resolved. " +
                                  "There is likely still a bug to fix in JavacCompiler, but we'll need a new mechanism to reproduce.");
    }

    @Test
    void testOutputOrMessagesReferenceProblematicFileName() throws Exception {
        var result = tryCompilation(true);
        Assumptions.assumeFalse(result.isSuccess(), "This test is not applicable if the compilation succeeds.");
        assertTrue(outputContainsString(result, PROBLEMATIC_FILE_NAME),
                   "Ideally, the output should also include the file name in which the failure occurred. " +
                   "I'm not sure if this is solvable at the Plexus level or if it requires changes to javac.");
    }

    private boolean outputContainsString(CompilerResult compilerResult, String desiredString) {
        boolean foundIt = false;
        for (CompilerMessage compilerMessage : compilerResult.getCompilerMessages()) {
            if (compilerMessage.getMessage().contains(desiredString)) {
                originalSystemOut.printf("Found '%s' in a compiler message.%n", desiredString);
                return true;
            }
        }

        String systemOut = capturedSystemOut.toString();
        if (systemOut.contains(desiredString)) {
            originalSystemOut.printf("Found '%s' in javac's stdout.%n", desiredString);
            return true;
        }

        String systemErr = capturedSystemErr.toString();
        if (systemErr.contains(desiredString)) {
            originalSystemOut.printf("Found '%s' in javac's stderr.%n", desiredString);
            return true;
        }

        return false;
    }

    private static CompilerResult tryCompilation(boolean fork) throws Exception {
        cleanOutputDirectory();

        var config = new CompilerConfiguration();
        config.setFork(fork);
        config.setSourceVersion("12");
        config.setTargetVersion("12");
        config.setWorkingDirectory(new File("."));
        config.addSourceLocation("project-to-compile/src");
        config.setOutputLocation("project-to-compile/out");
        var compilerResult = compiler.performCompile(config);
        if (!compilerResult.isSuccess()) {
            originalSystemOut.printf("Compilation failed with %d messages:%n", compilerResult.getCompilerMessages().size());

            for (CompilerMessage compilerMessage : compilerResult.getCompilerMessages()) {
                originalSystemOut.printf("* %s%n", compilerMessage);
            }

            assertThat("There should be a compiler message with kind=ERROR when compilation fails.",
                       compilerResult.getCompilerMessages(), hasItem(isCompilerErrorMessage()));
        }
        return compilerResult;
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
