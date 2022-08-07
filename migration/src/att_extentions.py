from operator import itemgetter
from pathlib import Path
from collections import defaultdict

ATT_DIR = "/home/moco/tmp/lucene-jira-archive/attachments/"

path = Path(ATT_DIR)
extensions = defaultdict(int)

for f in path.glob("**/*"):
    if f.is_file():
        p = f.name.rsplit(".")
        if len(p) < 2:
            continue
        ext = p[1]
        extensions[ext] += 1

for (ext, count) in sorted(extensions.items(), key=itemgetter(1), reverse=True):
    if count > 2:
        print(f"{ext} ({count})")
