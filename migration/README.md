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

You need a GitHub repository and personal access token for testing. Set `GITHUB_PAT` and `GITHUB_REPO` environment variables. See `.env.example` for other variables.

On Linux/MacOS:
```
cp .env.example .env

vi .env
export GITHUB_PAT=<your token>
export GITHUB_REPO=<your repository location> # e.g. "mocobeta/sandbox-lucene-10557"

source .env
```

## Usage

### 1. Download Jira issues

`src/download_jira.py` downloads Jira issues and dumps them as JSON files in `migration/jira-dump`. This also downloads attached files in each issue.

```
(.venv) migration $ python src/download_jira.py --min 10500 --max 10600
[2022-06-26 01:57:02,408] INFO:download_jira: Downloading Jira issues in /mnt/hdd/repo/sandbox-lucene-10557/migration/jira-dump
[2022-06-26 01:57:17,843] INFO:download_jira: Done.

(.venv) migration $ cat log/jira2github_import_2022-06-26T01\:34\:22.log 
[2022-06-26 01:34:22,300] INFO:jira2github_import: Converting Jira issues to GitHub issues in /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data
[2022-06-26 01:34:23,355] DEBUG:jira2github_import: GitHub issue data created: /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data/GH-LUCENE-10500.json
[2022-06-26 01:34:23,519] DEBUG:jira2github_import: GitHub issue data created: /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data/GH-LUCENE-10501.json
[2022-06-26 01:34:24,894] DEBUG:jira2github_import: GitHub issue data created: /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data/GH-LUCENE-10502.json
...
```

### 2. Convert Jira issues to GitHub issues

`src/jira2github_import.py` converts Jira dumps into GitHub data that are importable to [issue import API](https://gist.github.com/jonmagic/5282384165e0f86ef105). Converted JSON data is saved in `migration/github-import-data`.

Also this resolves all Jira user ID - GitHub account alignment if the account mapping is given in `mapping-data/account-map.csv`. 

```
(.venv) migration $ python src/jira2github_import.py --min 10500 --max 10600
[2022-06-26 01:34:22,300] INFO:jira2github_import: Converting Jira issues to GitHub issues in /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data
[2022-06-26 01:36:27,739] INFO:jira2github_import: Done.

(.venv) migration $ cat log/jira2github_import_2022-06-26T01\:34\:22.log
[2022-06-26 01:34:22,300] INFO:jira2github_import: Converting Jira issues to GitHub issues in /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data
[2022-06-26 01:34:23,355] DEBUG:jira2github_import: GitHub issue data created: /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data/GH-LUCENE-10500.json
[2022-06-26 01:34:23,519] DEBUG:jira2github_import: GitHub issue data created: /mnt/hdd/repo/sandbox-lucene-10557/migration/github-import-data/GH-LUCENE-10501.json
...
```

### 3. Import GitHub issues

First pass: `src/import_github_issues.py` imports GitHub issues and comments via issue import API. This also writes Jira issue key - GitHub issue number mappings to a file in migration/mappings-data.

We confirmed this script does not trigger any notifications.

```
(.venv) migration $ python src/import_github_issues.py --min 10500 --max 10600
[2022-06-26 01:36:46,749] INFO:import_github_issues: Importing GitHub issues
[2022-06-26 01:47:35,979] INFO:import_github_issues: Done.

(.venv) migration $ cat log/import_github_issues_2022-06-26T01\:36\:46.log
[2022-06-26 01:36:46,749] INFO:import_github_issues: Importing GitHub issues
[2022-06-26 01:36:52,299] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/migration-test-2/issues/1 was successfully completed.
[2022-06-26 01:36:57,883] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/migration-test-2/issues/2 was successfully completed.
[2022-06-26 01:37:03,405] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/migration-test-2/issues/3 was successfully completed.
...

(.venv) migration $ cat mappings-data/issue-map.csv
JiraKey,GitHubUrl,GitHubNumber
LUCENE-10500,https://github.com/mocobeta/migration-test-2/issues/1,1
LUCENE-10501,https://github.com/mocobeta/migration-test-2/issues/2,2
LUCENE-10502,https://github.com/mocobeta/migration-test-2/issues/3,3
...
```

### 4. Update GitHub issues and comments

Second pass: `src/update_issue_links.py` 1) iterates all imported GitHub issue descriptions and comments; 2) embed correct GitHub issue number next to the corresponding Jira issue key with previously created issue number mapping; 3) updates them if the texts are changed.

e.g.: if `LUCENE-10500` is mapped to GitHub issue `#100`, then all text fragments `LUCENE-10500`  in issue descriptions and comments will be updated to `LUCENE-10500 (#100)`.

We confirmed this script does not trigger any notifications.

```
(.venv) migration $ python src/update_issue_links.py
[2022-06-26 01:59:43,324] INFO:update_issue_links: Updating GitHub issues
[2022-06-26 02:17:38,332] INFO:update_issue_links: Done.

(.venv) migration $ cat log/update_issue_links_2022-06-26T01\:59\:43.log
[2022-06-26 01:59:43,324] INFO:update_issue_links: Updating GitHub issues
[2022-06-26 01:59:45,586] DEBUG:update_issue_links: Issue 1 does not contain any cross-issue links; nothing to do.
[2022-06-26 01:59:50,062] DEBUG:update_issue_links: # comments in issue 1 = 3
[2022-06-26 01:59:52,601] DEBUG:update_issue_links: Comment 1166321470 was successfully updated.
[2022-06-26 01:59:55,164] DEBUG:update_issue_links: Comment 1166321472 was successfully updated.
[2022-06-26 01:59:55,165] DEBUG:update_issue_links: Comment 1166321473 does not contain any cross-issue links; nothing to do.
[2022-06-26 01:59:57,426] DEBUG:update_issue_links: Issue 2 does not contain any cross-issue links; nothing to do.
...
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
