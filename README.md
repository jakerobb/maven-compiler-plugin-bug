# plexus-javac-compiler-bug

I originally reported this issue as https://issues.apache.org/jira/browse/MCOMPILER-325, but the Maven folks told me it was a Plexus issue. Then some time passed while I didn't have time to work on it, but I've now put together a simple project to reproduce the issue directly using Plexus. This is it. I'll be referencing this project from a bug I'm about to report to the Plexus folks.

It has now been reported as https://github.com/codehaus-plexus/plexus-compiler/issues/66, verified affecting all versions up to 2.8.5. The underlying version of Java does not seem to matter.

To reproduce, 
