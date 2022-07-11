from pathlib import Path
import tempfile
import sys
import os

from common import GITHUB_USERS_FILENAME, LOG_DIRNAME, WORK_DIRNAME, JIRA_USERS_FILENAME, logging_setup
from github_issues_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "list_github_user_candidates")


if __name__ == "__main__":
    github_token = os.getenv("GITHUB_PAT")
    if not github_token:
        print("Please set your GitHub token to GITHUB_PAT environment variable.")
        sys.exit(1)

    check_authentication(github_token)

    work_dir = Path(__file__).resolve().parent.parent.joinpath(WORK_DIRNAME)
    jira_user_file = work_dir.joinpath(JIRA_USERS_FILENAME)
    assert jira_user_file.exists()
    github_user_file = work_dir.joinpath(GITHUB_USERS_FILENAME)

    full_names = []
    with open(jira_user_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            cols = line.strip().split(",")
            full_name = cols[1]
            full_names.append(full_name)

    candidates = []
    with tempfile.TemporaryDirectory() as tmpdir:
        tempfile = Path(tmpdir).joinpath("candidates")

        logger.info("Searching GitHub users")
        for name in full_names:
            usernames = search_users(github_token, f"{name} in:name", logger)
            with open(tempfile, "a") as fp:
                for user in usernames:
                    fp.write(user + "\n")
        usernames = set({})
        with open(tempfile) as fp:
            for line in fp:
                usernames.add(line.strip())

        logger.info("Retrieving GitHub users info")
        for username in usernames:
            user = get_user(github_token, username, logger)
            name = user["name"]
            candidates.append((username, name))
    
    with open(github_user_file, "w") as fp:
        fp.write("GitHubAccount,Name\n")
        for a, n in candidates:
            fp.write(f"{a},{n}\n")

    logger.info(f"{len(candidates)} candidate accounts were found; saved in {github_user_file}")
    logger.info("Done.")
