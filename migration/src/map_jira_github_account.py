from pathlib import Path
import sys
import os
from collections import namedtuple, defaultdict
from datetime import datetime

from common import GITHUB_USERS_FILENAME, GITHUB_LUCENE_COMMIT_AUTHORS, LOG_DIRNAME, WORK_DIRNAME, JIRA_USERS_FILENAME, MAPPINGS_DATA_DIRNAME, ACCOUNT_MAPPING_FILENAME, logging_setup
from github_issues_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "map_jira_github_account")


JiraUser = namedtuple("JiraUser", ['username', 'dispname'])


def make_mapping_with_dups(jira_user: JiraUser, github_users: defaultdict[list[str]], token: str) -> list[str]:
    res: list[(JiraUser, str, bool)] = []
    github_accounts = github_users.get(jira_user.dispname)
    if not github_accounts:
        return []
    return github_accounts


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
    assert github_user_file.exists()
    gihtub_lucene_authors_file = work_dir.joinpath(GITHUB_LUCENE_COMMIT_AUTHORS)
    assert gihtub_lucene_authors_file.exists()

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
    
    commit_authors = []
    with open(gihtub_lucene_authors_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            author = line.strip()
            commit_authors.append(author)
    
    logger.info("Generating Jira-GitHub account map")
    
    with open(account_mapping_file, "w") as fp:
        fp.write("JiraName,GitHubAccount,JiraDispName,LoggedAsAuthor,HasPushAccess\n")
        for jira_user in jira_users:
            github_accounts = github_users.get(jira_user.dispname)
            if not github_accounts:
                continue
            for account in github_accounts:
                is_author = account in commit_authors
                has_push_access = check_if_can_be_assigned(github_token, "apache/lucene", account, logger)
                fp.write(f"{jira_user.username},{account},{jira_user.dispname},{'yes' if is_author else 'no'},{'yes' if has_push_access else 'no'}\n")
    logger.info(f"Candidate account mapping was written in {account_mapping_file}.")
    logger.info("Done.")
