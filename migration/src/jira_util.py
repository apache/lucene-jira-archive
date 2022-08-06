import re
import itertools
from dataclasses import dataclass
from collections import defaultdict
from typing import Optional

import jira2markdown
from jira2markdown.elements import MarkupElements
from jira2markdown.markup.lists import UnorderedList, OrderedList
from jira2markdown.markup.text_effects import BlockQuote, Quote, Monospaced
from jira2markdown.markup.text_breaks import Ruler

from markup.lists import UnorderedTweakedList, OrderedTweakedList
from markup.text_effects import EscapeHtmlTag, TweakedBlockQuote, TweakedQuote, TweakedMonospaced
from markup.text_breaks import LongRuler


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


def extract_environment(o: dict) -> str:
    environment = o.get("fields").get("environment", "")
    return environment if environment else ""


def extract_resolution(o: dict) -> Optional[str]:
    resolution = o.get("fields").get("resolution")
    if resolution:
        return resolution.get("name")
    return None


def extract_priority(o: dict) -> Optional[str]:
    priority = o.get("fields").get("priority")
    if priority:
        return priority.get("name")
    return None


def extract_vote_count(o: dict) -> Optional[int]:
    votes = o.get("fields").get("votes")
    if votes:
        vote_count = votes.get("votes")
        if vote_count > 0:
            return vote_count
    return None


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


def extract_parent_key(o: dict) -> Optional[str]:
    parent = o["fields"].get("parent")
    if parent:
        key = parent["key"]
        if key:
            return key
    return None


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


def extract_labels(o: dict) -> list[str]:
    return o.get("fields").get("labels", [])


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


def extract_comments(o: dict) -> list[tuple[str, str, str, str, str, str]]:
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
        comment_id = c.get("id", "")
        res.append((name, disp_name, body, created, updated, comment_id))
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


# space character + Jira emoji + space character
JIRA_EMOJI_TO_UNICODE = {
    "(?<=\s)\(y\)((?=$)|(?=\s))": "\U0001F44D",
    "(?<=\s)\(n\)((?=$)|(?=\s))": "\U0001F44E",
    "(?<=\s)\(i\)((?=$)|(?=\s))": "\U0001F6C8",
    "(?<=\s)\(/\)((?=$)|(?=\s))": "\u2714",
    "(?<=\s)\(x\)((?=$)|(?=\s))": "\u274C",
    "(?<=\s)\(\!\)((?=$)|(?=\s))": "\u26A0",
    "(?<=\s)\(\+\)((?=$)|(?=\s))": "\u002B",
    "(?<=\s)\(\-\)((?=$)|(?=\s))": "\u2212",
    "(?<=\s)\(\?\)((?=$)|(?=\s))": "\u003F",
    "(?<=\s)\(on\)((?=$)|(?=\s))": "\U0001F4A1",
    "(?<=\s)\(off\)((?=$)|(?=\s))": "\U0001F4A1",
    "(?<=\s)\(\*\)((?=$)|(?=\s))": "\u2B50",
    "(?<=\s)\(\*r\)((?=$)|(?=\s))": "\u2B50",
    "(?<=\s)\(\*g\)((?=$)|(?=\s))": "\u2B50",
    "(?<=\s)\(\*b\)((?=$)|(?=\s))": "\u2B50",
    "(?<=\s)\(flag\)((?=$)|(?=\s))": "\U0001F3F4",
    "(?<=\s)\(flagoff\)((?=$)|(?=\s))": "\U0001F3F3"
}

REGEX_CRLF = re.compile(r"\r\n")
REGEX_JIRA_KEY = re.compile(r"[^/]LUCENE-\d+")
REGEX_MENTION_ATMARK = re.compile(r"(^@[\w\.@_-]+?)|((?<=[\s\(\"'，．])@[\w\.@_-]+?)(?=[\s\)\"'\?!,，;:\.．$])")  # this regex may capture only "@" + "<username>" mentions
REGEX_MENION_TILDE = re.compile(r"(^\[~[\w\.@_-]+?\])|((?<=[\s\(\"'，．])\[~[\w\.@_-]+?\])(?=[\s\)\"'\?!,，;:\.．$])")  # this regex may capture only "[~" + "<username>" + "]" mentions
REGEX_LINK = re.compile(r"\[([^\]]+)\]\(([^\)]+)\)")
REGEX_GITHUB_ISSUE_LINK = re.compile(r"(\s)(#\d+)(\s)")


