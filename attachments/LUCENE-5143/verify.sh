#!/bin/sh
# echo "Fetching lucene file list (NB: May get rate blocked by apache)"
# lftp -e "set net:limit-rate 1000:1000; du -a >_lucene.txt;quit" https://archive.apache.org/dist/lucene/java/
# cat _lucene.txt |cut -f 2 |grep src|grep -vE "md5|sha1|zip" |grep -v archive >_lucene_files.txt
# sed 's|^\.|https://archive.apache.org/dist/lucene/java|g' <_lucene_files.txt >_lucene_src_urls.txt

# echo "Fetching solr file list (NB: May get rate blocked by apache)"
# lftp -e "set net:limit-rate 1000:1000; du -a >solr.txt;quit" https://archive.apache.org/dist/lucene/solr/
# cat _solr.txt |cut -f 2 |grep src|grep -vE "md5|sha1|zip" |grep -v archive >_solr_files.txt
# sed 's|^\.|https://archive.apache.org/dist/lucene/solr|g' <_solr_files.txt >_solr_src_urls.txt

# echo "Downloading src artifacts with detached sigs"
# wget -i _lucene_src_urls.txt
# wget -i _solr_src_urls.txt

# echo Downloading KEYS file from JIRA
# rm KEYS >/dev/null 2>&1
# wget https://issues.apache.org/jira/secure/attachment/12917687/KEYS

echo "Import KEYS in clean gpg (./gnupg) using Docker"
rm -rf ./gnupg >/dev/null 2>&1
mkdir gnupg
docker run --rm -it -v $(pwd)/gnupg:/root/.gnupg -v $(pwd):/lucene vladgh/gpg --import /lucene/KEYS

echo "Verifying all artifacts"
rm _SIG_FAIL.txt _SIG_FAIL.log _SIG_SUCCESS.txt >/dev/null 2>&1 ; touch _SIG_FAIL.txt _SIG_FAIL.log _SIG_SUCCESS.txt
for artifact in $(ls *.asc); do
    docker run --rm -it -v $(pwd)/gnupg:/root/.gnupg -v $(pwd):/lucene vladgh/gpg --verify /lucene/$artifact >tmp.txt 2>&1
    if [ ! $? -eq 0 ]; then
        echo $artifact >>_SIG_FAIL.txt
        echo "====================================================================" >>_SIG_FAIL.log
        cat tmp.txt >>_SIG_FAIL.log ; rm tmp.txt
        echo "$artifact [FAIL]"
    else
        echo $artifact >>_SIG_SUCCESS.txt
        echo "$artifact [OK]"
    fi
done

echo
echo "SUMMARY"
echo "======="
echo "Number of artifacts to check: $(ls *.asc |wc -l)"
echo "Number of artifacts checked : $(cat _SIG_SUCCESS.txt _SIG_FAIL.txt |wc -l)"
echo "Number of artifacts SUCCESS : $(cat _SIG_SUCCESS.txt |wc -l)"
echo "Number of artifacts FAILED  : $(cat _SIG_FAIL.txt |wc -l)"
echo
echo "FAILED artifacts:"
cat _SIG_FAIL.log

echo DONE
