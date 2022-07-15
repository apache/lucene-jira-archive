from pathlib import Path
import sys
import os

from common import GITHUB_LUCENE_COMMIT_AUTHORS, LOG_DIRNAME, WORK_DIRNAME, logging_setup
from github_issues_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "list_github_lucene_commit_authors")


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
    gihtub_lucene_authors_file = work_dir.joinpath(GITHUB_LUCENE_COMMIT_AUTHORS)

    logger.info("Retrieving commit authors")

    authors = list_commit_authors(github_token, "apache/lucene", logger)

    with open(gihtub_lucene_authors_file, "w") as fp:
        fp.write("GitHubAccount\n")
        for author in authors:
            fp.write(f"{author}\n")
    
    logger.info(f"{len(authors)} authors were found; saved in {gihtub_lucene_authors_file}")
    logger.info("Done.")