def convert_text(text: str, att_replace_map: dict[str, str] = {}, account_map: dict[str, str] = {}, jira_users: dict[str, str] = {}) -> str:
    """Convert Jira markup to Markdown
    """
    def repl_att(m: re.Match):
        res = m.group(0)
        for src, repl in att_replace_map.items():
            if m.group(2) == src:
                res = f"[{m.group(1)}]({repl})"
        return res

    def escape_gh_issue_link(m: re.Match):
        # escape #NN by backticks to prevent creating an unintentional issue link
        res = f"{m.group(1)}`{m.group(2)}`{m.group(3)}"
        return res

    text = re.sub(REGEX_CRLF, "\n", text)  # jira2markup does not support carriage return (?)

    # convert Jira special emojis into corresponding or similar Unicode characters
    for emoji, unicode in JIRA_EMOJI_TO_UNICODE.items():
        text = re.sub(emoji, unicode, text)

    # convert @ mentions
    mentions = re.findall(REGEX_MENTION_ATMARK, text)
    if mentions:
        mentions = set(filter(lambda x: x != '', itertools.chain.from_iterable(mentions)))
        for m in mentions:
            jira_id = m[1:]
            disp_name = jira_users.get(jira_id)
            gh_m = account_map.get(jira_id)
            # replace Jira name with GitHub account or Jira display name if it is available, othewise show Jira name with `` to avoid unintentional mentions
            mention = lambda: f"@{gh_m}" if gh_m else disp_name if disp_name else f"`@{jira_id}`"
            text = text.replace(m, mention())
    
    # convert ~ mentions
    mentions = re.findall(REGEX_MENION_TILDE, text)
    if mentions:
        mentions = set(filter(lambda x: x != '', itertools.chain.from_iterable(mentions)))
        for m in mentions:
            jira_id = m[2:-1]
            disp_name = jira_users.get(jira_id)
            gh_m = account_map.get(jira_id)
            # replace Jira name with GitHub account or Jira display name if it is available, othewise show Jira name with ``
            mention = lambda: f"@{gh_m}" if gh_m else disp_name if disp_name else f"`@{jira_id}`"
            text = text.replace(m, mention())
    
    # escape tilde to avoid unintentional strike-throughs in Markdown
    text = text.replace("~", "\~")

    # convert Jira markup into Markdown with customization
    elements = MarkupElements()
    elements.replace(UnorderedList, UnorderedTweakedList)
    elements.replace(OrderedList, OrderedTweakedList)
    elements.replace(BlockQuote, TweakedBlockQuote)
    elements.replace(Quote, TweakedQuote)
    elements.replace(Monospaced, TweakedMonospaced)
    elements.insert_after(Ruler, LongRuler)
    elements.append(EscapeHtmlTag)
    text = jira2markdown.convert(text, elements=elements)

    # convert links to attachments
    text = re.sub(REGEX_LINK, repl_att, text)

    # escape github style cross-issue link (#NN)
    text = re.sub(REGEX_GITHUB_ISSUE_LINK, escape_gh_issue_link, text)

    return text


def embed_gh_issue_link(text: str, issue_id_map: dict[str, int], gh_number_self: int) -> str:
    """Embed GitHub issue number
    """
    def repl_simple(m: re.Match):
        res = m.group(0)
        gh_number = issue_id_map.get(m.group(2))
        if gh_number and gh_number != gh_number_self:
            res = f"{m.group(1)}#{gh_number}{m.group(3)}"
        return res
    
    def repl_paren(m: re.Match):
        res = m.group(0)
        gh_number = issue_id_map.get(m.group(2))
        if gh_number and gh_number != gh_number_self:
            res = f"{m.group(1)}#{gh_number}{m.group(3)}"
        return res

    def repl_bracket(m: re.Match):
        res = m.group(0)
        gh_number = issue_id_map.get(m.group(2))
        if gh_number and gh_number != gh_number_self:
            res = f"#{gh_number}"
        return res
    
    def repl_md_link(m: re.Match):
        res = m.group(0)
        gh_number = issue_id_map.get(m.group(1))
        if gh_number and gh_number != gh_number_self:
            res = f"{m.group(0)} (#{gh_number})"
            # print(res)
        return res

    text = re.sub(r"(\s)(LUCENE-\d+)([\s,;:\?\!\.])", repl_simple, text)
    text = re.sub(r"(^)(LUCENE-\d+)([\s,;:\?\!\.])", repl_simple, text)
    text = re.sub(r"(\()(LUCENE-\d+)(\))", repl_paren, text)
    text = re.sub(r"(\[)(LUCENE-\d+)(\])(?!\()", repl_bracket, text)
    text = re.sub(r"\[(LUCENE-\d+)\]\(https?[^\)]+LUCENE-\d+\)", repl_md_link, text)

    return text
