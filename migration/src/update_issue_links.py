#
# Update GitHub issues/comments to map Jira key to GitHub issue number
# Usage:
#   python src/update_issue_links.py --issues <issue number list>
#   python src/update_issue_links.py
#

import argparse
from pathlib import Path
import sys
import os

from common import LOG_DIRNAME, MAPPINGS_DATA_DIRNAME, ISSUE_MAPPING_FILENAME, MaxRetryLimitExceedException, logging_setup, read_issue_id_map, retry_upto
from github_issues_util import *
from jira_util import embed_gh_issue_link


log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "update_issue_links")


@retry_upto(3, 1.0, logger)
def update_issue_link_in_issue_body(issue_number: int, issue_id_map: dict[str, str], token: str, repo: str):
    body = get_issue_body(token, repo, issue_number, logger)
    if body:
        updated_body = embed_gh_issue_link(body, issue_id_map)
        if updated_body == body:
            logger.debug(f"Issue {issue_number} does not contain any cross-issue links; nothing to do.")
            return
        if update_issue_body(token, repo, issue_number, updated_body, logger):
            logger.debug(f"Issue {issue_number} was successfully updated.")
            


@retry_upto(3, 1.0, logger)
def update_issue_link_in_comments(issue_number: int, issue_id_map: dict[str, str], token: str, repo: str):
    comments = get_issue_comments(token, repo, issue_number, logger)
    if not comments:
        return
    logger.debug(f"# comments in issue {issue_number} = {len(comments)}")
    for comment in comments:
        id = comment.id
        body = comment.body
        updated_body = embed_gh_issue_link(body, issue_id_map)
        if updated_body == body:
            logger.debug(f"Comment {id} does not contain any cross-issue links; nothing to do.")
            continue
        if update_comment_body(token, repo, id, updated_body, logger):
            logger.debug(f"Comment {id} was successfully updated.")


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
    parser.add_argument('--issues', type=int, required=False, nargs='*', help='Jira issue number list to be downloaded')
    args = parser.parse_args()
    
    mapping_data_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
    issue_mapping_file = mapping_data_dir.joinpath(ISSUE_MAPPING_FILENAME)
    if not issue_mapping_file.exists():
        logger.error(f"Jira-GitHub issue id mapping file not found. {issue_mapping_file}")
        sys.exit(1)
    issue_id_map = read_issue_id_map(issue_mapping_file)
    
    issues = []
    if args.issues:
        issues = args.issues
    else:
        issues = list(issue_id_map.values())
    
    logger.info(f"Updating GitHub issues")
    for num in issues:
        try:
            update_issue_link_in_issue_body(num, issue_id_map, github_token, github_repo)
        except MaxRetryLimitExceedException:
            logger.error(f"Failed to update issue body. Skipped issue {num}")
            continue
        try:
            update_issue_link_in_comments(num, issue_id_map, github_token, github_repo)
        except MaxRetryLimitExceedException:
            logger.error(f"Failed to update issue comments. Skipped issue {num}")
            continue

    logger.info("Done.")