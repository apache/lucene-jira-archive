#!/bin/bash
branctotag() {
    BRANCH=$1
    REMOTE=$2

    if [[ $# -lt 2 ]]; then
        echo "Usage: ./branchToTag.sh <branch-name> <remote>"
        echo "Example: ./branchToTag.sh jira/SOLR-123 origin"
        echo "  ...will create tag history/branches/lucene-solr/jira/SOLR-123 and delete the branch"
        exit 0
    fi

    echo "Creating tag history/branches/lucene-solr/$BRANCH in remote $REMOTE"
    git push ${REMOTE} $REMOTE/$BRANCH:refs/tags/history/branches/lucene-solr/$BRANCH
    if [[ $? -eq 0 ]]; then
        echo "Deleting branch $BRANCH from remote $REMOTE"
        git push ${REMOTE} --delete $BRANCH
    else
        echo "Command to create tag failed, will not delete branch $BRANCH"
    fi
}

# Sample output from one move operation:
# Creating tag history/branches/lucene-solr/mytest2 in remote cominvent
# Total 0 (delta 0), reused 0 (delta 0), pack-reused 0
# To https://github.com/cominvent/solr.git
#  * [new tag]                 cominvent/mytest2 -> history/branches/lucene-solr/mytest2
# Deleting branch mytest2 from remote cominvent
# To https://github.com/cominvent/solr.git
#  - [deleted]                 mytest2


# Solr branches
branctotag SOLR-13105-visual origin
branctotag SOLR-13635-solrlogstream origin
branctotag SOLR-14866 origin
branctotag SOLR-14882 origin
branctotag jira/SOLR-13101 origin
branctotag jira/SOLR-13488 origin
branctotag jira/SOLR-13608 origin
branctotag jira/SOLR-14713 origin
branctotag jira/solr-12730 origin
branctotag jira/solr-13004 origin
branctotag jira/solr-13004-8x origin
branctotag jira/solr-13350-new origin
branctotag jira/solr-13579 origin
branctotag jira/solr-13951-overseer-metrics origin
branctotag jira/solr-14067 origin
branctotag jira/solr-14275 origin
branctotag jira/solr-14381-8x origin
branctotag jira/solr-14381-master origin
branctotag jira/solr-14613 origin
branctotag jira/solr-14749 origin
branctotag jira/solr-14749-api origin
branctotag jira/solr-14749-cluster-singleton origin
branctotag jira/solr-14749-scheduler origin
branctotag jira/solr-14792 origin
branctotag jira/solr-14827-8x origin
branctotag jira/solr-14830 origin
branctotag jira/solr-14830-legacy-removal origin
branctotag jira/solr-14900-8x origin
branctotag jira/solr-14985 origin
branctotag jira/solr-15016 origin
branctotag jira/solr-9840 origin
branctotag jira/solr13951 origin
branctotag jira/solr14003 origin
branctotag jira/solr14089_backup origin
branctotag jira/solr14101 origin
branctotag jira/solr14275 origin
branctotag jira/solr14593 origin
branctotag jira/solr14712 origin
branctotag jira/solr14712_1 origin
branctotag jira/solr14712_impl origin
branctotag jira/solr14977 origin
branctotag solr/main origin
branctotag reference_impl origin
branctotag reference_impl_dev origin
branctotag jira/solr14827 origin
branctotag jira/solr-15051-blob origin
branctotag jira/solr-13350 origin
branctotag jira/solr-13350-8x origin
branctotag jira/solr-15019 origin
branctotag jira/solr14155 origin
branctotag jira/solr14155-1 origin
branctotag jira/solr_13951 origin
branctotag jira/solr-15052 origin
branctotag jira/solr-15052-8x origin
branctotag jira/SOLR-14608-export origin
branctotag jira/SOLR-14608-export-merge origin
branctotag jira/solr-13105-merge2 origin
branctotag jira/solr-13105-toMerge origin
branctotag jira/solr-15055 origin
branctotag jira/solr-15055-2 origin
branctotag jira/solr15094 origin
branctotag jira/solr-15131 origin
branctotag jira/solr15138 origin
branctotag jira/solr15138_2 origin
branctotag jira/solr15138_3 origin
branctotag jira/solr-15130 origin
branctotag jira/solr-15130-2 origin
branctotag jira/solr-15212 origin
branctotag jira/solr-15210 origin
branctotag visual-guide origin


# Historic release branches
branctotag branch_3x origin
branctotag branch_4x origin
branctotag branch_5_4 origin
branctotag branch_5_5 origin
branctotag branch_5x origin
branctotag branch_6_0 origin
branctotag branch_6_1 origin
branctotag branch_6_2 origin
branctotag branch_6_3 origin
branctotag branch_6_4 origin
branctotag branch_6_5 origin
branctotag branch_6_6 origin
branctotag branch_6x origin
branctotag branch_7 origin
branctotag branch_7_0 origin
branctotag branch_7_1 origin
branctotag branch_7_2 origin
branctotag branch_7_3 origin
branctotag branch_7_4 origin
branctotag branch_7_5 origin
branctotag branch_7_6 origin
branctotag branch_7_7 origin
branctotag branch_7x origin
branctotag branch_8_0 origin
branctotag branch_8_1 origin
branctotag branch_8_2 origin
branctotag branch_8_3 origin
branctotag branch_8_4 origin
branctotag branch_8_5 origin
branctotag branch_8_6 origin
branctotag branch_8_7 origin
branctotag branch_8x origin
branctotag branch_8_8 origin


# Lucene feature branches
branctotag LUCENE-9004 origin
branctotag LUCENE-9670 origin
branctotag jira/LUCENE-8692 origin
branctotag jira/lucene-9004-aknn-2 origin
branctotag jira/lucene-9302 origin
branctotag lucene/main origin
branctotag revert-962-jira/LUCENE-9021 origin
branctotag pointvalues origin
branctotag master-deprecations origin

