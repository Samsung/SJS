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

# This script downloads, configures, and builds the external dependencies for the SJS compiler

export EXT=`pwd`

./fetch_dependencies.sh

./configure_emscripten.sh

./boehm_native.sh

./boehm_asmjs.sh

mkdir spidermonkey
pushd spidermonkey
curl http://ftp.mozilla.org/pub/mozilla.org/firefox/nightly/latest-trunk/jsshell-mac.zip > jsshell-mac.zip
unzip jsshell-mac.zip
popd
