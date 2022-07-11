from pathlib import Path
import sys
import os
from collections import namedtuple, defaultdict
from datetime import datetime

from common import GITHUB_USERS_FILENAME, LOG_DIRNAME, WORK_DIRNAME, JIRA_USERS_FILENAME, MAPPINGS_DATA_DIRNAME, ACCOUNT_MAPPING_FILENAME, logging_setup
from github_issues_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "map_jira_github_account")


JiraUser = namedtuple("JiraUser", ['username', 'dispname'])


def make_mapping_with_dups(jira_users: list[JiraUser], github_users: defaultdict[list[str]], token: str) -> list[(JiraUser, str, bool)]:
    res: list[(JiraUser, str, bool)] = []
    for jira_user in jira_users:
        github_accounts = github_users.get(jira_user.dispname)
        if not github_accounts:
            continue
        for account in github_accounts:
            has_push_access = check_if_can_be_assigned(token, "apache/lucene", account, logger)
            res.append((jira_user, account, has_push_access))
    return res


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
    assert github_user_file

    mappings_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
    if not mappings_dir.exists():
        mappings_dir.mkdir()
    assert mappings_dir.exists()
    account_mapping_file = mappings_dir.joinpath(ACCOUNT_MAPPING_FILENAME + f".{datetime.now().strftime('%Y%m%d.%H%M%S')}")
    
    jira_users: list[JiraUser] = []
    with open(jira_user_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            cols = line.strip().split(",")
            u = JiraUser(cols[0], cols[1])
            jira_users.append(JiraUser(cols[0], cols[1]))
    
    github_users = defaultdict(list[str])  # name -> list of accounts
    with open(github_user_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            cols = line.strip().split(",")
            github_users[cols[1]].append(cols[0])
    
    logger.info("Generating Jira-GitHub account map")
    account_map = make_mapping_with_dups(jira_users, github_users, github_token)
    
    with open(account_mapping_file, "w") as fp:
        fp.write("JiraName,GitHubAccount,JiraDispName,HasPushAccess\n")
        for jira_user, gh_user, has_push_access in account_map:
            fp.write(f"{jira_user.username},{gh_user},{jira_user.dispname},{'yes' if has_push_access else 'no'}\n")
    logger.info(f"Candidate account mapping was written in {account_mapping_file}.")
    logger.info("Done.")







