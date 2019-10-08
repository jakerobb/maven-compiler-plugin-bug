# plexus-javac-compiler-bug

I originally reported this issue as https://issues.apache.org/jira/browse/MCOMPILER-325, but the Maven folks told me it was a Plexus issue. Then some time 
passed while I didn't have time to work on it, but I've now put together a simple project to reproduce the issue directly using Plexus. This is it. I'll be 
referencing this project from a bug I'm about to report to the Plexus folks.

UPDATE: It has now been reported as https://github.com/codehaus-plexus/plexus-compiler/issues/66, verified affecting the latest version as of this writing 
(2.8.5). The underlying version of Java does not seem to matter.

This repo contains an MVCE for the bug. 

To reproduce, simply clone this repo and execute `mvn test`. There are four test methods:
* **testSuccessfulWhenCompilingOutOfProcess_BugIsResolvedIfThisPasses**
    This performs the compilation with fork=true and verifies that there is at least one compiler message with kind=ERROR and that some form of output (compiler 
    message, stdout, stderr) includes the string "StackOverflowError".

* **testSuccessfulWhenCompilingInProcess**
    This performs the compilation with fork=false and performs the same verification checks as the previous test. This is the only test that passes as-is.

* **testCompilationSucceeds**
    This performs the compilation and simply verifies that it still fails. It's possible that the underlying problem in javac gets resolved (e.g. by 
    refactoring to not use recursion) before this bug gets fixed, and if that happens, the rest of this MVCE would need to be refactored to trigger some other
    failure in the compiler.

* **testOutputOrMessagesReferenceProblematicFileName**
    Ideally, the compiler output should contain the name of the problematic file. As-is, it does not. This could certainly be addressed in javac, but it seems
    to me that there might also be a way to address it in JavacCompiler, and so I added this test method to determine whether it is fixed.


External dependencies:
* Java 12+ (the problem is reproducible in versions as far back as 8, and probably older, but I used Java 12 in my testing and employed the new-in-10 `var` keyword)
* Maven 3.x (I used 3.5.0)

