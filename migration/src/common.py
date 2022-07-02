from pathlib import Path
import logging
from datetime import datetime
import functools
import time
import os
import tempfile

LOG_DIRNAME = "log"

JIRA_DUMP_DIRNAME = "jira-dump"
JIRA_ATTACHMENTS_DIRPATH = os.getenv("ATTACHMENTS_DL_DIR", str(Path(tempfile.gettempdir()).joinpath("attachments")))
GITHUB_IMPORT_DATA_DIRNAME = "github-import-data"
MAPPINGS_DATA_DIRNAME = "mappings-data"

ISSUE_MAPPING_FILENAME = "issue-map.csv"
ACCOUNT_MAPPING_FILENAME = "account-map.csv"

ASF_JIRA_BASE_URL = "https://issues.apache.org/jira/browse"


logging.basicConfig(level=logging.DEBUG, handlers=[])

def logging_setup(log_dir: Path, name: str) -> logging.Logger:
    if not log_dir.exists():
        log_dir.mkdir()
    formatter = logging.Formatter("[%(asctime)s] %(levelname)s:%(module)s: %(message)s")
    file_handler = logging.FileHandler(log_dir.joinpath(f'{name}_{datetime.now().isoformat(timespec="seconds")}.log'))
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)
    logger = logging.getLogger(name)
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    return logger


def jira_issue_url(issue_id: str) -> str:
    return ASF_JIRA_BASE_URL + f"/{issue_id}"


def jira_issue_id(issue_number: int) -> str:
    return f"LUCENE-{issue_number}"


def jira_dump_file(dump_dir: Path, issue_number: int) -> Path:
    issue_id = jira_issue_id(issue_number)
    return dump_dir.joinpath(f"{issue_id}.json")


def jira_attachments_dir(data_dir: Path, issue_number: int) -> Path:
    issue_id = jira_issue_id(issue_number)
    return data_dir.joinpath(issue_id)


def github_data_file(data_dir: Path, issue_number: int) -> Path:
    issue_id = jira_issue_id(issue_number)
    return data_dir.joinpath(f"GH-{issue_id}.json")


def make_github_title(summary: str, jira_id: str) -> str:
    return f"{summary} [{jira_id}]"


def read_issue_id_map(issue_mapping_file: Path) -> dict[str, int]:
    id_map = {}
    with open(issue_mapping_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            cols = line.strip().split(",")
            if len(cols) < 3:
                continue
            id_map[cols[0]] = int(cols[2])  # jira issue key -> github issue number
    return id_map


def read_account_map(account_mapping_file: Path) -> dict[str, str]:
    id_map = {}
    with open(account_mapping_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            cols = line.strip().split(",")
            if len(cols) < 2:
                continue
            id_map[cols[0]] = cols[1]  # jira name -> github account
    return id_map


def retry_upto(max_retry: int, interval: float, logger: logging.Logger):
    def retry(func):
        @functools.wraps(func)
        def _retry(*args, **kwargs):
            retry = 0
            while retry < max_retry:
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    retry += 1
                    logger.warning(f"Exception raised during function call {func}. error={str(e)} (retry={retry})")
                    time.sleep(interval)
                    continue
            if retry == max_retry:
                raise MaxRetryLimitExceedException()
            return None
        return _retry
    return retry


class MaxRetryLimitExceedException(Exception):
    pass


ISSUE_TYPE_TO_LABEL_MAP = {
    "Bug": "type:bug",
    "New Feature": "type:new_feature",
    "Improvement": "type:enhancement",
    "Test": "type:test",
    "Wish": "type:enhancement",
    "Task": "type:task"
}


COMPONENT_TO_LABEL_MAP = {
    "core": "component:module/core",
    "modules/analysis": "component:module/analysis",
    "modules/benchmark": "component:module/benchmark",
    "modules/classification": "component:module/classification",
    "modules/expressions": "component:module/expressions",
    "modules/facet": "component:module/facet",
    "modules/grouping": "component:module/grouping",
    "modules/highlithter": "component:module/highlithter",
    "modules/join": "component:module/join",
    "modules/luke": "component:module/luke",
    "modules/monitor": "component:module/monitor",
    "modules/queryparser": "component:module/queryparser",
    "modules/replicator": "component:module/replicator",
    "modules/sandbox": "component:module/sandbox",
    "modules/spatial": "component:module/spatial",
    "modules/spatial-extras": "component:module/spatial-extras",
    "modules/spatial3d": "component:module/spatial3d",
    "modules/suggest": "component:module/suggest",
    "modules/spellchecker": "component:module/suggest",
    "modules/test-framework": "component:module/test-framework",
    "luke": "component:module/luke",
    "general/build": "component:general/build",
    "general/javadocs": "component:general/javadocs",
    "general/test": "component:general/test",
    "general/website": "component:general/website",
    "release wizard": "component:general/release wizard",
}