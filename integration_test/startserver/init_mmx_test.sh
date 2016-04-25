#!/bin/bash
#   Copyright (c) 2015-2016 Magnet Systems, Inc.  All Rights Reserved.
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

# Pretty print
pprint() {
  echo
  printblk
  printline "$@"
  printblk
}

printblk() {
  echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
}

printline() {
  echo "+ $@"
}

# get the value from "name=value" in a file
getProperty() {
  sed -n -e 's/^'$2'=\(.*\)/\1/p' $1
}

curdir=`pwd`

MMX_SERVER_NAME="mmx-server-2.7.0-B-7"
MAX_SERVER_NAME="max-server-2.7.0-B-7"

seeddata_sql="$curdir/../test-conf/seed_testdata.sql"
max_seeddata_sql=

cleanup_sql="$curdir/../test-conf/clean_testdata.sql"
max_cleanup_sql="$curdir/../test-conf/max_clean_testdata.sql"

sandbox_dir="$curdir/$MMX_SERVER_NAME"
bin_dir="$sandbox_dir/bin"
conf_dir="$sandbox_dir/conf"
max_sandbox_dir="$curdir/$MAX_SERVER_NAME"
max_bin_dir="$max_sandbox_dir/bin"
max_conf_dir="$max_sandbox_dir/conf/default"

external_build_dir=
local_build_dir="$curdir/../../../message-server/tools/mmx-server-zip/target"
max_external_build_dir=
max_local_build_dir="$HOME/.magnet/server"

startup_properties=$curdir/../test-conf/startup.properties
datasource_properties=$curdir/../test-conf/datasource_mysql.properties

MMX_DB=`getProperty $startup_properties dbName`
MMX_MYSQL_USR=`getProperty $startup_properties dbUser`
MMX_MYSQL_PWD=`getProperty $startup_properties dbPassword`

MAX_MYSQL_USR=`getProperty $datasource_properties javax.persistence.jdbc.user`
MAX_MYSQL_PWD=`getProperty $datasource_properties javax.persistence.jdbc.password`

#echo MMX_SERVER_NAME=$MMX_SERVER_NAME
#echo MAX_SERVER_NAME=$MAX_SERVER_NAME
#echo cleanup_sql=$cleanup_sql
#echo max_cleanup_sql=$max_cleanup_sql
#echo sandbox_dir=$sandbox_dir
#echo max_sandbox_dir=$max_sandbox_dir
#echo conf_dir=$conf_dir
#echo max_conf_dir=$max_conf_dir
#echo startup_properties=$startup_properties
#echo datasource_properties=$datasource_properties
#echo MMX_DB=$MMX_DB
#echo MMX_MYSQL_USR=$MMX_MYSQL_USR
#echo MMX_MYSQL_PWD=$MMX_MYSQL_PWD
#echo MAX_MYSQL_USR=$MAX_MYSQL_USR
#echo MAX_MYSQL_PWD=$MAX_MYSQL_PWD

mysql_command="mysql -u $MMX_MYSQL_USR"
if [[ -n "$MMX_MYSQL_PWD" ]] ; then
  mysql_command="$mysql_command -p $MMX_MYSQL_PWD"
fi
max_mysql_command="mysql -u $MAX_MYSQL_USR"
if [[ -n "$MMX_MYSQL_PWD" ]] ; then
  max_mysql_command="$max_mysql_command -p $MAX_MYSQL_PWD"
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
    if [[ -e $sandbox_dir/bin/mmx.pid ]]; then
        echo "Attemp to stop existing MMX server..."
        stop
        sleep 5
    fi
    echo "Deleting unzipped mmx-server bits..."
    delete_command="rm -rf $sandbox_dir *.zip"
    eval "$delete_command"

    # Remove the MAX 
    echo "Deleting max-server bits..."
    delete_command="rm -rf $max_sandbox_dir"
    eval "$delete_command"
}

copy_local() {
    cleanup
    echo "Copying MMX server zip from local build, "
    copy_command="cp $local_build_dir/${MMX_SERVER_NAME}.zip ."
    eval "$copy_command"
    echo "Unzipping copied file..."
    eval "unzip ${MMX_SERVER_NAME}.zip"

    echo "Copying MAX server from local build"
    test -d $max_sandbox_dir || mkdir -p $max_sandbox_dir
    max_copy_command="(cd $max_local_build_dir; tar cf - . ) | (cd $max_sandbox_dir; tar xf - )"
    eval "$max_copy_command"
}

copy_external() {
    cleanup
    echo "Copying MMX server zip from external build, "
    copy_command="cp $external_build_dir/${MMX_SERVER_NAME}.zip ."
    eval "$copy_command"
    echo "Unzipping copied MMX file..."
    eval "unzip ${MMX_SERVER_NAME}.zip"

    echo "Copying MAX server from external build"
    test -d $max_sandbox_dir || mkdir -p $max_sandbox_dir
    max_copy_command="(cd $max_external_build_dir; tar cf - . ) | (cd $max_sandbox_dir; tar xf - )"
    eval "$max_copy_command"
}

start_local() {
    copy_local
    start
}

start_external() {
    copy_external
    start
}

check_dbsetup() {
  $mysql_command $1 2>/dev/null <<EOF
select count(*) from mmxTopicRole;
EOF
}

start() {
    # stop server (just in case)
    stop

    # copy test configuration
    pprint "Copying MMX test startup.properties"
    copy_command="cp $startup_properties $conf_dir"
    echo "$copy_command"
    eval "$copy_command"

    pprint "Copying MAX test datasource_mysql.properties"
    copy_command="cp $datasource_properties $max_conf_dir"
    echo "$copy_command"
    eval "$copy_command"

    # cleanup existing data
    cleanup_db_command="$mysql_command < $cleanup_sql"
    pprint "Deleting existing MMX test data..."
    echo "$cleanup_db_command"
    eval "$cleanup_db_command"

    cleanup_db_command="$max_mysql_command < $max_cleanup_sql"
    pprint "Deleting existing MAX test data..."
    echo "$cleanup_db_command"
    eval "$cleanup_db_command"

    # start MMX
    pushd $bin_dir
    echo `pwd`
    start_command="./mmx-server.sh start"
    pprint "Starting MMX..."
    echo "$start_command"
    eval "$start_command"

    # wait until all tables were created.
    pprint "Setting up DB $MMX_DB and waiting for the completion..."
    until [[ `check_dbsetup $MMX_DB` ]]; do
      /bin/echo -n .
      sleep 5
    done
    sleep 3
    popd

    # stop the servers again before seeding the data
    stop

    pprint "Seeding MMX test data..."
    seed_data="$mysql_command $MMX_DB < $seeddata_sql"
    echo "$seed_data"
    eval "$seed_data"
    if [[ $@ -eq 0 ]]; then
      pprint "Restarting MMX..."
      echo "$start_command"
      pushd $bin_dir
      eval "$start_command"
      popd
    else
      pprint "Seeding MMX test data failed"
      exit 1
    fi

    # start MAX
    pprint "Starting MAX..."
    start_command="bin/start.sh &"
    pushd $max_sandbox_dir
    echo "$start_command"
    eval "$start_command"
    popd
}

stop() {
    pushd $max_sandbox_dir
    stop_command="bin/stop.sh"
    echo "Stopping MAX..."
    eval "$stop_command"
    popd

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
        if [[ $# -le 1 ]]; then
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
