from operator import itemgetter
from pathlib import Path
import json
import re
import itertools
from collections import defaultdict

from common import JIRA_DUMP_DIRNAME, MAPPINGS_DATA_DIRNAME, JIRA_USERS_FILENAME, ACCOUNT_MAPPING_FILENAME, read_jira_users_map, read_account_map
from jira_util import REGEX_MENION_TILDE, extract_description, extract_comments

dump_dir = Path(__file__).resolve().parent.parent.joinpath(JIRA_DUMP_DIRNAME)
mappings_dir = Path(__file__).resolve().parent.parent.joinpath(MAPPINGS_DATA_DIRNAME)
jira_users_file = mappings_dir.joinpath(JIRA_USERS_FILENAME)
jira_users = read_jira_users_map(jira_users_file) if jira_users_file.exists() else {}
account_mapping_file = mappings_dir.joinpath(ACCOUNT_MAPPING_FILENAME)
account_map = read_account_map(account_mapping_file) if account_mapping_file.exists() else {}


def extract_tilde_mentions(text):
    mentions = re.findall(REGEX_MENION_TILDE, text)
    mentions = set(filter(lambda x: x != '', itertools.chain.from_iterable(mentions)))
    mentions = [x[2:-1] for x in mentions]
    return mentions


orphan_ids = defaultdict(int)
for dump_file in dump_dir.glob("LUCENE-*.json"):
    mentions = set([])
    with open(dump_file) as fp:
        o = json.load(fp)
        description = extract_description(o)
        mentions.update(extract_tilde_mentions(description))
        comments = extract_comments(o)
        for (_, _, comment, _, _, _) in comments:
            mentions.update(extract_tilde_mentions(comment))
    for m in mentions:
        if m not in account_map:
            orphan_ids[m] += 1

orphan_ids = sorted(orphan_ids.items(), key=itemgetter(1), reverse=True)
for id, count in orphan_ids:
    print(f'{id}\t{count}')
