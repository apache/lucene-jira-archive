# Instruction manual for Infra team

These commands should work by copy-pasting.

## Setup

```
# clone the repo
~ $ git clone --single-branch --branch main git@github.com:apache/lucene-jira-archive.git

# move to the tool's directory
~ $ cd lucene-jira-archive/migration/

# download and unarchive the GitHub importable data (i will upload the tgz)
migration $ wget https://home.apache.org/~tomoko/github-import-data.tgz
migration $ tree -L 1
.
├── README.md
├── github-import-data
├── github-import-data.tgz
├── mappings-data
├── requirements.txt
└── src

migration $ tar xzf github-import-data.tgz
(.venv) migration $ ls -1 github-import-data | head
GH-LUCENE-1.json
GH-LUCENE-2.json
GH-LUCENE-3.json
...

# set the PAT token to an env variable
migration $ cp .env.example .env
migration $ vi .env
export GITHUB_PAT=<set the personal access token here>
# you don't need to touch other lines

migration $ source .env

# setup python virtual env
migration $ python -V
Python 3.9.13  # the script was tested with python 3.9
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

## Run the import script
```
(.venv) migration $ nohup python src/import_github_issues.py --min 1 --max <will be known> &
# would take 24 hours
```

## Output files

The import script outputs two files. Both are important for succeeding steps, please send me back them via any channels (e.g., attach them to the Jira issue).

```
migration $ ls log/import_github_issues_yyyy-mm-ddTHH:MM:SS.log
migration $ ls mappings-data/issue-map.csv
```

Thank you!