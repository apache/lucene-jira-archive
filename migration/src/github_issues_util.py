from dataclasses import dataclass, field
from typing import Any, Optional
import time
from logging import Logger
import requests


GITHUB_API_BASE = "https://api.github.com"
INTERVAL_IN_SECONDS = 1.0


@dataclass
class GHIssueComment:
    id: int
    body: str


def check_authentication(token: str):
    check_url = GITHUB_API_BASE + "/user"
    headers = {"Authorization": f"token {token}"}
    res = requests.get(check_url, headers=headers)
    assert res.status_code == 200, f"Authentication failed. Please check your GitHub token. status_code={res.status_code}, message={res.text}"


def get_issue_body(token: str, repo: str, issue_number: int, logger: Logger) -> Optional[str]:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues/{issue_number}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    res = requests.get(url, headers=headers)
    if res.status_code != 200:
        logger.error(f"Failed to get issue {issue_number}; status_code={res.status_code}, message={res.text}")
        return None
    time.sleep(INTERVAL_IN_SECONDS)
    return res.json().get("body")


def update_issue_body(token: str, repo: str, issue_number: int, body: str, logger: Logger) -> bool:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues/{issue_number}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    data = {"body": body}
    res = requests.patch(url, headers=headers, json=data)
    if res.status_code != 200:
        logger.error(f"Failed to update issue {issue_number}; status_code={res.status_code}, message={res.text}")
        return False
    time.sleep(INTERVAL_IN_SECONDS)
    return True


def get_issue_comments(token: str, repo: str, issue_number: int, logger: Logger) -> list[GHIssueComment]:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues/{issue_number}/comments?per_page=100"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    li = []
    stop = False
    page = 1
    while not stop:
        url_with_paging = url + f"&page={page}"
        res = requests.get(url_with_paging, headers=headers)
        if res.status_code != 200:
            logger.error(f"Failed to get issue comments for {issue_number}; status_code={res.status_code}, message={res.text}")
            break
        if not res.json():
            stop = True
        for comment in res.json():
            li.append(GHIssueComment(id=comment.get("id"), body=comment.get("body")))
        page += 1
        time.sleep(INTERVAL_IN_SECONDS)
    return li


def update_comment_body(token: str, repo: str, comment_id: int, body: str, logger: Logger) -> bool:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues/comments/{comment_id}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    data = {"body": body}
    res = requests.patch(url, headers=headers, json=data)
    if res.status_code != 200:
        logger.error(f"Failed to update comment {comment_id}; status_code={res.status_code}, message={res.text}")
        return False
    time.sleep(INTERVAL_IN_SECONDS)
    return True


def import_issue(token: str, repo: str, issue_data: dict, logger: Logger) -> str:
    url = GITHUB_API_BASE + f"/repos/{repo}/import/issues"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.golden-comet-preview+json"}
    res = requests.post(url, headers=headers, json=issue_data)
    if res.status_code != 202:
        logger.error(f"Failed to import issue {issue_data['issue']['title']}; status_code={res.status_code}, message={res.text}")
    time.sleep(INTERVAL_IN_SECONDS)
    return res.json().get("url")


def get_import_status(token: str, url: str, logger: Logger) -> Optional[tuple[str, str, list[Any]]]:
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.golden-comet-preview+json"}
    res = requests.get(url, headers=headers)
    if res.status_code != 200:
        logger.error(f"Failed to get import status for {url}; status code={res.status_code}, message={res.text}")
        return None
    return (res.json().get("status"), res.json().get("issue_url", ""), res.json().get("errors", []))


def check_if_can_be_assigned(token: str, repo: str, assignee: str, logger: Logger) -> bool:
    url = GITHUB_API_BASE + f"/repos/{repo}/assignees/{assignee}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    res = requests.get(url, headers=headers)
    if res.status_code == 204:
        return True
    else:
        logger.warning(f"Assignee {assignee} cannot be assigned; status code={res.status_code}, message={res.text}")
        return False
