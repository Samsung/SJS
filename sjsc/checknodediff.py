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
#!/usr/bin/env python

# script to build jars for maven central
import sys
import subprocess
import os

script = sys.argv[1]

node_output = subprocess.check_output("v8 " + script, shell=True)

print node_output

# run sjs compiler
subprocess.check_output("./sjsc-fast --oldExpl " + script, shell=True)

# get output from a.out

sjs_output = subprocess.check_output("./a.out", shell=True)

print sjs_output

if (not (node_output == sjs_output)):
    print "DIFFERENT OUTPUT"


