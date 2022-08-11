#
# Import GitHub issues via Import Issues API (https://gist.github.com/jonmagic/5282384165e0f86ef105)
# Usage:
#   python src/import_github_issues.py --issues <issue number list>
#   python src/import_github_issues.py --min <min issue number> --max <max issue number>
#

import argparse
from pathlib import Path
import json
import sys
import os
import time

from common import LOG_DIRNAME, GITHUB_IMPORT_DATA_DIRNAME, MAPPINGS_DATA_DIRNAME, ISSUE_MAPPING_FILENAME, logging_setup, jira_issue_id, github_data_file, retry_upto, MaxRetryLimitExceedException
from github_issues_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "import_github_issues")


def issue_web_url(repo: str, issue_number: str) -> str:
    return f"https://github.com/{repo}/issues/{issue_number}"


@retry_upto(3, 1.0, logger)
def import_issue_with_comments(num: int, data_dir: Path, token: str, repo: str) -> Optional[tuple[str, str]]:
    data_file = github_data_file(data_dir, num)
    if not data_file.exists():
        return None
    with open(data_file) as fp:
        issue_data = json.load(fp)
        assignee = issue_data["issue"].get("assignee")
        if assignee and not check_if_can_be_assigned(token, repo, assignee, logger):
            # this field should be removed; otherwise an error occurs.
            del issue_data["issue"]["assignee"]
        url = import_issue(token, repo, issue_data, logger)
        (status, issue_url, errors) = ("pending", "", [])
        while not status or status == "pending":
            (status, issue_url, errors) = get_import_status(token, url, logger)
            time.sleep(3)
        if status == "imported":
            issue_number = issue_url.rsplit("/", maxsplit=1)[1]
            web_url = issue_web_url(repo, issue_number)
            logger.debug(f"Import GitHub issue {web_url} was successfully completed.")
            return (web_url, issue_number)
        else:
            logger.error(f"Import GitHub issue {data_file} was failed. status={status}, errors={errors}")
        
        raise RuntimeError("Importing issue failed")


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
    parser.add_argument('--min', type=int, dest='min', required=False, default=1, help='Minimum Jira issue number to be converted')
    parser.add_argument('--max', type=int, dest='max', required=False, help='Maximum Jira issue number to be converted')
    args = parser.parse_args()

    github_data_dir = Path(__file__).resolve().parent.parent.joinpath(GITHUB_IMPORT_DATA_DIRNAME)
    if not github_data_dir.exists():
        logger.error(f"GitHub data dir not exists. {github_data_dir}")
        sys.exit(1)
    
    mapping_data_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
    if not mapping_data_dir.exists():
        mapping_data_dir.mkdir()
    issue_mapping_file = mapping_data_dir.joinpath(ISSUE_MAPPING_FILENAME)
    if not issue_mapping_file.exists():
        with open(issue_mapping_file, "w") as fp:
            fp.write("JiraKey,GitHubUrl,GitHubNumber\n")   
    
    issues = []
    if args.issues:
        issues = args.issues
    else:
        if args.max:
            issues.extend(list(range(args.min, args.max + 1)))
        else:
            issues.append(args.min)

    logger.info(f"Importing GitHub issues")
    for num in issues:
        try:
            res = import_issue_with_comments(num, github_data_dir, github_token, github_repo)
            if res:
                (issue_url, issue_number) = res
                with open(issue_mapping_file, "a") as fp:
                    fp.write(f"{jira_issue_id(num)},{issue_url},{issue_number}\n")
        except MaxRetryLimitExceedException:
            logger.error(f"Failed to import issue to GitHub. Skipped issue {num}")
            continue

    logger.info("Done.")