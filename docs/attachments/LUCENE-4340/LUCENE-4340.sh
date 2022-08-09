#! /bin/bash

set -e

mkdir -p lucene/codecs/src/java/org/apache/lucene/codecs
mkdir -p lucene/codecs/src/test/org/apache/lucene/codecs
mkdir -p lucene/codecs/src/resources/META-INF/services
svn add lucene/codecs
for f in Codec PostingsFormat; do
  svn cp lucene/core/src/resources/META-INF/services/org.apache.lucene.codecs.${f} lucene/codecs/src/resources/META-INF/services
done
for f in appending block bloom intblock memory pulsing sep simpletext BlockTermsReader.java BlockTermsWriter.java FixedGapTermsIndexReader.java FixedGapTermsIndexWriter.java TermsIndexReaderBase.java TermsIndexWriterBase.java VariableGapTermsIndexReader.java VariableGapTermsIndexWriter.java; do
  svn mv lucene/core/src/java/org/apache/lucene/codecs/${f} lucene/codecs/src/java/org/apache/lucene/codecs
done
for f in appending block intblock pulsing; do
  svn mv lucene/core/src/test/org/apache/lucene/codecs/${f} lucene/codecs/src/test/org/apache/lucene/codecs
done
