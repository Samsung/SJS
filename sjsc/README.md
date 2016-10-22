SJS Compiler
============

Quick Start
-----------

We're building and managing Java dependencies via gradle.

The first time you run a test suite, you'll notice the build system building 2 GCs for your host OS.

To build the Java source:

    gradle build

To run, use the script

    ./sjsc <js file>

which will produce an a.out that can then be executed.

If you are only using the compiler, and not actively developing on the compiler, you can first run

    gradle distJar

which will package the compiler as a JAR, though still with dependencies on the development
repository layout (when invoking the C compiler).  You can run this canned compiler using

    ./sjsc-fast <args...>

which will run much faster than invoking through gradle, but will not detect if the JAR is out of
date.

Note that gradle is configured for Java 8.  To make this work on a Mac, set

    export JAVA_HOME=$(/usr/libexec/java_home)

You must set it manually on Linux.

Detailed Build Instructions
---------------------------

### Requirements
There are a few specific requirements:

* JDK 8
* gradle 2.0 or higher
* node.js (for the test suite)
* pkg-config (without this, building the GC will fail in mysterious ways)
* libtool (without this, building the GC will fail in mysterious ways)
* clang (3.1 or higher when linking against C++ code)
* make, automake, autoreconf
* libbsd-dev (on Linux)
* g++-multilib (on Linux)

We have tested SJS on Mac OS X, and Debian and Ubuntu Linux.  Other Linux distributions and Windows
are wholly untested.

To get JDK 8 on Linux:

* Install java-package
* Download the latest Linux JDK 8 tarball from Oracle
* Run make-jpkg on that tarball
* sudo dpkg -i <resulting .deb>
* Use update-alternatives to switch java and javac to JDK 8

### Building Other GCs

The 'external' directory includes scripts to build a number of other GCs:

* boehm_native.sh: Build for local host
* boehm_32.sh: 32-bit host (tested on Mac OS X and Linux)
* boehm_arm.sh: 32-bit Tizen ARM (for device)
* boehm_i386.sh: 32-bit Tizen i386 (for emulator)

### Testing

To run the test suite, run

    gradle test

To run only a specific test suite, run

    gradle test -Dtest.single=<test suite name>

Some interesting examples include

    gradle test -Dtest.single=EndToEndTest
    gradle test -Dtest.single=X86Test

Note that X86Test is currently broken on Linux unless you install a 32-bit version of libbsd.

### Directory structure

    sjsc/ -- this directory
    |
    +- external/ -- scripts and build directories for external dependencies (Boehm GC)
    |
    +- src/ -- main compiler source
       |
       +- main/ -- main compiler source
       |  |
       |  +- java/ -- source
       |  |  |
       |  |  +- com/samsung/sjs/ -- main SJS compiler namespace
       |  |     |
       |  |     +- Compiler.java -- main compiler driver, including orchestrating backend passes
       |  |     |
       |  |     +- CompilerOptions.java -- compiler config
       |  |     |
       |  |     +- FFILinkage.java -- processing for ffi linking information
       |  |     |
       |  |     +- JSEnvironment.java -- processing for top-level environment description
       |  |     |
       |  |     +- backend/ -- Backend code gen: optimizations, lowering, IRs, generating C
       |  |     |  |
       |  |     |  +- ast/ -- internal IR and C AST types
       |  |     |  |
       |  |     |  +- ConstantInliningPass -- Inline variables that are never assigned to, repeatedly
       |  |     |  |
       |  |     |  +- EnvironmentCapture -- Type for the environment parameter to a closure
       |  |     |  |
       |  |     |  +- EnvironmentLayout -- Representation of a closure environment
       |  |     |  |
       |  |     |  +- FieldAccessOptimizer -- Optimization pass to apply field access optimizations
       |  |     |  |
       |  |     |  +- FieldCollector -- Gather property names that appear explicitly in the program
       |  |     |  |
       |  |     |  +- IRCBacked -- last step in code gen, translates lowered IR to C + SJS runtime
       |  |     |  |
       |  |     |  +- IRClosureConversionPass -- closure conversion pass
       |  |     |  |
       |  |     |  +- IREnvironmentLayoutPass -- calculate environment layout for closures (variable capture)
       |  |     |  |
       |  |     |  +- IndirectionMapPass -- calculate object layouts, and therefore indirection maps
       |  |     |  |
       |  |     |  +- IntrinsicsInliningPass -- inline operators, accesses to known objects like Math
       |  |     |  |
       |  |     |  +- RhinoToIR -- convert Rhino AST to high-level SJS IR
       |  |     |  |
       |  |     |  +- RhinoTypeValidator -- perform very rough type-check; verify sanity of inference results
       |  |     |  |
       |  |     |  +- SJSTypeConverter -- Convert SJS types their C-type representations
       |  |     |
       |  |     +- constraintgenerator/ -- Generating type constraints
       |  |     |
       |  |     +- constraintsolver/ -- Solving type constraints
       |  |     |
       |  |     +- typeconstraints/ -- Type constraint representations
       |  |     |
       |  |     +- types/ -- Type representations
       |  | 
       |  |
       |  +- resources/ -- support files: global environment and C runtime implementation
       |     |
       |     +- environment.json -- top level environment spec
       |     |
       |     +- linkage.json -- top level environment linking information
       |     |
       |     +- operators.json -- specification for operator overloads
       |     |
       |     +- backend/ -- C implementation of the SJS runtime
       |
       +- test/ -- test programs and JUnit tests


