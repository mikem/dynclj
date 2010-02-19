#!/bin/bash

CLASSPATH="classes/:lib/*:src"
exec java -client -cp $CLASSPATH com.mmazur.dynclj.dynclj $*
