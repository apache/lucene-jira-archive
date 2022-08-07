#
# Remap Jira key to GitHub issue number
# Usage:
#   python src/remap_cross_issue_links.py --issues <github issue number list>
#   python src/remap_cross_issue_links.py
#

import argparse
from pathlib import Path
import sys
import os
import json

from common import LOG_DIRNAME, MAPPINGS_DATA_DIRNAME, ISSUE_MAPPING_FILENAME, GITHUB_REMAPPED_DATA_DIRNAME, MaxRetryLimitExceedException, logging_setup, read_issue_id_map, retry_upto, github_remapped_issue_data_file, github_remapped_comment_data_file
from github_issues_util import *
from jira_util import create_issue_links_outside_projects, embed_gh_issue_link


log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "remap_cross_issue_links")


@retry_upto(3, 1.0, logger)
def remap_issue_link_in_issue_body(issue_number: int, issue_id_map: dict[str, int], data_dir: Path, token: str, repo: str):
    body = get_issue_body(token, repo, issue_number, logger)
    if body:
        updated_body = embed_gh_issue_link(body, issue_id_map, issue_number)
        updated_body = create_issue_links_outside_projects(updated_body)
        if updated_body == body:
            logger.debug(f"Issue {issue_number} does not contain any cross-issue links; nothing to do.")
            return
        data = {"issue_number": issue_number, "body": updated_body}
        data_file = github_remapped_issue_data_file(data_dir, issue_number)
        with open(data_file, "w") as fp:
            json.dump(data, fp=fp, indent=2)
            logger.debug(f"Updated issue body for issue_number={issue_number} was saved to {data_file}.")


@retry_upto(3, 1.0, logger)
def remap_issue_link_in_comments(issue_number: int, issue_id_map: dict[str, int], data_dir: Path, token: str, repo: str):
    comments = get_issue_comments(token, repo, issue_number, logger)
    if not comments:
        return
    logger.debug(f"# comments in issue {issue_number} = {len(comments)}")
    for comment in comments:
        id = comment.id
        body = comment.body
        updated_body = embed_gh_issue_link(body, issue_id_map, issue_number)
        updated_body = create_issue_links_outside_projects(updated_body)
        if updated_body == body:
            logger.debug(f"Comment {id} does not contain any cross-issue links; nothing to do.")
            continue
        data = {"comment_id": id, "body": updated_body}
        data_file = github_remapped_comment_data_file(data_dir, id)
        with open(data_file, "w") as fp:
            json.dump(data, fp=fp, indent=2)
            logger.debug(f"Updated comment body for comment_id={id} was saved to {data_file}.")


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
    mapping_data_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
    issue_mapping_file = mapping_data_dir.joinpath(ISSUE_MAPPING_FILENAME)
    parser.add_argument('--issues', type=int, required=False, nargs='*', help=f'GitHub issue number list to be exported, else all GitHub issues in {issue_mapping_file.relative_to(Path.cwd())}')
    args = parser.parse_args()
    
    if not issue_mapping_file.exists():
        logger.error(f"Jira-GitHub issue id mapping file not found. {issue_mapping_file}")
        sys.exit(1)
    issue_id_map = read_issue_id_map(issue_mapping_file)

    remapped_data_dir = Path(__file__).resolve().parent.parent.joinpath(GITHUB_REMAPPED_DATA_DIRNAME)
    if not remapped_data_dir.exists():
        remapped_data_dir.mkdir()
    assert remapped_data_dir.exists()
    
    issues = []
    if args.issues:
        issues = args.issues
    else:
        issues = list(issue_id_map.values())
    
    logger.info(f"Remapping cross-issue links")
    for num in issues:
        try:
            remap_issue_link_in_issue_body(num, issue_id_map, remapped_data_dir, github_token, github_repo)
        except MaxRetryLimitExceedException:
            logger.error(f"Failed to export/convert issue body. Skipped issue {num}")
            continue
        try:
            remap_issue_link_in_comments(num, issue_id_map, remapped_data_dir, github_token, github_repo)
        except MaxRetryLimitExceedException:
            logger.error(f"Failed to export/convert issue comments. Skipped issue {num}")
            continue

    logger.info("Done.")
