from dataclasses import dataclass, field
from typing import Any, Optional
import time
from logging import Logger
from urllib.parse import quote_plus
import requests


GITHUB_API_BASE = "https://api.github.com"
# https://docs.github.com/en/rest/overview/resources-in-the-rest-api#rate-limiting
INTERVAL_IN_SECONDS = 1.0
# https://docs.github.com/en/rest/search#rate-limit
SEARCH_INTERVAL_IN_SECONDS = 5

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
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 200:
        logger.error(f"Failed to get issue {issue_number}; status_code={res.status_code}, message={res.text}")
        return None
    return res.json().get("body")


def create_issue(token: str, repo: str, title: str, body: str, logger: Logger) -> Optional[tuple[str, int]]:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    data = {"title": title, "body": body}
    res = requests.post(url, headers=headers, json=data)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 201:
        logger.error(f"Failed to create issue; status_code={res.status_code}, message={res.text}")
        return None
    html_url = res.json()["html_url"]
    number = res.json()["number"]
    return (html_url, number)


def update_issue_body(token: str, repo: str, issue_number: int, body: str, logger: Logger) -> bool:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues/{issue_number}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    data = {"body": body}
    res = requests.patch(url, headers=headers, json=data)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 200:
        logger.error(f"Failed to update issue {issue_number}; status_code={res.status_code}, message={res.text}")
        return False
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
        time.sleep(INTERVAL_IN_SECONDS)
        if res.status_code != 200:
            logger.error(f"Failed to get issue comments for {issue_number}; status_code={res.status_code}, message={res.text}")
            break
        if not res.json():
            stop = True
        for comment in res.json():
            li.append(GHIssueComment(id=comment.get("id"), body=comment.get("body")))
        page += 1
    return li


def create_comment(token: str, repo: str, issue_number: int, body: str, logger: Logger) -> Optional[int]:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues/{issue_number}/comments"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    data = {"body": body}
    res = requests.post(url, headers=headers, json=data)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 201:
        logger.error(f"Failed to create comment; status_code={res.status_code}, message={res.text}")
        return None
    id = res.json()["id"]
    return id


def update_comment_body(token: str, repo: str, comment_id: int, body: str, logger: Logger) -> bool:
    url = GITHUB_API_BASE + f"/repos/{repo}/issues/comments/{comment_id}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    data = {"body": body}
    res = requests.patch(url, headers=headers, json=data)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 200:
        logger.error(f"Failed to update comment {comment_id}; status_code={res.status_code}, message={res.text}")
        return False
    return True


def import_issue(token: str, repo: str, issue_data: dict, logger: Logger) -> str:
    url = GITHUB_API_BASE + f"/repos/{repo}/import/issues"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.golden-comet-preview+json"}
    res = requests.post(url, headers=headers, json=issue_data)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 202:
        logger.error(f"Failed to import issue {issue_data['issue']['title']}; status_code={res.status_code}, message={res.text}")
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
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code == 204:
        return True
    else:
        logger.warning(f"Assignee {assignee} cannot be assigned; status code={res.status_code}, message={res.text}")
        return False


def search_users(token: str, q: str, logger: Logger) -> list[str]:
    url = GITHUB_API_BASE + f"/search/users?q={quote_plus(q)}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    res = requests.get(url, headers=headers)
    time.sleep(SEARCH_INTERVAL_IN_SECONDS)    
    if res.status_code != 200:
        logger.error(f"Failed to search users with query {q}; status_code={res.status_code}, message={res.text}")
        return []
    return [item["login"] for item in res.json()["items"]]


def get_user(token: str, username: str, logger: Logger) -> Optional[dict[str, Any]]:
    url = GITHUB_API_BASE + f"/users/{username}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    res = requests.get(url, headers=headers)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 200:
        logger.error(f"Failed to get user {username}; status_code={res.status_code}, message={res.text}")
        return None
    return res.json()


def list_organization_members(token: str, org: str, logger: Logger) -> list[str]:
    url = GITHUB_API_BASE + f"/orgs/{org}/members?per_page=100"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    page = 1
    users = []
    while True:
        res = requests.get(f"{url}&page={page}", headers=headers)
        time.sleep(INTERVAL_IN_SECONDS)
        if res.status_code != 200:
            logger.error(f"Failed to get organization members for {org}; status_code={res.status_code}, message={res.text}")
            return users
        if len(res.json()) == 0:
            break
        users.extend(x["login"] for x in res.json())
        logger.debug(f"{len(users)} members found.")
        page += 1
    return users


def list_commit_authors(token: str, repo: str, logger: Logger) -> list[str]:
    url = GITHUB_API_BASE + f"/repos/{repo}/commits?per_page=100"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    page = 1
    authors = set([])
    while True:
        res = requests.get(f"{url}&page={page}", headers=headers)
        time.sleep(INTERVAL_IN_SECONDS)
        if res.status_code != 200:
            logger.error(f"Failed to get commits for {repo}; status_code={res.status_code}, message={res.text}")
            return authors
        if len(res.json()) == 0:
            break
        for commit in res.json():
            author = commit.get("author")
            if author:
                authors.add(author["login"])
        logger.debug(f"{len(authors)} authors found.")
        page += 1
    return authors


def list_labels(token: str, repo: str, logger: Logger) -> list[str]:
    url = GITHUB_API_BASE + f"/repos/{repo}/labels?per_page=100"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    page = 1
    labels = []
    while True:
        res = requests.get(f"{url}&page={page}", headers=headers)
        time.sleep(INTERVAL_IN_SECONDS)
        if res.status_code != 200:
            logger.error(f"Failed to get labels for {repo}; status_code={res.status_code}, message={res.text}")
            return labels
        if len(res.json()) == 0:
            break
        for l in res.json():
            name = l["name"]
            labels.append(name)
        page += 1
    return labels


def update_label(token: str, repo: str, name: str, color: str, description: str, logger: Logger) -> bool:
    url = GITHUB_API_BASE + f"/repos/{repo}/labels/{name}"
    headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}
    data = {"color": color, "description": description}
    res = requests.patch(url, headers=headers, json=data)
    time.sleep(INTERVAL_IN_SECONDS)
    if res.status_code != 200:
        logger.error(f"Failed to update label {name}; status_code={res.status_code}, message={res.text}")
        return False
    logger.debug(f"Label {name} updated.")
    return True