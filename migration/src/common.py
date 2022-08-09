from pathlib import Path
import logging
from datetime import datetime
import functools
import time
import os
import multiprocessing
from logging.handlers import QueueHandler

LOG_DIRNAME = "log"
WORK_DIRNAME = "work"

JIRA_DUMP_DIRNAME = "jira-dump"
JIRA_ATTACHMENTS_DIRPATH = os.getenv("ATTACHMENTS_DL_DIR")
GITHUB_IMPORT_DATA_DIRNAME = "github-import-data"
GITHUB_REMAPPED_DATA_DIRNAME = "github-remapped-data"
MAPPINGS_DATA_DIRNAME = "mappings-data"

JIRA_USERS_FILENAME = "jira-users.csv"
GITHUB_USERS_FILENAME = "github-users.csv"
GITHUB_LUCENE_COMMITTERS_FILENAME = "github-lucene-committers.csv"
GITHUB_LUCENE_COMMIT_AUTHORS = "github-lucene-commit-authors.csv"

ISSUE_MAPPING_FILENAME = "issue-map.csv"
ACCOUNT_MAPPING_FILENAME = "account-map.csv"
JIRA_USERS_FILENAME = "jira-users.csv"

ASF_JIRA_BASE_URL = "https://issues.apache.org/jira/browse"

LOGGING_FOMATTER = logging.Formatter("[%(asctime)s] %(levelname)s:%(module)s: %(message)s")

logging.basicConfig(level=logging.DEBUG, handlers=[])

def logging_setup(log_dir: Path, name: str) -> logging.Logger:
    if not log_dir.exists():
        log_dir.mkdir()
    file_handler = logging.FileHandler(log_dir.joinpath(f'{name}_{datetime.now().isoformat(timespec="seconds")}.log'))
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(LOGGING_FOMATTER)
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(LOGGING_FOMATTER)
    logger = logging.getLogger(name)
    logger.handlers = []  # clear current handlers
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    return logger


# helper to support logging to a single file from multiple processes
# https://docs.python.org/3/howto/logging-cookbook.html#logging-to-a-single-file-from-multiple-processes
def log_listener(log_dir: Path, name: str) -> tuple[multiprocessing.Process, multiprocessing.Queue]:

    def listener_process(queue: multiprocessing.Queue, path: Path):
        file_handler = logging.FileHandler(path)
        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(LOGGING_FOMATTER)
        console_handler = logging.StreamHandler()
        console_handler.setLevel(logging.INFO)
        console_handler.setFormatter(LOGGING_FOMATTER)
        root = logging.getLogger()
        root.addHandler(file_handler)
        root.addHandler(console_handler)

        while True:
            try:
                record: logging.LogRecord = queue.get()
                if record is None:  # sentinel
                    break
                logger = logging.getLogger(record.name)
                logger.handle(record)
            except Exception:
                import sys, traceback
                print('Whoops! Problem:', file=sys.stderr)
                traceback.print_exc(file=sys.stderr)

    if not log_dir.exists():
        log_dir.mkdir()
    path = log_dir.joinpath(f'{name}_{datetime.now().isoformat(timespec="seconds")}.log')
    queue = multiprocessing.Queue(-1)
    listener = multiprocessing.Process(target=listener_process, args=(queue, path))
    return (listener, queue)


def logging_setup_worker(queue: multiprocessing.Queue):
    logger = logging.getLogger()
    queue_handler = QueueHandler(queue)
    logger.handlers = []  # clear current handlers
    logger.addHandler(queue_handler)
    logger.setLevel(logging.DEBUG)


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


def github_remapped_issue_data_file(data_dir: Path, issue_number: int) -> Path:
    return data_dir.joinpath(f"ISSUE-{issue_number}.json")


def github_remapped_comment_data_file(data_dir: Path, comment_id: int) -> Path:
    return data_dir.joinpath(f"COMMENT-{comment_id}.json")


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
            line = line.strip()
            if line.startswith('#'):
                continue
            cols = line.strip().split(",")
            if len(cols) < 2:
                logger.warning(f"Skipping malformed entry {line} (< 2 columns) in {account_mapping_file}")
                continue
            id_map[cols[0]] = cols[1]  # jira name -> github account
    return id_map


def read_jira_users_map(jira_users_file: Path) -> dict[str, str]:
    users_map = {}
    with open(jira_users_file) as fp:
        fp.readline()  # skip header
        for line in fp:
            cols = line.strip().split(",")
            if len(cols) < 2:
                continue
            users_map[cols[0]] = cols[1]  # jira name -> jira display name
    return users_map


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
    "New Feature": "type:enhancement",
    "Improvement": "type:enhancement",
    "Test": "type:test",
    "Wish": "type:enhancement",
    "Task": "type:task",
    "Sub-task": "type:task"
}


COMPONENT_TO_LABEL_MAP = {
    "core": "module:core",
    "modules/analysis": "module:analysis",
    "modules/benchmark": "module:benchmark",
    "modules/classification": "module:classification",
    "modules/examples": "module:demo",
    "modules/expressions": "module:expressions",
    "modules/facet": "module:facet",
    "modules/grouping": "module:grouping",
    "modules/highlighter": "module:highlighter",
    "modules/join": "module:join",
    "modules/luke": "module:luke",
    "modules/misc": "module:misc",
    "modules/monitor": "module:monitor",
    "modules/other": None,
    "modules/queries": "module:queries",
    "modules/query": "mocule:queries",
    "modules/queryparser": "module:queryparser",
    "modules/replicator": "module:replicator",
    "modules/sandbox": "module:sandbox",
    "modules/spatial": "module:spatial",
    "modules/spatial-extras": "module:spatial-extras",
    "modules/spatial3d": "module:spatial3d",
    "modules/suggest": "module:suggest",
    "modules/spellchecker": "module:suggest",
    "modules/test-framework": "module:test-framework",
    "luke": "module:luke",
    "replication (java)": "module:replicator",
    "spatial": "module:spatial",
    "query parsers": "module:queryparser",
    "general/build": "tool:build",
    "general/javadocs": "type:documentation",
    "general/tools": "tool:other",
    "general/test": "type:test",
    "general/website": "website",
    "release wizard": "tool:release-wizard",
    "Build": None,
    "Schema and Analysis": None,
    "Tests": None,
    "contrib - Solr Cell (Tika extraction)": None,
}
