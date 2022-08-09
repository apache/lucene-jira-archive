#!/bin/sh

# path to lucene 9.0.0 binary distribution
LUCENE9_DIR="lucene-9.0.0-SNAPSHOT"

OUT_DIR="module-info"

# Discover all lucene jars and their dependencies.
# Note: exclude test-framework and benchmark.
# - we left split packages in test-framework (LUCENE-9499)
# - benchmark depends on xercesImpl which has issues with module system
lucene_jars=( `find ${LUCENE9_DIR} -name "lucene-*.jar" | grep -v "test-framework" | grep -v "benchmark"` )
dependencies=( `find ${LUCENE9_DIR} -name "*.jar" -not -name "lucene-*.jar" | grep -v "test-framework" | grep -v "benchmark"` `find ~/.gradle/caches/ -name "asm-*7.2.jar"`)
mp="$(IFS=:; echo "${dependencies[*]%/*}")"

# generate non-open module discriptors for all lucene jars.
# --multi-release option is needed beacuse luke module depends on log4j; which is a MR jar
jdeps --generate-module-info ${OUT_DIR} --module-path ${mp} --multi-release 11 ${lucene_jars[*]}

# ...or, generate open module descriptors for all lucene jars.
#jdeps --generate-open-module ${OUT_DIR} --module-path ${mp} --multi-release 11 ${lucene_jars[*]}
