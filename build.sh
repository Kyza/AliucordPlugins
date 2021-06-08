#!/bin/sh
# POSIX Shell script to build plugin, push it to device, and reopen discord via adb.
trap "cd '$PWD'" EXIT
set -ex
cd ../AliucordBuildtool
./buildtool.exe -p "$1"
cd ../AliucordPlugins/buildsPlugins
adb push "./${1}.zip" //storage/emulated/0/Aliucord/plugins
adb shell am force-stop com.aliucord
adb shell monkey -p com.aliucord -c android.intent.category.LAUNCHER 1
