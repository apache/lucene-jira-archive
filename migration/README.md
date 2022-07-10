# [WIP] Migration tools (Jira issue -> GitHub issue)

## Setup

You need Python 3.9+. The scripts were tested on Linux; maybe works also on Mac and Windows (not tested).

On Linux/MacOS:
```
python -V
Python 3.9.13

# install dependencies
python -m venv .venv
source .venv/bin/activate
(.venv) pip install -r requirements.txt
```

You need a GitHub repository and [personal access token (PAT)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) for testing. Set `GITHUB_PAT` and `GITHUB_REPO` environment variables. See `.env.example` for other variables.

On Linux/MacOS:
```
cp .env.example .env

vi .env
export GITHUB_PAT=<your token>
export GITHUB_REPO=<your repository location> # e.g. "mocobeta/sandbox-lucene-10557"

source .env
```

ou must first manually create the repository yourself using GitHub.  Consider naming your repository with `stargazers-` prefix as this [might prevent Web crawlers from indexing your migrated issues](https://github.com/apache/lucene-jira-archive/issues/1#issuecomment-1173701233), thus confusing the daylights out of future Googlers.

## Usage

All logs are saved in `migration/log`.

### 1. Download Jira issues

`src/download_jira.py` downloads Jira issues and dumps them as JSON files in `migration/jira-dump`. This also downloads attached files in each issue.

```
(.venv) migration $ python src/download_jira.py --min 10500 --max 10510
[2022-07-06 16:22:01,994] INFO:download_jira: Downloading Jira issues in /mnt/hdd/repo/lucene-jira-archive/migration/jira-dump. Attachments are saved in /tmp/attachments.
[2022-07-06 16:22:17,172] INFO:download_jira: Done.

(.venv) migration $ ls -1 jira-dump/
LUCENE-10500.json
LUCENE-10501.json
LUCENE-10502.json

(.venv) migration $ ls -1 /tmp/attachments/
LUCENE-10500
LUCENE-10501
LUCENE-10502
...
...
```

Downloaded attachments should be separately committed to a dedicated branch named `attachments` (or matching the `GITHUB_ATT_BRANCH` env variable) for them.


### 2. Convert Jira issues to GitHub issues

`src/jira2github_import.py` converts Jira dumps into GitHub data that are importable to [issue import API](https://gist.github.com/jonmagic/5282384165e0f86ef105). Converted JSON data is saved in `migration/github-import-data`.

This also resolves all Jira user ID - GitHub account alignment if the account mapping is given in `mapping-data/account-map.csv`.  If you have no mapping, `cp mapping-data/account-map.csv{.example,}`.

```
(.venv) migration $ python src/jira2github_import.py --min 10500 --max 10510
[2022-07-06 15:46:38,837] INFO:jira2github_import: Converting Jira issues to GitHub issues in /mnt/hdd/repo/lucene-jira-archive/migration/github-import-data
[2022-07-06 15:46:48,761] INFO:jira2github_import: Done.

(.venv) migration $ ls github-import-data/
GH-LUCENE-10500.json
GH-LUCENE-10501.json
GH-LUCENE-10502.json
...
```

### 3. Import GitHub issues

First pass: `src/import_github_issues.py` imports GitHub issues and comments via issue import API. This also writes Jira issue key - GitHub issue number mappings to local file `migration/mappings-data/issue-map.csv`.

We confirmed this script does not trigger any GitHub notifications.

```
(.venv) migration $ python src/import_github_issues.py --min 10500 --max 10510
[2022-07-06 15:47:48,230] INFO:import_github_issues: Importing GitHub issues
[2022-07-06 15:52:06,314] INFO:import_github_issues: Done.
...

(.venv) migration $ cat mappings-data/issue-map.csv
JiraKey,GitHubUrl,GitHubNumber
LUCENE-10500,https://github.com/mocobeta/migration-test-3/issues/42,42
LUCENE-10501,https://github.com/mocobeta/migration-test-3/issues/43,43
LUCENE-10502,https://github.com/mocobeta/migration-test-3/issues/44,44
...
```

### 4. Re-map cross-issue links on GitHub

`src/remap_cross_issue_links.py` exports issues and comments from GitHub and save updated issue/comment bodies to `migration/github-remapped-data`.

```
(.venv) migration $ python src/remap_cross_issue_links.py --issues 40 41
[2022-07-06 15:32:39,895] INFO:remap_cross_issue_links: Remapping cross-issue links
[2022-07-06 15:32:47,729] INFO:remap_cross_issue_links: Done.

(.venv) migration $ ls github-remapped-data/
COMMENT-1175792003.json  COMMENT-1175792076.json  COMMENT-1175797378.json  COMMENT-1175797444.json  COMMENT-1175797570.json  ISSUE-40.json  ISSUE-41.json
```

### 5. Update GitHub issues and comments

Second pass: `src/update_issues.py` updates issues and comments with updated issue/comment bodies.

```
(.venv) migration $ python src/update_issues.py --issues 40 41 --comments 1175797570 1175797444
[2022-07-06 15:34:59,537] INFO:update_issues: Updating issues/comments
[2022-07-06 15:35:06,532] INFO:update_issues: Done.
```

## Already implemented things

You can:

* migrate all texts in issue descriptions and comments to GitHub; browsing/searching old issues should work fine.
* extract every issue metadata from Jira and port it to labels or issue descriptions (as plain text).
* create links to attachments.
* map Jira cross-issue link "LUCENE-xxx" to GitHub issue mention "#yyy".
* map Jira user ids to GitHub accounts if the mapping is given.
* set assignee field if the account mapping is given.
* convert Jira markups to Markdown with parser library.
   * not perfect - there can be many conversion errors



## Limitations

You cannot:

* simulate original issue reporters or comment authors; they have to be preserved in free-text forms.
