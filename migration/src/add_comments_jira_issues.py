import argparse
import os
import sys
from logging import Logger
from pathlib import Path
import requests
import time
import requests
from common import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "add-comments-jira-issues")

JIRA_API_BASE = "https://issues.apache.org/jira/rest/api/latest"
INTERVAL_IN_SECONDS = 1.0


def __read_issue_url_map(issue_mapping_file: Path) -> dict[str, int]:
    url_map = {}
    with open(issue_mapping_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            cols = line.strip().split(",")
            if len(cols) < 3:
                continue
            url_map[cols[0]] = cols[1]  # jira issue key -> github issue url
    return url_map


def __add_comment(token: str, issue_id: str, comment: str, logger: Logger) -> bool:
    url = JIRA_API_BASE + f"/issue/{issue_id}/comment"
    headers = {"Authorization": f"Bearer {token}"}
    data = {"body": comment}
    res = requests.post(url, headers=headers, json=data)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 201:
        logger.error(f"Failed to add a comment to {issue_id}; status_code={res.status_code}, message={res.text}")
        return False
    return True


@retry_upto(3, 1.0, logger)
def add_moved_to_comment(token: str, issue_id: str, github_url: str) -> bool:
    gh_issue_number = github_url.rsplit("/", 1)[1]
    comment = f"""[TEST] This issue was moved to GitHub issue: [#{gh_issue_number}|{github_url}]."""
    res = __add_comment(token, issue_id, comment, logger)
    if res:
        logger.debug(f"Added a comment to {issue_id}")
    return res


if __name__ == "__main__":
    jira_token = os.getenv("JIRA_PAT")
    if not jira_token:
        print("Please set your Jira token to JIRA_PAT environment variable.")
        sys.exit(1)

    parser = argparse.ArgumentParser()
    parser.add_argument('--issues', type=int, required=False, nargs='*', help='Jira issue number list to be downloaded')    
    args = parser.parse_args()

    mapping_data_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
    issue_mapping_file = mapping_data_dir.joinpath(ISSUE_MAPPING_FILENAME)
    if not issue_mapping_file.exists():
        logger.error(f"Jira-GitHub issue id mapping file not found. {issue_mapping_file}")
        sys.exit(1)
    issue_url_map = __read_issue_url_map(issue_mapping_file)

    issue_ids = []
    if args.issues:
        issue_ids = [jira_issue_id(num) for num in args.issues]
    else:
        issue_ids = list(issue_url_map.keys())

    logger.info("Add comments to Jira issues.")

    for issue_id in issue_ids:
        github_url = issue_url_map.get(issue_id)
        if not github_url:
            logger.warning(f"No corresponding GitHub issue for {issue_id}.")
            continue
        try:
            add_moved_to_comment(jira_token, issue_id, github_url)
        except MaxRetryLimitExceedException:
            logger.error(f"Failed to update issue comments. Skipped {issue_id}.")

    logger.info("Done.")

