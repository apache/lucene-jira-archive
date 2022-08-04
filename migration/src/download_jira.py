#
# Create local dump of Jira issues 
# Usage:
#   python src/download_jira.py --issues <jira issue number list>
#   python src/download_jira.py --min <min issue number> --max <max issue number>
#

import argparse
from pathlib import Path
import json
import time
from dataclasses import dataclass
import tempfile

import requests

from common import LOG_DIRNAME, JIRA_DUMP_DIRNAME, JIRA_ATTACHMENTS_DIRPATH, logging_setup, jira_dump_file, jira_attachments_dir, jira_issue_id

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "download_jira")

DOWNLOAD_INTERVAL_SEC = 0.5


@dataclass
class Attachment(object):
    filename: str
    created: str
    content: str
    mime_type: str


def issue_uri(issue_id: str) -> str:
    return f"https://issues.apache.org/jira/rest/api/latest/issue/{issue_id}"


def download_issue(num: int, dump_dir: Path) -> bool:
    issue_id = jira_issue_id(num)
    uri = issue_uri(issue_id)
    res = requests.get(uri)
    if res.status_code != 200:
        logger.warning(f"Can't download {issue_id}. status code={res.status_code}, message={res.text}")
        return False
    data = res.json()
    if "key" not in data:
        logger.warning(f"The issue's key does not exist. Skipped {issue_id}")
        return False
    if data["key"] != issue_id:
        logger.warning(f"The issue key {data['key']} does not match the request key {issue_id}. Maybe this was moved.")
        return False
    dump_file = jira_dump_file(dump_dir, num)
    with open(dump_file, "w") as fp:
        json.dump(data, fp, indent=2)
    logger.debug(f"Jira issue {issue_id} was downloaded in {dump_file}.")
    return True


def download_attachments(num: int, dump_dir: Path, att_data_dir: Path):
    dump_file = jira_dump_file(dump_dir, num)
    assert dump_file.exists()
    attachments_dir = jira_attachments_dir(att_data_dir, num)
    if not attachments_dir.exists():
        attachments_dir.mkdir()
    
    files: dict[str, Attachment] = {}
    with open(dump_file) as fp:
        o = json.load(fp)
        attachments = o.get("fields").get("attachment")
        if not attachments:
            return
        for a in attachments:
            filename = a.get("filename")
            created = a.get("created")
            content = a.get("content")
            mime_type = a.get("mimeType")
            if not (filename and created and content and mime_type):
                continue
            if filename not in files or created > files[filename].created:
                files[filename] = Attachment(filename=filename, created=created, content=content, mime_type=mime_type)

    for (_, a) in files.items():
        logger.debug(f"Downloading attachment {a.filename}")
        res = requests.get(a.content, headers={"Accept": a.mime_type})
        if res.status_code != 200:
            logger.error(f"Failed to download attachment {a.filename} in issue {jira_issue_id(num)}")
            continue
        attachment_file = attachments_dir.joinpath(a.filename)
        with open(attachment_file, "wb") as fp:
            fp.write(res.content)
        time.sleep(DOWNLOAD_INTERVAL_SEC)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--issues', type=int, required=False, nargs='*', help='Jira issue number list to be downloaded')    
    parser.add_argument('--min', type=int, dest='min', required=False, default=1, help='Minimum (inclusive) Jira issue number to be downloaded')
    parser.add_argument('--max', type=int, dest='max', required=False, help='Maximum (inclusive) Jira issue number to be downloaded')
    parser.add_argument('--skip_attachments', type=bool, dest='skip_attachments', required=False, help='Do not download attachments (they are already (mostly?) cached in this repo)', action=argparse.BooleanOptionalAction)
    args = parser.parse_args()

    dump_dir = Path(__file__).resolve().parent.parent.joinpath(JIRA_DUMP_DIRNAME)
    if not dump_dir.exists():
        dump_dir.mkdir()
    assert dump_dir.exists()

    att_data_dir = Path(JIRA_ATTACHMENTS_DIRPATH) if JIRA_ATTACHMENTS_DIRPATH else Path(tempfile.gettempdir()).joinpath("attachments")
    if not att_data_dir.exists():
        att_data_dir.mkdir()
    assert att_data_dir.exists()

    issues = []
    if args.issues:
        issues = args.issues
    else:
        if args.max:
            issues.extend(list(range(args.min, args.max + 1)))
        else:
            issues.append(args.min)

    message = f"Downloading Jira issues in {dump_dir}."
    if args.skip_attachments:
        message += " Attachments will not be saved."
    else:
        message += f" Attachments will be saved in {att_data_dir}."
    logger.info(message)
    for num in issues:
        if download_issue(num, dump_dir) and not args.skip_attachments:
            download_attachments(num, dump_dir, att_data_dir)
        time.sleep(DOWNLOAD_INTERVAL_SEC)
    
    logger.info("Done.")
    

    
