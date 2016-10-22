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
scp csgordon@192.168.56.101:debian.tgz .
sdb -d push debian.tgz /root/debian.tgz
echo Go unpack on the device into /root/debian, mount devices, chroot, run 
echo     debootstrap/debootstrap --second-stage
echo and press ENTER to continue
read
sdb -d push ~/phone /root/debian/js/
sdb -d push src/main/resources/backend /root/debian/js/backend
sdb -d push armeabi.sh /root/debian/js/gcc.sh