How to get involved
-------------------

We welcome contributions to SJS.  The procedure for contributing is
outlined below.  (These notes were adapted from those for the
[JerryScript](https://github.com/Samsung/jerryscript) project.)

### Certificate of Origin

SJS uses the signed-off-by language and process, to give us a clear chain of trust for every patch received.

> By making a contribution to this project, I certify that:  

> (a)	The contribution was created in whole or in part by me and I have the right to submit it under the open source license indicated in the file; or

> (b)	The contribution is based upon previous work that, to the best of my knowledge, is covered under an appropriate open source license and I have the right under that license to submit that work with modifications, whether created in whole or in part by me, under the same open source license (unless I am permitted to submit under a different license), as indicated in the file; or

> (c)	The contribution was provided directly to me by some other person who certified (a), (b) or (c) and I have not modified it.

> (d)	I understand and agree that this project and the contribution are public and that a record of the contribution (including all personal information I submit with it, including my sign-off) is maintained indefinitely and may be redistributed consistent with this project, under the same open source license. 


#### Using the Signed-Off-By Process

We have the same requirements for using the signed-off-by process as the Linux kernel. In short, you need to include a signed-off-by tag in every patch:

"Signed-off-by:" this is a developer's certification that he or she has the right to submit the patch for inclusion into the project. It is an agreement to the Developer's Certificate of Origin (above). **Code without a proper signoff cannot be merged into the mainline.**

You should use your real name and email address in the format below:

> SJS-DCO-1.0-Signed-off-by: Random J Developer random@developer.example.org


#### How to add DCO every single commit automatically.

It is easy to forget adding DCO end of every commit message. Fortunately there is a nice way to do it automatically. Once you've clone the repository into your local machine, you can add `prepare commit message hook` in `.git/hooks` directory like this:

```
#!/usr/bin/env python

import sys

commit_msg_filepath = sys.argv[1]

with open(commit_msg_filepath, "r+") as f:
	content = f.read()
	f.seek(0, 0)
	f.write("%s\n\nSJS-DCO-1.0-Signed-off-by: <Your Name> <Your Email>" % content)
```

Please refer [Git Hooks](http://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks) for more information.

### Development Process


#### Proposals, Get Answers and Report a Bug via Github Issues

If you have a question about SJS code, have trouble any documentation,
would like to suggest new feature, or find a bug, review the current
SJS issues in GitHub, and if necessary, create a new issue.

#### Feature development process

The SJS Project development process is marked by the following highlights:
* The feature development process starts with an author discussing a proposed feature with the Maintainers and Reviewers
  - Open the issue with label 'feature request'
* The Maintainers and Reviewers evaluate the idea, give feedback, and finally approve or reject the proposal.
* The author shares the proposal with the community via **_Github issues with 'feature request' label_**
* The community provides feedback which can be used by the author to modify their proposal and share it with the community again.
* The above steps are repeated until the community reaches a consensus
  according to the community guidelines.
* After a consensus is reached, the author proceeds with the implementation and testing of the feature.
* After the author is confident their code is ready for integration:
  - The author generates a patch and signs off on their code.
  - The author submits a patch according to the patch submission process.
* The Maintainers and Reviewers watch the pull request for the patch, test the code, and accept or reject the patch accordingly.
* After the code passes code review, the Maintainers and Reviewers accept the code(integrated into the master branch), which completes the development process.
* After a patch has been accepted, it remains the authoring developer's responsibility to maintain the code throughout its lifecycle, and to provide security and feature updates as needed.

#### Approval Path for PR(Pull Request)
1. Developer should create/update PR to a given issue or enhancement
2. If Developer works in a team, then peer-review by a colleague developer should be performed
3. If peer-review was OK, then Developer should summon the component's maintainer
4. Maintainer should check the code:
   - make precommit testing is OK (performed automatically)
   - No minor issues (unified implementation style, comments, etc.)
   - No major issues (memory leak, crashes, breakage of ECMA logic, etc.)
5. If Developer has to rework the solution then goto step 3
6. If everything is OK, then Maintainer should approve the PR with +1(or LGTM)
   - Code review can be performed by all the members of the project. However only Maintainer can give binding scores.
7. When the PR get +2(2 LGTM from 2 mainatiners respectively), it should be merged.

### Patch Submission Process

The following guidelines on the submission process are provided to help you be more effective when submitting code to the SJS project.

When development is complete, a patch set should be submitted via Github pull requests. A review of the patch set will take place. When accepted, the patch set will be integrated into the master branch, verified, and tested. It is then the responsibility of the authoring developer to maintain the code throughout its lifecycle.

Please submit all patches in public by opening a pull request. Patches sent privately to Maintainers and Committers will not be considered. Because SJS is an Open Source project, be prepared for feedback and criticism-it happens to everyone-. If asked to rework your code, be persistent and resubmit after making changes.

#### 1. Scope the patch

Smaller patches are generally easier to understand and test, so please submit changes in the smallest increments possible, within reason. Smaller patches are less likely to have unintended consequences, and if they do, getting to root cause is much easier for you and the Maintainers and Committers. Additionally, smaller patches are much more likely to be accepted.

#### 2. Sign your work with the SJS DCO

The sign-off is a simple line at the end of the explanation for the patch, which certifies that you wrote it or otherwise have the right to pass it on as an Open Source patch. The  sign-off is required for a patch to be accepted.

#### 3. Open a Github pull request

#### 4. What if my patch is rejected?

It happens all the time, for many reasons, and not necessarily because the code is bad. Take the feedback, adapt your code, and try again. Remember, the ultimate goal is to preserve the quality of the code and maintain the focus of the Project through intensive review.

Maintainers and Committers typically have to process a lot of submissions, and the time for any individual response is generally limited. If the reason for rejection is unclear, please ask for more information to the Maintainers and Committers.
If you have a solid technical reason to disagree with feedback and you feel that reason has been overlooked, take the time to thoroughly explain it in your response.

#### 5. Code review

Code review can be performed by all the members of the Project (not just Maintainers and Committers). Members can review code changes and share their opinion by comments with the following principles:
* Discuss code; never discuss the code's author.
* Respect and acknowledge contributions, suggestions, and comments.
* Listen and be open to all different opinions.
* Help each other.

Changes are submitted via pull requests and only the Maintainers and Committers should approve or reject the pull request.
Changes should be reviewed in reasonable amount of time. Maintainers and Committers should leave changes open for some time (at least 1 full business day) so others can offer feedback. Review times increase with the complexity of the review.

