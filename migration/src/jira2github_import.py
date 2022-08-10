#
# Convert Jira issues to GitHub issues for Import Issues API (https://gist.github.com/jonmagic/5282384165e0f86ef105)
# Usage:
#   python src/jira2github_import.py --issues <jira issue number list> [--num-workers <# worker processes>]
#   python src/jira2github_import.py --min <min issue number> --max <max issue number> [--num-workers <# worker processes>]
#

import argparse
from logging import Logger
from pathlib import Path
import json
import sys
from urllib.parse import quote
import dateutil.parser
import os
import traceback
import multiprocessing

from common import *
from jira_util import *


def attachment_url(issue_num: int, filename: str, att_base_url: str) -> str:
    return f"{att_base_url}/{jira_issue_id(issue_num)}/{quote(filename)}"


def jira_timestamp_to_github_timestamp(ts: str) -> str:
    # convert Jira timestamp format to GitHub acceptable format
    # e.g., "2006-06-06T06:24:38.000+0000" -> "2006-06-06T06:24:38Z"
    return ts[:-9] + "Z"


def convert_issue(num: int, dump_dir: Path, output_dir: Path, account_map: dict[str, str], jira_users: dict[str, str], att_base_url: str, att_dir: Optional[Path], logger: Logger) -> bool:
    jira_id = jira_issue_id(num)
    dump_file = jira_dump_file(dump_dir, num)
    if not dump_file.exists():
        logger.warning(f"Jira dump file not found: {dump_file}")
        return False

    with open(dump_file) as fp:
        o = json.load(fp)
        summary = extract_summary(o).strip()
        description = extract_description(o).strip()
        status = extract_status(o)
        issue_type = extract_issue_type(o)
        environment = extract_environment(o)
        (reporter_name, reporter_dispname) = extract_reporter(o)
        (assignee_name, assignee_dispname) = extract_assignee(o)
        created = extract_created(o)
        updated = extract_updated(o)
        resolutiondate = extract_resolutiondate(o)
        fix_versions = extract_fixversions(o)
        versions = extract_versions(o)
        components = extract_components(o)
        attachments = extract_attachments(o)
        linked_issues = extract_issue_links(o)
        subtasks = extract_subtasks(o)
        pull_requests = extract_pull_requests(o)
        jira_labels = extract_labels(o)
        resolution = extract_resolution(o)
        priority = extract_priority(o)
        vote_count = extract_vote_count(o)
        parent_issue_key = extract_parent_key(o)
        comments = extract_comments(o)

        reporter_gh = account_map.get(reporter_name)
        reporter = f"{reporter_dispname} (@{reporter_gh})" if reporter_gh else f"{reporter_dispname}"
        assignee_gh = account_map.get(assignee_name)
        assignee = f"{assignee_dispname} (@{assignee_gh})" if assignee_gh else f"{assignee_dispname}"

        # make attachment list
        attachment_list_items = []
        att_replace_map = {}
        for (filename, cnt) in attachments:
            attachment_list_items.append(f"[{filename}]({attachment_url(num, filename, att_base_url)})" + (f" (versions: {cnt})" if cnt > 1 else ""))
            att_replace_map[filename] = attachment_url(num, filename, att_base_url)

        # detect unmentioned image files
        # https://github.com/apache/lucene-jira-archive/issues/126
        image_files = [x[0] for x in attachments if re.match(r"^.+\.(png|jpg|jpeg|gif|svg|bmp|ico|tif|tiff)$", x[0], flags=re.IGNORECASE)]
        embedded_image_files = extract_embedded_image_files(description, image_files)
        for (_, _, comment_body, _, _, _) in comments:
            embedded_image_files.update(extract_embedded_image_files(comment_body, image_files))
        unmentioned_images = [x for x in image_files if x not in embedded_image_files]

        # embed github issue number next to linked issue keys
        linked_issues_list_items = []
        for jira_key in linked_issues:
            linked_issues_list_items.append(f"- [{jira_key}]({jira_issue_url(jira_key)})\n")
        
        # embed github issue number next to sub task keys
        subtasks_list_items = []
        for jira_key in subtasks:
            subtasks_list_items.append(f"- [{jira_key}]({jira_issue_url(jira_key)})\n")

        created_datetime = dateutil.parser.parse(created)
        updated_datetime = dateutil.parser.parse(updated)
        if resolutiondate is not None:
            resolutiondate_datetime = dateutil.parser.parse(resolutiondate)
        else:
            resolutiondate_datetime = None

        try:
            body = f'{convert_text(description, att_replace_map, account_map, jira_users, att_dir)}\n\n'
            if len(body) > 65000:
                logger.warning(f"Issue description on {jira_id} is too long: {body[:100]}")
                body = "FIXME"
            for image_file in unmentioned_images:
                # show orphaned (unmentioned) image files in the issue description
                att_url = att_replace_map.get(image_file)
                body += f'![{image_file}]({att_url})\n\n'
        except Exception as e:
            logger.error(traceback.format_exc(limit=100))
            logger.error(f"Failed to convert opening issue description on {jira_issue_id(num)} due to above exception, ({str(e)}); falling back to original Jira description as code block.")
            logger.error(f"Original description: {description}")
            body = f"```\n{description}```\n\n"

        body += f"""

---
Migrated from [{jira_id}]({jira_issue_url(jira_id)}) by {reporter}"""

        if vote_count:
            body += f", {vote_count} vote"
            if vote_count > 1:
                body += 's'
            
        if resolutiondate_datetime is not None:
            body += f", resolved {resolutiondate_datetime.strftime('%b %d %Y')}"
        elif created_datetime.date() != updated_datetime.date():
            body += f", updated {updated_datetime.strftime('%b %d %Y')}"

        if parent_issue_key:
            body += f'\nParent: [{parent_issue_key}](https://issues.apache.org/jira/browse/{parent_issue_key})'

        if environment:
            body += f'\nEnvironment:\n```\n{environment}\n```\n'

        if len(attachment_list_items) > 0:
            body += f'\nAttachments: {", ".join(attachment_list_items)}'

        if len(linked_issues_list_items) > 0:
            body += f'\nLinked issues:\n {"".join(linked_issues_list_items)}'

        if len(subtasks_list_items) > 0:
            body += f'\nSub-tasks:\n {"".join(subtasks_list_items)}'

        if len(pull_requests) > 0:
            body += f'\nPull requests: {", ".join([str(x) for x in pull_requests])}'

        body += '\n'

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

        comments_data = []
        for (comment_author_name, comment_author_dispname, comment_body, comment_created, comment_updated, comment_id) in comments:
            # TODO: since we now have accurate created_at reflected in the github comment, mabye we remove these
            #       timestamps?  also, if the account id mapped over to known GH account, we can drop Jira footer entirely?
            comment_created_datetime = dateutil.parser.parse(comment_created)
            comment_time = f'{comment_created_datetime.strftime("%b %d %Y")}'
            comment_updated_datetime = dateutil.parser.parse(comment_updated)
            if comment_updated_datetime.date() != comment_created_datetime.date():
                comment_time += f' [updated: {comment_updated_datetime.strftime("%b %d %Y")}]'
            try:
                comment_body = f'{convert_text(comment_body, att_replace_map, account_map, jira_users, att_dir)}\n\n'
                if len(comment_body) > 65000:
                    logger.warning(f"Comment on {jira_id} is too long: {comment_body[:100]}")
                    comment_body = "FIXME"
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
                
            comment_data = f'{comment_author(comment_author_name, comment_author_dispname)} ([migrated from JIRA]({jira_comment_link}))\n\n{comment_body}'
            data = {
                "body": comment_data
            }
            if comment_created:
                data["created_at"] = jira_timestamp_to_github_timestamp(comment_created)
            comments_data.append(data)

        labels = []
        if issue_type and ISSUE_TYPE_TO_LABEL_MAP.get(issue_type):
            labels.append(ISSUE_TYPE_TO_LABEL_MAP.get(issue_type))
        # milestone?
        for v in fix_versions:
            if v:
                labels.append(f"fix-version:{v}")
        for v in versions:
            if v:
                labels.append(f"affects-version:{v}")
        for c in components:
            if c.startswith("core"):
                labels.append(f"module:{c}")
            elif c in COMPONENT_TO_LABEL_MAP:
                l = COMPONENT_TO_LABEL_MAP[c]
                if l is not None:
                    labels.append(l)
            else:
                logger.error(f"Unknown Component: {c}")
        for label in jira_labels:
            # GitHub does not allow commas in labels
            label = label.replace(",","")
            # several label texts have to be skipped; otherwise import fails. don't know why.
            if label in ["queryparser", "fastvectorhighlighter", "highlighter", "Documentation", "Sort", "Highlighting", "Stemmer", "Scorer", "spatialrecursiveprefixtreefieldtype"] or \
                label.startswith("java"):
                logger.warning(f"Jira label '{label}' was skipped for {jira_id}. Please manually attach it from GitHub Web GUI.")
                continue
            labels.append(f"legacy-jira-label:{label}")
        if resolution:
            labels.append(f"legacy-jira-resolution:{resolution}")
        if priority:
            labels.append(f"legacy-jira-priority:{priority}")
        
        # ensure there are not duplicate labels
        labels = list(set(labels))

        data = {
            "issue": {
                "title": make_github_title(summary, jira_id),
                "body": body,
                "closed": status in ["Closed", "Resolved"],
                "labels": labels,
            },
            "comments": comments_data
        }
        if created:
            data["issue"]["created_at"] = jira_timestamp_to_github_timestamp(created)
        if updated:
            data["issue"]["updated_at"] = jira_timestamp_to_github_timestamp(updated)
        if resolutiondate:
            data["issue"]["closed_at"] = jira_timestamp_to_github_timestamp(resolutiondate)
        if assignee_gh:
            data["issue"]["assignee"] = assignee_gh

        data_file = github_data_file(output_dir, num)
        with open(data_file, "w") as fp:
            json.dump(data, fp, indent=2)

    logger.debug(f"GitHub issue data created: {data_file}")
    return True


