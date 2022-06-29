import re
from dataclasses import dataclass
from collections import defaultdict
from typing import Optional

import jira2markdown


@dataclass
class Attachment(object):
    filename: str
    created: str
    content: str
    mime_type: str


def extract_summary(o: dict) -> str:
    return o.get("fields").get("summary", "")


def extract_description(o: dict) -> str:
    description = o.get("fields").get("description", "")
    return description if description else ""


def extract_status(o: dict) -> str:
    status = o.get("fields").get("status")
    return status.get("name", "") if status else ""


def extract_issue_type(o: dict) -> str:
    issuetype = o.get("fields").get("issuetype")
    return issuetype.get("name", "") if issuetype else ""


def extract_reporter(o: dict) -> tuple[str, str]:
    reporter = o.get("fields").get("reporter")
    name = reporter.get("name", "") if reporter else ""
    disp_name = reporter.get("displayName", "") if reporter else ""
    return (name, disp_name)


def extract_assignee(o: dict) -> tuple[str, str]:
    assignee = o.get("fields").get("assignee")
    name = assignee.get("name", "") if assignee else ""
    disp_name = assignee.get("displayName", "") if assignee else ""
    return (name, disp_name)


def extract_created(o: dict) -> str:
    return o.get("fields").get("created", "")


def extract_updated(o: dict) -> str:
    return o.get("fields").get("updated", "")


def extract_resolutiondate(o: dict) -> str:
    return o.get("fields").get("resolutiondate", "")


def extract_fixversions(o: dict) -> list[str]:
    return [x.get("name", "") for x in o.get("fields").get("fixVersions", [])]


def extract_versions(o: dict) -> list[str]:
    return [x.get("name", "") for x in o.get("fields").get("versions", [])]


def extract_components(o: dict) -> list[str]:
    return [x.get("name", "") for x in o.get("fields").get("components", [])]


def extract_attachments(o: dict) -> list[tuple[str, int]]:
    attachments = o.get("fields").get("attachment")
    if not attachments:
        return []
    files = {}
    counts = defaultdict(int)
    for a in attachments:
        filename = a.get("filename")
        created = a.get("created")
        content = a.get("content")
        mime_type = a.get("mimeType")
        if not (filename and created and content and mime_type):
            continue
        if filename not in files or created > files[filename].created:
            files[filename] = Attachment(filename=filename, created=created, content=content, mime_type=mime_type)
        counts[filename] += 1
    result = []
    for name in files.keys():
        result.append((name, counts[name]))
    return result


def extract_issue_links(o: dict) -> list[str]:
    issue_links = o.get("fields").get("issuelinks", [])
    if not issue_links:
        return []

    res = []
    for link in issue_links:
        key = link.get("outwardIssue", {}).get("key")
        if key:
            res.append(key)
        key = link.get("inwardIssue", {}).get("key")
        if key:
            res.append(key)
    return res


def extract_subtasks(o: dict) -> list[str]:
    return [x.get("key", "") for x in o.get("fields").get("subtasks", [])]


def extract_comments(o: dict) -> list[str, str, str, str, str]:
    comments = o.get("fields").get("comment", {}).get("comments", [])
    if not comments:
        return []
    res = []
    for c in comments:
        author = c.get("author")
        name = author.get("name", "") if author else ""
        disp_name = author.get("displayName", "") if author else ""
        body = c.get("body", "")
        created = c.get("created", "")
        updated = c.get("updated", "")
        res.append((name, disp_name, body, created, updated))
    return res


def extract_pull_requests(o: dict) -> list[str]:
    worklogs = o.get("fields").get("worklog", {}).get("worklogs", [])
    if not worklogs:
        return []
    res = []
    for wl in worklogs:
        if wl.get("author").get("name", "") != "githubbot":
            continue
        comment: str = wl.get("comment", "")
        if not comment:
            continue
        if "opened a new pull request" not in comment and not "opened a pull request" in comment:
            continue
        comment = comment.replace('\n', ' ')
        # detect pull request url
        matches = re.match(r".*(https://github\.com/apache/lucene/pull/\d+)", comment)
        if matches:
            res.append(matches.group(1))
        # detect pull request url in old lucene-solr repo
        matches = re.match(r".*(https://github\.com/apache/lucene-solr/pull/\d+)", comment)
        if matches:
            res.append(matches.group(1))     
    return res


REGEX_JIRA_KEY = re.compile(r"[^/]LUCENE-\d+")
REGEX_MENTION = re.compile(r"@\w+")
REGEX_LINK = re.compile(r"\[([^\]]+)\]\(([^\)]+)\)")


def convert_text(text: str, att_replace_map: dict[str, str] = {}) -> str:
    """Convert Jira markup to Markdown
    """
    def repl_att(m: re.Match):
        res = m.group(0)
        for src, repl in att_replace_map.items():
            if m.group(2) == src:
                res = f"[{m.group(1)}]({repl})"
        return res

    text = jira2markdown.convert(text)

    # markup @ mentions with ``
    mentions = re.findall(REGEX_MENTION, text)
    if mentions:
        mentions = set(mentions)
        for m in mentions:
            with_backtick = f"`{m}`"
            text = text.replace(m, with_backtick)
    
    text = re.sub(REGEX_LINK, repl_att, text)

    return text


def embed_gh_issue_link(text: str, issue_id_map: dict[str, str]) -> str:
    """Embed GitHub issue number
    """
    jira_keys = [m[1:] for m in re.findall(REGEX_JIRA_KEY, text)]
    if jira_keys:
        jira_keys = set(jira_keys)
        for key in jira_keys:
            gh_number = issue_id_map.get(key)
            if gh_number:
                new_key = f"{key} (#{gh_number})"
                text = text.replace(key, new_key)
    return text
