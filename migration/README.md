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

You must first manually create the repository yourself using GitHub.  Consider naming your repository with `stargazers-` prefix as this [might prevent Web crawlers from indexing your migrated issues](https://github.com/apache/lucene-jira-archive/issues/1#issuecomment-1173701233), thus confusing the daylights out of future Googlers.

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

### 2. (Optional) Generate Jira username - GitHub account mapping

See "How to Generate Account Mapping" seciton.

### 3. Convert Jira issues to GitHub issues

`src/jira2github_import.py` converts Jira dumps into GitHub data that are importable to [issue import API](https://gist.github.com/jonmagic/5282384165e0f86ef105). Converted JSON data is saved in `migration/github-import-data`.

This also resolves all Jira user ID - GitHub account alignment if the account mapping is given in `mapping-data/account-map.csv`.

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

### 4. Import GitHub issues

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

### 5. Re-map cross-issue links on GitHub

`src/remap_cross_issue_links.py` exports issues and comments from GitHub and save updated issue/comment bodies to `migration/github-remapped-data`.

```
(.venv) migration $ python src/remap_cross_issue_links.py --issues 40 41
[2022-07-06 15:32:39,895] INFO:remap_cross_issue_links: Remapping cross-issue links
[2022-07-06 15:32:47,729] INFO:remap_cross_issue_links: Done.

(.venv) migration $ ls github-remapped-data/
COMMENT-1175792003.json  COMMENT-1175792076.json  COMMENT-1175797378.json  COMMENT-1175797444.json  COMMENT-1175797570.json  ISSUE-40.json  ISSUE-41.json
```

### 6. Update GitHub issues and comments

Second pass: `src/update_issues.py` updates issues and comments with updated issue/comment bodies.

```
(.venv) migration $ python src/update_issues.py --issues 40 41 --comments 1175797570 1175797444
[2022-07-06 15:34:59,537] INFO:update_issues: Updating issues/comments
[2022-07-06 15:35:06,532] INFO:update_issues: Done.
```

### How to Generate Account Mapping

This optional step creates Jira username - GitHub account mapping. To associate Jira user with GitHub account, Jira user's "Full Name" and GitHub account's "Name" needs to be set to exactly the same value. See https://github.com/apache/lucene-jira-archive/issues/3.

Note that this tool would not generate a correct mapping - you should manually check/edit the output file to create the final mapping (see step 4.).

1. List all Jira users

You need to download all Jira issues (see "1. Download Jira issues") in advance.

```
(.venv) migration $ python src/list_jira_users.py
[2022-07-11 23:53:52,020] INFO:list_jira_users: Listing Jira users
[2022-07-11 23:54:34,179] INFO:list_jira_users: All Jira usernames and display names were saved in /mnt/hdd/repo/lucene-jira-archive/migration/work/jira-users.csv.
[2022-07-11 23:54:34,179] INFO:list_jira_users: Done.

# the Jira users are sorted by activity counts
(.venv) migration $ cat work/jira-users.csv
JiraName,DispName
jira-bot,ASF subversion and git services
mikemccand,Michael McCandless
rcmuir,Robert Muir
uschindler,Uwe Schindler
jpountz,Adrien Grand
sarowe,Steven Rowe
...
```

2. List candidate GitHub accounts

```
(.venv) migration $ python src/list_github_user_candidates.py 
[2022-07-11 23:58:49,368] INFO:list_github_user_candidates: Searching GitHub users
[2022-07-11 23:59:02,052] INFO:list_github_user_candidates: Retrieving GitHub users info
[2022-07-11 23:59:24,585] INFO:list_github_user_candidates: nnnn candidate accounts were found; saved in /mnt/hdd/repo/lucene-jira-archive/migration/work/github-users.csv
[2022-07-11 23:59:24,586] INFO:list_github_user_candidates: Done.

(.venv) migration $ cat work/github-users.csv 
GitHubAccount,Name
rmuir,Robert Muir
jpountz,Adrien Grand
mikemccand,Michael McCandless
...
```

3. Generate a candidate account map

Note that this script emits lots of warnings, please ignore them (the warnings are emitted when checking if the candidate GitHub account has push access on `apache/lucene` repository; if you want to apply this script to another repo, modfy the repo name in the script).

```
(.venv) migration $ python src/map_jira_github_account.py 
[2022-07-12 00:01:45,637] INFO:map_jira_github_account: Generating Jira-GitHub account map
[2022-07-12 00:01:46,153] WARNING:github_issues_util: Assignee RobertMMuir cannot be assigned; status code=404, message={"message":"Not Found","documentation_url":"https://docs.github.com/rest/reference/issues#check-if-a-user-can-be-assigned"}

[2022-07-12 00:01:51,238] INFO:map_jira_github_account: Candidate account mapping was written in /mnt/hdd/repo/lucene-jira-archive/migration/mappings-data/account-map.csv.20220712.000145.
[2022-07-12 00:01:51,239] INFO:map_jira_github_account: Done.
```

4. Manually create the final account mapping

```
# remove false mappings, add/edit correct mappings
(.venv) migration $ vim mappings-data/account-map.csv.20220712.000145

# then copy the edited file to mappings-data/account-map.csv - this is used in "3. Convert Jira issues to GitHub issues" section.
(.venv) migration $ cp mappings-data/account-map.csv.20220712.000145 mappings-data/account-map.csv
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
