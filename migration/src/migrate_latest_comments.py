import json
import traceback
import sys
import os
import argparse
from urllib.parse import quote
import dateutil.parser
from common import *
from github_issues_util import *
from jira_util import *

log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
logger = logging_setup(log_dir, "migrate_latest_comments")


def attachment_url(issue_num: int, filename: str, att_repo: str, att_branch: str) -> str:
    return f"https://raw.githubusercontent.com/{att_repo}/{att_branch}/attachments/{jira_issue_id(issue_num)}/{quote(filename)}"


def jira_timestamp_to_github_timestamp(ts: str) -> str:
    # convert Jira timestamp format to GitHub acceptable format
    # e.g., "2006-06-06T06:24:38.000+0000" -> "2006-06-06T06:24:38Z"
    return ts[:-9] + "Z"


def convert_latest_comments(
    num: int, 
    dump_dir: Path, 
    after: str,
    account_map: dict[str, str], 
    jira_users: dict[str, str], 
    att_repo: str,
    att_branch: str,
    logger: Logger
) -> list[str]:
    jira_id = jira_issue_id(num)
    dump_file = jira_dump_file(dump_dir, num)
    if not dump_file.exists():
        logger.warning(f"Jira dump file not found: {dump_file}")
        return []

    gh_comments = []
    with open(dump_file) as fp:
        o = json.load(fp)

        # make attachment list
        attachments = extract_attachments(o)
        attachment_list_items = []
        att_replace_map = {}
        for (filename, cnt) in attachments:
            attachment_list_items.append(f"[{filename}]({attachment_url(num, filename, att_repo, att_branch)})" + (f" (versions: {cnt})" if cnt > 1 else ""))
            att_replace_map[filename] = attachment_url(num, filename, att_repo, att_branch)

        def comment_author(author_name, author_dispname):
            author_gh = account_map.get(author_name)
            return f"{author_dispname} (@{author_gh})" if author_gh else author_dispname
        
        def enable_hyperlink_to_commit(comment_body: str):
            lines = []
            for line in comment_body.split("\n"):
                # remove '[' and ']' iff it contains a URL (i.e. link to a commit in ASF GitBox repo).
                m = re.match(r"^\[\s?(https?://\S+)\s?\]$", line.strip())
                if m:
                    lines.append(m.group(1))
                else:
                    lines.append(line)
            return "\n".join(lines)

        comments = extract_comments(o)
        for (comment_author_name, comment_author_dispname, comment_body, comment_created, comment_updated, comment_id) in comments:
            if comment_created < after:
                continue
            comment_created_datetime = dateutil.parser.parse(comment_created)
            comment_time = f'{comment_created_datetime.strftime("%b %d %Y")}'
            comment_updated_datetime = dateutil.parser.parse(comment_updated)
            if comment_updated_datetime.date() != comment_created_datetime.date():
                comment_time += f' [updated: {comment_updated_datetime.strftime("%b %d %Y")}]'
            try:
                comment_body = f'{convert_text(comment_body, att_replace_map, account_map, jira_users)}\n\n'
                # apply a special conversion for jira-bot's comments.
                # see https://github.com/apache/lucene-jira-archive/issues/54
                if comment_author_name == "jira-bot":
                    comment_body = enable_hyperlink_to_commit(comment_body)
            except Exception as e:
                logger.error(traceback.format_exc(limit=100))
                logger.error(f"Failed to convert comment on {jira_issue_id(num)} due to above exception ({str(e)}); falling back to original Jira comment as code block.")
                logger.error(f"Original text: {comment_body}")
                comment_body = f"```\n{comment_body}```\n\n"

            jira_comment_link = f'https://issues.apache.org/jira/browse/{jira_id}?focusedCommentId={comment_id}&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-{comment_id}'
                
            comment_body += f'[Legacy Jira: {comment_author(comment_author_name, comment_author_dispname)} on [{comment_time}]({jira_comment_link})]\n'

            gh_comments.append(comment_body)
    
    return gh_comments


@retry_upto(3, 1.0, logger)
def add_latest_comments_to_github(comment_body: str, issue_number: int, token: str, repo: str):
    issue_id = create_comment(token, repo, issue_number, comment_body, logger)
    if issue_id:
        logger.debug(f"Comment {issue_id} was created on issue {issue_number}.")


if __name__ == "__main__":
    github_att_repo = os.getenv("GITHUB_ATT_REPO")
    if not github_att_repo:
        print("Please set your GitHub attachment repo to GITHUB_ATT_REPO environment variable.")
        sys.exit(1)
    github_att_branch = os.getenv("GITHUB_ATT_BRANCH")
    if not github_att_repo:
        print("Please set your GitHub attachment branch to GITHUB_ATT_BRANCH environment variable.")
        sys.exit(1)
    github_token = os.getenv("GITHUB_PAT")
    if not github_token:
        print("Please set your GitHub token to GITHUB_PAT environment variable.")
        sys.exit(1)
    github_repo = os.getenv("GITHUB_REPO")
    if not github_repo:
        print("Please set GitHub repo location to GITHUB_REPO environment varialbe.")
        sys.exit(1)

    check_authentication(github_token)

    parser = argparse.ArgumentParser()
    parser.add_argument('--issues', type=int, required=True, nargs='*', help='Jira issue number list')
    parser.add_argument('--after', type=str, dest='after', required=True)
    args = parser.parse_args()

    dump_dir = Path(__file__).resolve().parent.parent.joinpath(JIRA_DUMP_DIRNAME)
    if not dump_dir.exists():
        print(f"Jira dump dir not exists: {dump_dir}")
        sys.exit(1)

    mappings_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
    account_mapping_file = mappings_dir.joinpath(ACCOUNT_MAPPING_FILENAME)
    jira_users_file = mappings_dir.joinpath(JIRA_USERS_FILENAME)
    issue_mapping_file = mappings_dir.joinpath(ISSUE_MAPPING_FILENAME)

    account_map = read_account_map(account_mapping_file) if account_mapping_file.exists() else {}
    jira_users = read_jira_users_map(jira_users_file) if jira_users_file.exists() else {}
    issue_id_map = read_issue_id_map(issue_mapping_file)

    issues = args.issues
    after = args.after

    logger.info(f"Adding latest comments.")

    for num in issues:
        gh_issue_number = issue_id_map.get(jira_issue_id(num))
        if not gh_issue_number:
            logger.error(f"Corresponding GitHub issue not foud for {jira_issue_id(num)}")
            continue
        gh_comments = convert_latest_comments(num, dump_dir, after, account_map, jira_users, github_att_repo, github_att_branch, logger)
        for comment_body in gh_comments:
            try:
                add_latest_comments_to_github(comment_body, gh_issue_number, github_token, github_repo)
            except MaxRetryLimitExceedException:
                logger.error(f"Failed to update issue comment {comment_body}")
                continue

    logger.info("Done.")