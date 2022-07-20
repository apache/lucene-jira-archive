import os
import sys
import time
from enum import Enum, auto
from random import randint, shuffle
from github_issues_util import *;
from common import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "random_agent")


MIN_INTERVAL = 600
MAX_INTERVAL = 1800

class Command(Enum):
    CREATE_ISSUE = auto(),
    UPDATE_ISSUE = auto(),
    CREATE_COMMENT = auto()


def action(command: Command, new_issues_file: Path, token: str, repo: str) -> bool:
    result = False
    if command is Command.CREATE_ISSUE:
        title = "Issue created by an agent"
        body = "issue created"
        res = create_issue(token, repo, title, body, logger)
        if res:
            (url, num) = res
            with open(new_issues_file, "a") as fp:
                fp.write(f"{url},{num}\n")
            logger.debug(f"Issue created. {num}")
            result = True
    elif command is Command.UPDATE_ISSUE:
        body = "issue updated"
        issue_number = __select_random_issue(new_issues_file)
        if update_issue_body(token, repo, issue_number, body, logger):
            logger.debug(f"Issue updated. {issue_number}")
            result = True
    elif command is Command.CREATE_COMMENT:
        body = "comment added"
        issue_number = __select_random_issue(new_issues_file)
        if create_comment(token, repo, issue_number, body, logger):
            logger.debug(f"Comment added to {issue_number}.")
            result = True
    return result


def __select_random_issue(issues_file: Path) -> int:
    issues = []
    with open(issues_file) as fp:
        for line in fp:
            cols = line.strip().split(",")
            issues.append(int(cols[1]))
    shuffle(issues)
    return issues[0]


if __name__ == '__main__':
    github_token = os.getenv("GITHUB_PAT")
    if not github_token:
        print("Please set your GitHub token to GITHUB_PAT environment variable.")
        sys.exit(1)
    github_repo = os.getenv("GITHUB_REPO")
    if not github_repo:
        print("Please set GitHub repo location to GITHUB_REPO environment varialbe.")
        sys.exit(1)

    check_authentication(github_token)

    work_dir = Path(__file__).resolve().parent.parent.joinpath(WORK_DIRNAME)
    if not work_dir.exists():
        work_dir.mkdir()
    new_issues_file = work_dir.joinpath("new_issues.csv")
    new_issues_file.unlink(missing_ok=True)

    num_issues = 0
    num_updated_issues = 0
    num_comments = 0
    try:
        while True:
            r = randint(0,9)
            if num_issues < 5 or r <= 1:
                if action(Command.CREATE_ISSUE, new_issues_file, github_token, github_repo):
                    num_issues += 1
            elif r <= 5:
                if action(Command.UPDATE_ISSUE, new_issues_file, github_token, github_repo):
                    num_updated_issues += 1
            else:
                if action(Command.CREATE_COMMENT, new_issues_file, github_token, github_repo):
                    num_comments += 1
            time.sleep(randint(MIN_INTERVAL, MAX_INTERVAL))
    except KeyboardInterrupt:
        logger.info(f"Created issues: {num_issues}, Updated issues: {num_updated_issues}, Created comments: {num_comments}")
        sys.exit(0)



