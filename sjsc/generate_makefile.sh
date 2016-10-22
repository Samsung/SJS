# Copyright 2014-2016 Samsung Research America, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#!/bin/bash

# This script generates a Makefile that wraps all available gradle tasks
# Then in any editor with Make support (e.g., vim, emacs), gradle tasks
# can be invoked as if they were make targets

# Pretty straightforward, except we hoist the build task to the top to make it the default

# Note this first line resets the Makefile
echo .PHONY: `gradle tasks | grep ' - ' | cut -f 1 -d ' ' | tr "\\n" " "` > Makefile
echo "build:" >> Makefile
echo -e "\tgradle build" >> Makefile
for target in `gradle tasks | grep ' - ' | cut -f 1 -d ' ' | grep -v '^build$'`;
do
    echo "$target:" >> Makefile;
    echo -e "\tgradle $target" >> Makefile;
done
