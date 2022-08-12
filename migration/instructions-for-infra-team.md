# Instruction manual for Infra team

These commands should work by copy-pasting.

## Setup

```
# clone the repo
~ $ git clone --single-branch --branch main git@github.com:apache/lucene-jira-archive.git

# move to the tool's directory
~ $ cd lucene-jira-archive/migration/

# wait for us to upload this tgz, then download and unarchive the GitHub importable data
migration $ wget https://home.apache.org/~tomoko/github-import-data.tgz
migration $ tar xzf github-import-data.tgz

migration $ tree -L 1
.
├── README.md
├── github-import-data
├── github-import-data.tgz
├── mappings-data
├── requirements.txt
└── src

migration $ ls -1 github-import-data
GH-LUCENE-1.json
GH-LUCENE-2.json
GH-LUCENE-3.json
...
GH-LUCENE-10676.json
GH-LUCENE-10677.json

# set the GitHub PAT token to an env variable
migration $ cp .env.example .env
migration $ vi .env
export GITHUB_PAT=<set the `asfgit` personal access token to be used for importing here>
# other lines don't need to be touched

# set env variables from .env
migration $ source .env

# setup python virtual env
# note that the script was tested with python 3.9
migration $ python -V
Python 3.9.13

migration $ python -m venv .venv
migration $ . .venv/bin/activate
(.venv) migration $ pip install -r requirements.txt 
(.venv) migration $ pip freeze
certifi==2022.6.15
charset-normalizer==2.0.12
idna==3.3
jira2markdown==0.2.1
pyparsing==2.4.7
python-dateutil==2.8.2
requests==2.28.0
six==1.16.0
urllib3==1.26.9
```

## Test the import script

To make sure everything is correctly set up, you can import one issue for a trial. This command imports only LUCENE-1 to GitHub `apache/lucene` repo.

```
(.venv) migration $ python src/import_github_issues.py --min 1
```

If the command is successfully done, you'll see an issue id mapping file `mapping-data/issue-map.csv`. This will look like this.

```
(.venv) migration $ cat mappings-data/issue-map.csv
JiraKey,GitHubUrl,GitHubNumber
LUCENE-1,https://github.com/apache/lucene/issues/1080,1080
```

Peek at the issue through GitHub and confirm it was imported and looks reasonable.

### Clean up the test data

Once the test is done, please delete `mapping-data/issue-map.csv` local file and the imported issue through GitHub (only admin accounts can delete an issue) before the actual migration.

## Run the import script

Please specify the `min` option to 1 and `max` option to the maximum number of the Lucene Jira issue (we will update these instructions with the actual `MAX_ID` during migration).

```
(.venv) migration $ nohup python src/import_github_issues.py --min 1 --max MAX_ID &
# should take ~24 hours
```

Progress will be written in the log file. E.g.:
```
migration $ cat log/import_github_issues_2022-08-10T13\:57\:56.log 
[2022-08-10 13:57:56,423] INFO:import_github_issues: Importing GitHub issues
[2022-08-10 13:58:00,983] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/forks-migration-test-3/issues/11 was successfully completed.
[2022-08-10 13:58:05,563] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/forks-migration-test-3/issues/12 was successfully completed.
[2022-08-10 13:58:10,096] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/forks-migration-test-3/issues/13 was successfully completed.
...
[2022-08-11 11:56:06,159] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/forks-migration-test-3/issues/10634 was successfully completed.
[2022-08-11 11:56:13,986] DEBUG:import_github_issues: Import GitHub issue https://github.com/mocobeta/forks-migration-test-3/issues/10635 was successfully completed.
[2022-08-11 11:56:13,986] INFO:import_github_issues: Done.
```

## Output files

The import script outputs two files. Both are important for subsequent steps. Please send them back to us via any channels (e.g., attach them to the Jira issue).

```
migration $ ls log/import_github_issues_yyyy-mm-ddTHH:MM:SS.log  # log file
migration $ ls mappings-data/issue-map.csv                       # Jira - GitHub issue id mapping file
```

Thank you!