if __name__ == "__main__":
    att_base_url = os.getenv("ATTACHMENTS_BASE_URL")
    if not att_base_url:
        print("Please set attachments base url to ATTACHMENTS_BASE_URL environment variable.")
        sys.exit()

    parser = argparse.ArgumentParser()
    parser.add_argument('--issues', type=int, required=False, nargs='*', help='Jira issue number list to be downloaded')
    parser.add_argument('--min', type=int, dest='min', required=False, default=1, help='Minimum Jira issue number to be converted')
    parser.add_argument('--max', type=int, dest='max', required=False, help='Maximum Jira issue number to be converted')
    parser.add_argument('--num-workers', type=int, dest='num_workers', required=False, default=1, help='Number of worker processes')
    args = parser.parse_args()

    dump_dir = Path(__file__).resolve().parent.parent.joinpath(JIRA_DUMP_DIRNAME)
    if not dump_dir.exists():
        print(f"Jira dump dir not exists: {dump_dir}")
        sys.exit(1)

    mappings_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
    account_mapping_file = mappings_dir.joinpath(ACCOUNT_MAPPING_FILENAME)
    jira_users_file = mappings_dir.joinpath(JIRA_USERS_FILENAME)

    output_dir = Path(__file__).resolve().parent.parent.joinpath(GITHUB_IMPORT_DATA_DIRNAME)
    if not output_dir.exists():
        output_dir.mkdir()
    assert output_dir.exists()

    account_map = read_account_map(account_mapping_file) if account_mapping_file.exists() else {}
    jira_users = read_jira_users_map(jira_users_file) if jira_users_file.exists() else {}

    issues = []
    if args.issues:
        issues = args.issues
    else:
        if args.max:
            issues.extend(list(range(args.min, args.max + 1)))
        else:
            issues.append(args.min)
    num_workers = args.num_workers

    log_dir = Path(__file__).resolve().parent.parent.joinpath(LOG_DIRNAME)
    name = "jira2github_import"
    (listener, queue) = log_listener(log_dir, name)
    listener.start()
    logging_setup_worker(queue)
    logger = logging.getLogger(name)

    logger.info(f"Converting Jira issues to GitHub issues in {output_dir}. num_workers={num_workers}")

    def task(num):
        logger = logging.getLogger(name)
        try:
            # if you have attachment files in $JIRA_ATTACHMENTS_DIRPATH, text attachments'data may be embedded in issue comments.
            att_dir = Path(JIRA_ATTACHMENTS_DIRPATH).joinpath(jira_issue_id(num)) if JIRA_ATTACHMENTS_DIRPATH else None
            convert_issue(num, dump_dir, output_dir, account_map, jira_users, att_base_url, att_dir, logger)
        except Exception as e:
            logger.error(traceback.format_exc(limit=100))
            logger.error(f"Failed to convert Jira issue. An error '{str(e)}' occurred; skipped {jira_issue_id(num)}.")

    results = []
    # Try to support Windows: The worker configuration is done at the start of the worker process run.
    with multiprocessing.Pool(num_workers, initializer=logging_setup_worker, initargs=(queue,)) as pool:
        for num in issues:
            result = pool.apply_async(task, (num,))
            results.append(result)
        for res in results:
            res.get()

    logger.info("Done.")
    queue.put_nowait(None)
    listener.join()
