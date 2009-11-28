#!/bin/bash

LIBS="$(find -H lib/ -mindepth 2> /dev/null 1 -maxdepth 1 -print0 | tr \\0 \:)"
CLASSPATH="src/:classes/:$LIBS"
exec java -client -cp $CLASSPATH com.mmazur.dynclj.dynclj $*
