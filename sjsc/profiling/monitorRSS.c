/* 
 * Copyright 2014-2016 Samsung Research America, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*-----------------------------------------------------------------------------
 * Monitor the RSS size of a given process tree.
 *
 * Author: Cole Schlesinger
 */

#include <stdio.h>
#include <string.h>
#include <sys/resource.h>
#include <sys/times.h>
#include <sys/wait.h>
#include <unistd.h>

int main(int argc, char *const argv[]) {

    pid_t pid;
    struct rusage my_rusage, child_rusage;
    struct tms my_tms;
    long maxRSS;
    int i;

    if (argc < 2 || strcmp(argv[1], "-h") == 0 || strcmp(argv[1], "--help") == 0) {
        printf("use: monitorRSS <cmd> [<cmd args>]\n");
        printf("out: <max rss (bytes)>\t<user time (seconds)>\t<system time (seconds)>\n");
        return 1;
    }

    // Print command being monitored.
    // printf("=== monitorRSS\n");
    // printf("=== command:");
    // for (i = 1; i < argc; i++) {
    //     printf(" %s", argv[i]);
    // }
    // printf("\n");
    // fflush(stdout);

    // Fork a child to execute the command.
    pid = fork();
    if (!pid) {
        return execvp(argv[1], &(argv[1]));
    }

    // When the child terminates, get its peak RSS use.
    if (waitpid(pid, NULL, 0) < 0) {
        printf("error: waitpid() failed\n");
        return -1;
    }

    if (getrusage(RUSAGE_SELF, &my_rusage) < 0) {
        printf("error: getrusage()\n");
        return -1;
    }

    if (getrusage(RUSAGE_CHILDREN, &child_rusage) < 0) {
        printf("error: getrusage()\n");
        return -1;
    }

    if (times(&my_tms) == -1) {
        printf("error: times()\n");
        return -1;
    }

    maxRSS = child_rusage.ru_maxrss - my_rusage.ru_maxrss;
    #if defined(__unix__) || defined(__unix) || defined(unix)
    maxRSS = maxRSS * 1024L;
    #endif

    printf( "%ld\t%ld.%06ld\t%ld.%06ld\n"
          , maxRSS
          , (long int)child_rusage.ru_utime.tv_sec
          , (long int)child_rusage.ru_utime.tv_usec
          , (long int)child_rusage.ru_stime.tv_sec
          , (long int)child_rusage.ru_stime.tv_usec);

    return 0;
}
