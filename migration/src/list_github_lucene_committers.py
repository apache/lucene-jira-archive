from pathlib import Path
import sys
import os

from common import GITHUB_LUCENE_COMMITTERS_FILENAME, LOG_DIRNAME, WORK_DIRNAME, logging_setup
from github_issues_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "list_github_lucene_committers")


if __name__ == "__main__":
    github_token = os.getenv("GITHUB_PAT")
    if not github_token:
        print("Please set your GitHub token to GITHUB_PAT environment variable.")
        sys.exit(1)

    check_authentication(github_token)

    work_dir = Path(__file__).resolve().parent.parent.joinpath(WORK_DIRNAME)
    if not work_dir.exists():
        work_dir.mkdir()
    assert work_dir.exists()
    gihtub_lucene_committers_file = work_dir.joinpath(GITHUB_LUCENE_COMMITTERS_FILENAME)

    logger.info("Retrieving ASF organization member accounts.")

    asf_members = list_organization_members(github_token, "apache", logger)
    logger.info(f"{len(asf_members)} member accounts were found.")

    with open(gihtub_lucene_committers_file, "w") as fp:
        fp.write("GitHubAccount,Name\n")

    cnt = 0
    for username in asf_members:
        if not check_if_can_be_assigned(github_token, "apache/lucene", username, logger):
            continue
        cnt += 1
        user = get_user(github_token, username, logger)
        with open(gihtub_lucene_committers_file, "a") as fp:
            fp.write(f"{username},{user['name']}\n")
    
    logger.info(f"{cnt} Committer accounts were found; saved in {gihtub_lucene_committers_file}")
    logger.info("Done.")


