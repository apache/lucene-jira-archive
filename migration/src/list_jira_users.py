from pathlib import Path
import json
from collections import defaultdict

from common import JIRA_DUMP_DIRNAME, LOG_DIRNAME, WORK_DIRNAME, JIRA_USERS_FILENAME, logging_setup
from jira_util import *


log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "list_jira_users")


def extract_authors(jira_dump_file: Path) -> set[tuple[str, str]]:
    assert jira_dump_file.exists()
    authors = set([])
    with open(jira_dump_file) as fp:
        o = json.load(fp)
        reporter = extract_reporter(o)
        authors.add(reporter)
        assignee = extract_assignee(o)
        authors.add(assignee)
        comment_authors = set(__extract_comment_authors(o))
        authors |= comment_authors
    return authors


def __extract_comment_authors(o: dict) -> tuple[str, str]:
    comments = extract_comments(o)
    return [(c[0], c[1]) for c in comments]


if __name__ == "__main__":
    dump_dir = Path(__file__).resolve().parent.parent.joinpath(JIRA_DUMP_DIRNAME)
    work_dir = Path(__file__).resolve().parent.parent.joinpath(WORK_DIRNAME)
    if not work_dir.exists():
        work_dir.mkdir()
    assert work_dir.exists()
    jira_user_file = work_dir.joinpath(JIRA_USERS_FILENAME)

    logger.info("Listing Jira users")
    counts = defaultdict(int)
    for child in dump_dir.iterdir():
        if child.is_file() and child.name.endswith(".json"):
            authors = extract_authors(child)
            for author in authors:
                counts[author] += 1
    counts = sorted(counts.items(), key=lambda x: x[1], reverse=True)

    with open(jira_user_file, "w") as fp:
        fp.write("JiraName,DispName\n")
        for a, _ in counts:
            if len(a[0]) == 0:
                continue
            fp.write(f"{a[0]},{a[1]}\n")
    
    logger.info(f"All Jira usernames and display names were saved in {jira_user_file}.")
    logger.info("Done.")