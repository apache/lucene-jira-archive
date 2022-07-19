from pathlib import Path
import os
import sys
from common import LOG_DIRNAME, logging_setup
from github_issues_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "update_issue_labels")


# label prefix -> color, description
LABEL_DETAILS_MAP = {
    "type:": ("ffbb00", ""),
    "fix-version:": ("7ebea5", ""),
    "affects-version:": ("f19072", ""),
    "module:": ("a0d8ef", ""),
    "tool:": ("a0d8ef", "")
}


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

    logger.info("Retrieving labels.")

    labels = list_labels(github_token, github_repo, logger)
    logger.info(f"{len(labels)} labels are found.")

    for label in labels:
        for prefix, detail in LABEL_DETAILS_MAP.items():
            if label.startswith(prefix):
                update_label(github_token, github_repo, label, detail[0], detail[1], logger)

    print("Done.")