#
# Update GitHub issues/comments with re-mapped issue links.
# Usage:
#   python src/update_issues.py --issues <github issue number list>
#   python src/update_issues.py --comments <github comment list>
#   python src/update_issues.py
#

import argparse
from pathlib import Path
import sys
import os
import json

from common import LOG_DIRNAME, GITHUB_REMAPPED_DATA_DIRNAME, MaxRetryLimitExceedException, logging_setup, retry_upto, github_remapped_issue_data_file, github_remapped_comment_data_file
from github_issues_util import *


log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "update_issues")


def update_issue_by_number(issue_number: int, data_dir: Path, token: str, repo: str):
    data_file = github_remapped_issue_data_file(data_dir, issue_number)
    update_issue(data_file, token, repo)


@retry_upto(3, 1.0, logger)
def update_issue(data_file: Path, token: str, repo: str):
    with open(data_file) as fp:
        o = json.load(fp)
        issue_number = o["issue_number"]
        body = o["body"]
        if update_issue_body(token, repo, issue_number, body, logger):
            logger.debug(f"Issue {issue_number} was successfully updated.")
        else:
            raise RuntimeError("Failed to update issue")


def update_comment_by_id(comment_id: int, data_dir: Path, token: str, repo: str):
    data_file = github_remapped_comment_data_file(data_dir, comment_id)
    update_comment(data_file, token, repo)


@retry_upto(3, 1.0, logger)
def update_comment(data_file: Path, token: str, repo: str):
    with open(data_file) as fp:
        o = json.load(fp)
        comment_id = o["comment_id"]
        body = o["body"]
        if update_comment_body(token, repo, comment_id, body, logger):
            logger.debug(f"Comment {comment_id} was successfully updated.")
        else:
            raise RuntimeError("Failed to update comment")


if __name__ == "__main__":
    github_token = os.getenv("GITHUB_PAT")
    if not github_token:
        print("Please set your GitHub token to GITHUB_PAT environment variable.")
        sys.exit(1)
    github_repo = os.getenv("GITHUB_REPO")
    if not github_repo:
        print("Please set GitHub repo location to GITHUB_REPO environment varialbe.")
        sys.exit(1)

    check_authentication(github_token)

    parser = argparse.ArgumentParser()
    parser.add_argument('--issues', type=int, required=False, nargs='*', help='GitHub issue number list to be updated')
    parser.add_argument('--comments', type=int, required=False, nargs='*', help='GitHub comment id list to be updated')
    args = parser.parse_args()

    remapped_data_dir = Path(__file__).resolve().parent.parent.joinpath(GITHUB_REMAPPED_DATA_DIRNAME)
    if not remapped_data_dir.exists():
        remapped_data_dir.mkdir()
    assert remapped_data_dir.exists()
    
    issues = []
    if args.issues:
        issues = args.issues
    comments = []
    if args.comments:
        comments = args.comments
    
    logger.info(f"Updating issues/comments")
    
    if not issues and not comments:
        for data_file in remapped_data_dir.glob("ISSUE-*.json"):
            try:
                update_issue(data_file, github_token, github_repo)
            except MaxRetryLimitExceedException:
                logger.error(f"Failed to update issue body. Skipped {data_file}")
                continue
        for data_file in remapped_data_dir.glob("COMMENT-*.json"):
            try:
                update_comment(data_file, github_token, github_repo)
            except MaxRetryLimitExceedException:
                logger.error(f"Failed to update issue comments. Skipped {data_file}")
                continue
    else:
        for num in issues:
            try:
                update_issue_by_number(num, remapped_data_dir, github_token, github_repo)
            except MaxRetryLimitExceedException:
                logger.error(f"Failed to update issue body. Skipped issue {num}")
                continue
        for id in comments:
            try:
                update_comment_by_id(id, remapped_data_dir, github_token, github_repo)
            except MaxRetryLimitExceedException:
                logger.error(f"Failed to update issue comments. Skipped comment {id}")
                continue

    logger.info("Done.")
