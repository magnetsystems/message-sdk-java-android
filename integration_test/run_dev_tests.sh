#!/bin/bash -x
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
WAITTIME=60

trap "cd $curdir/startserver; ./init_mmx_test.sh stop; exit 1" SIGINT SIGTERM

usage() {
  echo "Usage: $0 [-w seconds-to-wait-for-server] [-r] [-a|android]" 1>&2
  echo "       -r for REST test" 1>&2
  echo "       -a for Android test" 1>&2
  exit 1
}

genLocalProps() {
  case `uname` in
  "Darwin")
    rm -f $2;
    for iface in `ifconfig -l -u inet`; do
      if [ "$iface" != "lo0" ]; then
        IPADDR=`ifconfig "$iface" | sed -n -e '/inet /p' | awk '{ print $2 }'`
        if [ "$IPADDR" != "" ]; then
          set +x
          echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
          echo "@"
          echo "@ Configure host=$IPADDR"
          echo "@"
          echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
          set -x
          sed -e "s/^host=[0-9.]*$/host=$IPADDR/" $1 > $2
          return
        fi
      fi
    done;;
  "Linux")
    rm -f $2;
    # running interface
    for iface in `ifconfig -s | tail -n +2 | grep R | cut -d' ' -f1`; do
      if [ "$iface" != "lo" ]; then
        IPADDR=`ifconfig "$iface" | sed -n -e '/inet addr:/s/:/ /gp' | awk '{ print $3 }'`
        if [ "$IPADDR" != "" ]; then
          set +x
          echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
          echo "@"
          echo "@ Configure host=$IPADDR"
          echo "@"
          echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
          set -x
          sed -e "s/^host=[0-9.]*$/host=$IPADDR/" $1 > $2
          return
        fi
      fi
    done;;
  esac
}

while getopts "rae:w:" o; do
  case "${o}" in
  "a") TESTANDROID="android";;
  "e") export EXTERNAL_BUILD_DIR=${OPTARG};;
  "r") TESTREST="rest";;
  "w") export WAITTIME=${OPTARG};;
  *)   usage;;
  esac
done
if [ "${TESTANDROID}" = "" ]; then
  shift $((OPTIND-1))
  TESTANDROID=${1}
fi

stop_server() {
  # stop the server
  pushd "$curdir/startserver"
  stop_command="./init_mmx_test.sh stop"
  eval "$stop_command"
  popd
}

# bootstrap and start then server
pushd "$curdir/startserver"
if [[ "$EXTERNAL_BUILD_DIR" != "" ]]; then
  start_command="./init_mmx_test.sh start $EXTERNAL_BUILD_DIR"
else
  start_command="./init_mmx_test.sh start local"
fi
eval "$start_command"
popd

# Run REST API
if [[ "$EXTERNAL_BUILD_DIR" != "" ]]; then
  echo "Running REST API functional tests for MMX external build..."
else
  echo "Running REST API functional tests for MMX local build..."
fi

# Run REST API tests
if [[ "${TESTREST}" == "rest" ]]; then
  pushd "./restapi"
  ./gradlew test
  status=$?
  popd

  echo "REST API STATUS: $status"
  if [ "$status" != "0" ]; then
    echo "FAILURE: REST API TEST FAILED"
    stop_server
    exit $status
  fi
fi

if [[ "${TESTANDROID}" == "android" ]]; then
  # start Android unit tests
  echo "Running Android functional tests for MMX local build..."

  # push localhost configuration to emulator
  genLocalProps ./test-conf/android_local.properties ./test-conf/local.properties
  if [ -f ./test-conf/local.properties ]; then
    adb_command="adb push ./test-conf/local.properties /sdcard/mmx-debug.properties"
  else
    adb_command="adb push ./test-conf/android_local.properties /sdcard/mmx-debug.properties"
  fi
  eval "$adb_command"

  pushd "../android"
  run_android_test="./gradlew clean build connectedCheck"
  eval "$run_android_test"
  status=$?
  popd
  adb_command="adb shell rm /sdcard/mmx-debug.properties"
  eval "$adb_command"
fi

stop_server;
exit $status

