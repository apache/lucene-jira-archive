#!/bin/bash

: ${BRANCH:="master"}
: ${PATCH:="LUCENE-8585_${BRANCH}.patch"}
: ${PATCH_PURE:="LUCENE-8585.patch"}
: ${PATCH_DATE:="${PATCH}.$(date +%Y%m%d)"}
: ${FILES:=$(git log --name-status --author "Toke Eskildsen"  | grep -m 1 -B 9999 6c111611118ceda0837f25a27e5b4549f2693457 | grep core/src | sed 's/^.\s*//' | sort | uniq | tr '\n' ' ')}

rm -r patch_files/
mkdir -p patch_files
cp $FILES patch_files/

git diff $BRANCH HEAD -- $FILES > "$PATCH"
cp "$PATCH" "$PATCH_DATE"
cp "$PATCH" "$PATCH_PURE"

echo "Files in patch (and also in the 'patch_files' folder)"
echo "$FILES" | tr ' ' '\n' | tee patch_files/files.lst

echo "$PATCH"

