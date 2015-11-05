#!/bin/bash
#   Copyright (c) 2015 Magnet Systems, Inc.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#


curdir=`pwd`

MMX_SERVER_NAME="mmx-server-2.0.12"

seeddata_sql="$curdir/../test-conf/seed_testdata.sql"
cleanup_sql="$curdir/../test-conf/clean_testdata.sql"

sandbox_dir="$curdir/$MMX_SERVER_NAME"
bin_dir="$sandbox_dir/bin"
conf_dir="$sandbox_dir/conf"

external_build_dir=
local_build_dir="$curdir/../../../message-server/tools/mmx-server-zip/target"

if [ -z "$MMX_MYSQL_USR" ] ; then
    MMX_MYSQL_USR=root
    MMX_MYSQL_PWD=
fi

if [ -z "$MMX_DB" ] ; then
    MMX_DB=mmxintegtest
fi

mysql_command="mysql -u $MMX_MYSQL_USR"
if [ -n "$MMX_MYSQL_PWD" ] ; then
    mysql_command="$mysql_command -p $MMX_MYSQL_PWD"
fi

# sleep and prompt the wait time
sleepAndEcho() {
  timeout=$1
  while [ "$timeout" -gt 0 ]; do
    printf "wait ${timeout}s  \r"
    sleep 5
    timeout=`expr $timeout - 5`
  done
}

# remove existing test binaries

cleanup() {
    if [ -e $sandbox_dir/bin/mmx.pid ]; then
        echo "Attemp to stop existing MMX server..."
        stop
        sleep 5
    fi
    echo "Deleting unzipped mmx-server bits..."
    delete_command="rm -rf $sandbox_dir *.zip"
    eval "$delete_command"
}

copy_local() {
    cleanup
    echo "Copying MMX server zip from local build, "
    copy_command="cp $local_build_dir/${MMX_SERVER_NAME}.zip ."
    eval "$copy_command"
    echo "Unzipping copied file..."
    eval "unzip ${MMX_SERVER_NAME}.zip"
}

copy_external() {
    cleanup
    echo "Copying MMX server zip from external build, "
    copy_command="cp $external_build_dir/${MMX_SERVER_NAME}.zip ."
    eval "$copy_command"
    echo "Unzipping copied file..."
    eval "unzip ${MMX_SERVER_NAME}.zip"
}

start_local() {
    copy_local
    start
}

start_external() {
    copy_external
    start
}

start() {
    # copy test configuration
    echo "Copying test startup.properties"
    copy_command="cp $curdir/../test-conf/startup.properties $conf_dir"
    echo "$copy_command"
    eval "$copy_command"

    # cleanup existing data
    cleanup_db_command="$mysql_command < $cleanup_sql"
    echo "Deleting existing test data..."
    echo "$cleanup_db_command"
    eval "$cleanup_db_command"

    # stop server
    stop
    sleep 5

    # start MMX
    pushd $bin_dir
    echo `pwd`
    start_command="./mmx-server.sh start"
    echo "Starting MMX..."
    echo "$start_command"
    eval "$start_command"

    # sleep a bit to wait for it to finish initializing; default is 60
    WAITTIME=${WAITTIME:-60}
    sleepAndEcho ${WAITTIME}

    # seed test data
    echo "Seeding test data..."
    seed_data="$mysql_command $MMX_DB < $seeddata_sql"
    echo "$seed_data"
    eval "$seed_data"
    if eval "$@"; then
    # restart the server to pick up new test data from db into server memory cache, such case pubsub nodes
        eval "./mmx-server.sh restart"
        echo "MMX server started successfully"
    fi
    popd
    # sleep some more to wait for server to finish initializing
    sleepAndEcho 20
}

stop() {
    pushd $bin_dir
    stop_command="./mmx-server.sh stop"
    echo "Stopping MMX..."
    eval "$stop_command"
    popd
}

usage() {
    echo "Usage: $0 {start|stop|restart} {local|external-build-dir}" 1>&2
    echo "Example: $0 start local"
    echo "Example: $0 start /mnt/host/build/target # where ${MMX_SERVER_NAME}.zip can be found"
    echo "Example: $0 stop"
}

case "$1" in
    start)
        if [ $# -le 1 ]; then
            usage
            exit 1
        fi
        ;;
    stop)
        stop
        exit 0
        ;;
    restart)
        stop
        start
        exit 0
        ;;
    **)
        usage
        exit 1
        ;;
esac

case "$2" in
    local)
        start_local
        exit 0
        ;;
    **)
        external_build_dir=$2
        start_external
        exit 0
        ;;
esac
