from pathlib import Path
import re
import itertools
from dataclasses import dataclass
from collections import defaultdict
from typing import Optional

import jira2markdown
from jira2markdown.elements import MarkupElements
from jira2markdown.markup.advanced import Code
from jira2markdown.markup.lists import UnorderedList, OrderedList
from jira2markdown.markup.text_effects import BlockQuote, Quote, Monospaced
from jira2markdown.markup.text_breaks import Ruler

from markup.advanced import TweakedCode
from markup.lists import UnorderedTweakedList, OrderedTweakedList
from markup.text_effects import EscapeHtmlTag, EscapeQuoteMD, TweakedBlockQuote, TweakedQuote, TweakedMonospaced
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


REGEX_EMBEDDED_IMAGE = r"!([^!\n]+)!"

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
REGEX_MENTION_ATMARK = re.compile(r"(^@[\w\s\.@_-]+?)|((?<=[\s\({\[\"'，．])@[\w\s\.@_-]+?)($|(?=[\s\)}\]\"'\?!,，;:\.．]))")  # this regex may capture only "@" + "<username>" mentions
REGEX_MENION_TILDE = re.compile(r"(^\[~[\w\s\.@_-]+?\])|((?<=[\s\({\[\"'，．])\[~[\w\s\.@_-]+?\])($|(?=[\s\)|\]\"'\?!,，;:\.．]))")  # this regex may capture only "[~" + "<username>" + "]" mentions
REGEX_LINK = re.compile(r"\[([^\]]+)\]\(([^\)]+)\)")
REGEX_GITHUB_ISSUE_LINK = re.compile(r"(\s)(#\d+)(\s)")


# common file extensions in Lucene Jira attachments
# these extensions appear at least three times in Lucene Jira.
FILE_EXT_TO_LANG = {
    ".patch": "diff",
    ".PATCH": "diff",
    ".pat": "diff",
    ".diff": "diff",
    ".java": "java",
    ".jj": "java",
    ".jflex": "java",  # text?
    ".txt": "text",
    ".log": "text",
    ".out": "text",
    ".alg": "text",
    ".perf": "text",
    ".benchmark": "text",
    ".test": "text",
    ".py": "python",
    ".html": "html",
    ".xml": "xml",
    ".sh": "sh",
    ".json": "json",
    ".jsp": "jsp",
    ".properties": "ini"
}

def extract_embedded_image_files(text: str, image_files: list[str]) -> set[str]:
    """Extract embedded image files in the given text.
    https://jira.atlassian.com/secure/WikiRendererHelpAction.jspa?section=images
    """
    # capture candidates for embedded images
    candidates = re.findall(REGEX_EMBEDDED_IMAGE, text)
    embedded_image_files = set([])
    for x in candidates:
        if x in image_files:
            # !xyz.png!
            embedded_image_files.add(x)
        elif any(map(lambda s: x.startswith(s + "|"), image_files)):
            # !xyz.png|styles!
            embedded_image_files.add(x.split("|", 1)[0])
    return embedded_image_files


def convert_text(text: str, att_replace_map: dict[str, str] = {}, account_map: dict[str, str] = {}, jira_users: dict[str, str] = {}, att_dir: Optional[Path] = None) -> str:
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
    elements.replace(Code, TweakedCode)
    elements.replace(UnorderedList, UnorderedTweakedList)
    elements.replace(OrderedList, OrderedTweakedList)
    elements.replace(BlockQuote, TweakedBlockQuote)
    elements.replace(Quote, TweakedQuote)
    elements.replace(Monospaced, TweakedMonospaced)
    elements.insert_after(Ruler, LongRuler)
    elements.append(EscapeHtmlTag)
    elements.append(EscapeQuoteMD)
    text = jira2markdown.convert(text, elements=elements)

    # convert links to attachments
    text = re.sub(REGEX_LINK, repl_att, text)

    # escape github style cross-issue link (#NN)
    text = re.sub(REGEX_GITHUB_ISSUE_LINK, escape_gh_issue_link, text)

    # embed attachments (patches, etc.) if possible
    links = re.findall(REGEX_LINK, text)
    if links and att_dir:
        paths = []
        for link in links:
            try:
                path = att_dir.joinpath(link[0])
                if path.exists():
                    paths.append(path)
            except OSError:
                continue
        if paths:
            path = paths[0]
            # skip unknown file extensions; skip too large files.
            if path.suffix in FILE_EXT_TO_LANG and path.stat().st_size < 50000:
                text += __textdata_as_details(path, FILE_EXT_TO_LANG[path.suffix])

    return text


def __textdata_as_details(path: Path, lang: str) -> str:
    assert path.exists()
    name = path.name
    att_open = "open" if path.stat().st_size < 5000 else ""
    text = ""
    with open(path) as fp:
        try:
            data = fp.read()
            # use <details> markup to collapse long texts as default
            # https://gist.github.com/pierrejoubert73/902cc94d79424356a8d20be2b382e1ab
            text = f"\n<details {att_open}>\n<summary>{name}</summary>\n\n```{lang}\n{data}\n```\n\n</details>\n\n"
        except UnicodeDecodeError:
            # other encoding than 'utf-8' is used in the file
            pass
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
            res = f"#{gh_number}"
            # print(res)
        return res

    text = re.sub(r"(\s)(LUCENE-\d+)([\s,;:\?\!\.])", repl_simple, text)
    text = re.sub(r"(^)(LUCENE-\d+)([\s,;:\?\!\.])", repl_simple, text)
    text = re.sub(r"(\s)https?://issues\.apache\.org/.+/(LUCENE-\d+)([\s,;:\!\.])", repl_simple, text)
    text = re.sub(r"(^)https?://issues\.apache\.org/.+/(LUCENE-\d+)([\s,;:\!\.])", repl_simple, text)
    text = re.sub(r"(\()(LUCENE-\d+)(\))", repl_paren, text)
    text = re.sub(r"(\[)(LUCENE-\d+)(\])(?!\()", repl_bracket, text)
    text = re.sub(r"\[(LUCENE-\d+)\]\(https?[^\)]+LUCENE-\d+\)", repl_md_link, text)

    return text


ASF_JIRA_BASE = "https://issues.apache.org/jira/browse/"

def create_issue_links_outside_projects(text: str) -> str:
    """Create links to outside ASF projects.
    """

    def repl_simple(m: re.Match):
        prj = m.group(2).split("-")[0]
        if prj not in ALL_JIRA_PROJECTS:
            return m.group(0)
        jira_link = ASF_JIRA_BASE + m.group(2)
        return f"{m.group(1)}[{m.group(2)}]({jira_link}){m.group(3)}"

    def repl_paren(m: re.Match):
        prj = m.group(2).split("-")[0]
        if prj not in ALL_JIRA_PROJECTS:
            return m.group(0)
        jira_link = ASF_JIRA_BASE + m.group(2)
        return f"{m.group(1)}[{m.group(2)}]({jira_link}){m.group(3)}"

    def repl_bracket(m: re.Match):
        prj = m.group(2).split("-")[0]
        if prj not in ALL_JIRA_PROJECTS:
            return m.group(0)
        jira_link = ASF_JIRA_BASE + m.group(2)
        return jira_link

    text = re.sub(r"(\s)([A-Z0-9]{2,20}-\d+)([\s,;:\?\!\.])", repl_simple, text)
    text = re.sub(r"(^)([A-Z0-9]{2,20}-\d+)([\s,;:\?\!\.])", repl_simple, text)
    text = re.sub(r"(\()([A-Z0-9]{2,20}-\d+)(\))", repl_paren, text)
    text = re.sub(r"(\[)([A-Z0-9]{2,20}-\d+)(\])(?!\()", repl_bracket, text)

    return text


ALL_JIRA_PROJECTS = set([
    "AAR",
    "ABDERA",
    "ACCUMULO",
    "ACE",
    "ACL",
    "AMQ",
    "AMQNET",
    "APLO",
    "ARTEMIS",
    "AMQCPP",
    "AMQCLI",
    "OPENWIRE",
    "BLAZE",
    "ADDR",
    "AGILA",
    "AIRAVATA",
    "ALOIS",
    "ARMI",
    "AMATERASU",
    "AMBARI",
    "AMBER",
    "ANAKIA",
    "AGE2",
    "AGEOLD",
    "ANY23",
    "APEXCORE",
    "APEXMALHAR",
    "ARROW",
    "ASTERIXDB",
    "AVRO",
    "AWF",
    "BLUEMARLIN",
    "BLUR",
    "CHAINSAW",
    "CMDA",
    "COMMONSSITE",
    "COMMONSRDF",
    "TESTING",
    "CONCERTED",
    "CORAL",
    "CB",
    "CRAIL",
    "CURATOR",
    "DATALAB",
    "DATASKETCHES",
    "DIRECTMEMORY",
    "DORIS",
    "DRILL",
    "DUBBO",
    "ECHARTS",
    "EVENTMESH",
    "FINERACT",
    "FLEX",
    "FREEMARKER",
    "GEARPUMP",
    "GOBBLIN",
    "GORA",
    "HAWQ",
    "HELIX",
    "HOP",
    "HORN",
    "HUDI",
    "INLONG",
    "IOTDB",
    "JENA",
    "KNOX",
    "LENS",
    "LIMINAL",
    "LINKIS",
    "CLOWNFISH",
    "MADLIB",
    "MARVIN",
    "MASFRES",
    "METAMODEL",
    "MXNET",
    "NEMO",
    "NETBEANSINFRA",
    "NIFI",
    "MINIFI",
    "MINIFICPP",
    "NUTTX",
    "OLTU",
    "ONAMI",
    "CLIMATE",
    "OPENAZ",
    "HDDS",
    "PEGASUS",
    "PETRI",
    "PINOT",
    "PLC4X",
    "QPIDIT",
    "QUICKSTEP",
    "RAT",
    "RIPPLE",
    "ROCKETMQ",
    "ROL",
    "S4",
    "SDAP",
    "SCIMPLE",
    "SEDONA",
    "SCB",
    "STORM",
    "SUBMARINE",
    "TAVERNA",
    "TENTACLES",
    "TEZ",
    "MTOMCAT",
    "TRAFODION",
    "TRAINING",
    "TWILL",
    "UNOMI",
    "WAYANG",
    "WHIRR",
    "WHISKER",
    "YUNIKORN",
    "ZIPKIN",
    "APISIX",
    "MRM",
    "ARIA",
    "ARIES",
    "ASYNCWEB",
    "ATLAS",
    "ATTIC",
    "AURORA",
    "AVALON",
    "AVNSHARP",
    "RUNTIME",
    "STUDIO",
    "CENTRAL",
    "PLANET",
    "TOOLS",
    "PNIX",
    "AXIOM",
    "AXIS",
    "AXISCPP",
    "WSIF",
    "AXIS2",
    "TRANSPORTS",
    "AXIS2C",
    "BAHIR",
    "BATCHEE",
    "BATIK",
    "BEAM",
    "BEEHIVE",
    "BIGTOP",
    "BLUESKY",
    "BOOKKEEPER",
    "TM",
    "BROOKLYN",
    "BUILDR",
    "BVAL",
    "STDCXX",
    "CACTUS",
    "CALCITE",
    "CAMEL",
    "CARBONDATA",
    "CASSANDRA",
    "CAY",
    "CELIX",
    "CS",
    "CMIS",
    "CHUKWA",
    "CLEREZZA",
    "CLK",
    "CLKE",
    "CLOUDSTACK",
    "COCOON",
    "COCOON3",
    "ATTRIBUTES",
    "BCEL",
    "BEANUTILS",
    "BETWIXT",
    "BSF",
    "CHAIN",
    "CLI",
    "CODEC",
    "COLLECTIONS",
    "COMPRESS",
    "CONFIGURATION",
    "CRYPTO",
    "CSV",
    "DAEMON",
    "DBCP",
    "DBUTILS",
    "DIGESTER",
    "DISCOVERY",
    "DORMANT",
    "EL",
    "EMAIL",
    "EXEC",
    "FEEDPARSER",
    "FILEUPLOAD",
    "FUNCTOR",
    "GEOMETRY",
    "IMAGING",
    "IO",
    "JCI",
    "JCS",
    "JELLY",
    "JEXL",
    "JXPATH",
    "LANG",
    "LAUNCHER",
    "LOGGING",
    "MATH",
    "MODELER",
    "NET",
    "NUMBERS",
    "OGNL",
    "POOL",
    "PRIMITIVES",
    "PROXY",
    "RESOURCES",
    "RNG",
    "SANDBOX",
    "SANSELAN",
    "SCXML",
    "STATISTICS",
    "TEXT",
    "TRANSACTION",
    "VALIDATOR",
    "VFS",
    "WEAVER",
    "COMDEV",
    "CONTINUUM",
    "COR",
    "COTTON",
    "COUCHDB",
    "CRUNCH",
    "CTAKES",
    "CUSTOS",
    "CXF",
    "DOSGI",
    "CXFXJC",
    "FEDIZ",
    "DAFFODIL",
    "DATAFU",
    "DAYTRADER",
    "DDLUTILS",
    "DTACLOUD",
    "DELTASPIKE",
    "DEPOT",
    "DERBY",
    "DMAP",
    "DIR",
    "DIRSERVER",
    "DIRAPI",
    "DIRGROOVY",
    "DIRKRB",
    "DIRNAMING",
    "DIRSHARED",
    "DIRSTUDIO",
    "DL",
    "DI",
    "DBF",
    "DROIDS",
    "DVSL",
    "EAGLE",
    "EASYANT",
    "ECS",
    "EDGENT",
    "EMPIREDB",
    "ESME",
    "ESCIMO",
    "ETCH",
    "EWS",
    "EXLBR",
    "FORTRESS",
    "FALCON",
    "FELIX",
    "FINCN",
    "FLAGON",
    "FLINK",
    "FLUME",
    "FOP",
    "FOR",
    "FC",
    "FTPSERVER",
    "GBUILD",
    "GEODE",
    "GERONIMO",
    "GERONIMODEVTOOLS",
    "GIRAPH",
    "GOSSIP",
    "GRFT",
    "GRIFFIN",
    "GROOVY",
    "GSHELL",
    "GUACAMOLE",
    "GUMP",
    "HADOOP",
    "HDT",
    "HDFS",
    "MAPREDUCE",
    "YARN",
    "HAMA",
    "HARMONY",
    "HBASE",
    "HCATALOG",
    "HERALDRY",
    "HISE",
    "HIVE",
    "HIVEMALL",
    "HIVEMIND",
    "HTRACE",
    "HTTPASYNC",
    "HTTPCLIENT",
    "HTTPCORE",
    "IBATISNET",
    "IBATIS",
    "RBATIS",
    "IGNITE",
    "IMPALA",
    "IMPERIUS",
    "INCUBATOR",
    "INFRATEST3",
    "INFRATEST987",
    "INFRACLOUD1",
    "INFRA",
    "TEST6",
    "IOTA",
    "ISIS",
    "IVY",
    "IVYDE",
    "JCR",
    "JCRVLT",
    "JCRBENCH",
    "JCRCL",
    "JCRSERVLET",
    "JCRTCK",
    "JCRRMI",
    "OAK",
    "OCM",
    "JCRSITE",
    "HUPA",
    "IMAP",
    "JDKIM",
    "JSIEVE",
    "JSPF",
    "MAILBOX",
    "MAILET",
    "MIME4J",
    "MPT",
    "POSTAGE",
    "PROTOCOLS",
    "JAMES",
    "JAXME",
    "JCLOUDS",
    "JDO",
    "JS1",
    "JS2",
    "JOHNZON",
    "JOSHUA",
    "JSEC",
    "JSPWIKI",
    "JUDDI",
    "JUNEAU",
    "KAFKA",
    "KALUMET",
    "KAND",
    "KARAF",
    "KATO",
    "KI",
    "KITTY",
    "KUDU",
    "KYLIN",
    "LABS",
    "HTTPDRAFT",
    "LEGAL",
    "LIBCLOUD",
    "LIVY",
    "LOGCXX",
    "LOG4J2",
    "LOG4NET",
    "LOG4PHP",
    "LOKAHI",
    "LUCENENET",
    "LCN4C",
    "LUCY",
    "MAHOUT",
    "CONNECTORS",
    "MARMOTTA",
    "MNG",
    "MACR",
    "MANT",
    "MANTTASKS",
    "MANTRUN",
    "ARCHETYPE",
    "MARCHETYPES",
    "MARTIFACT",
    "MASSEMBLY",
    "MBUILDCACHE",
    "MCHANGELOG",
    "MCHANGES",
    "MCHECKSTYLE",
    "MCLEAN",
    "MCOMPILER",
    "MDEP",
    "MDEPLOY",
    "MDOAP",
    "MDOCCK",
    "DOXIA",
    "DOXIASITETOOLS",
    "DOXIATOOLS",
    "MEAR",
    "MECLIPSE",
    "MEJB",
    "MENFORCER",
    "MGPG",
    "MPH",
    "MINDEXER",
    "MINSTALL",
    "MINVOKER",
    "MJAR",
    "MJARSIGNER",
    "MJAVADOC",
    "MJDEPRSCAN",
    "MJDEPS",
    "MJLINK",
    "MJMOD",
    "JXR",
    "MLINKCHECK",
    "MPATCH",
    "MPDF",
    "MPLUGINTESTING",
    "MPLUGIN",
    "MPMD",
    "MPOM",
    "MPIR",
    "MNGSITE",
    "MRAR",
    "MRELEASE",
    "MRRESOURCES",
    "MREPOSITORY",
    "MRESOLVER",
    "MRESOURCES",
    "SCM",
    "MSCMPUB",
    "MSCRIPTING",
    "MSHADE",
    "MSHARED",
    "MSITE",
    "MSKINS",
    "MSOURCES",
    "MSTAGE",
    "SUREFIRE",
    "MTOOLCHAINS",
    "MVERIFIER",
    "WAGON",
    "MWAR",
    "MWRAPPER",
    "MAVIBOT",
    "MEECROWAVE",
    "MESOS",
    "METRON",
    "MILAGRO",
    "DIRMINA",
    "SSHD",
    "MIRAE",
    "MNEMONIC",
    "MODPYTHON",
    "MRQL",
    "MRUNIT",
    "MUSE",
    "MXNETTEST",
    "ADFFACES",
    "EXTCDI",
    "MFCOMMONS",
    "MYFACES",
    "EXTSCRIPT",
    "EXTVAL",
    "MFHTML5",
    "ORCHESTRA",
    "PORTLETBRIDGE",
    "MYFACESTEST",
    "TOBAGO",
    "TOMAHAWK",
    "TRINIDAD",
    "MYNEWT",
    "MYNEWTDOC",
    "MYRIAD",
    "NEETHI",
    "NETBEANS",
    "NIFIREG",
    "NIFILIBS",
    "NLPCRAFT",
    "NPANDAY",
    "NUTCH",
    "NUVEM",
    "ODE",
    "JACOB",
    "OWC",
    "ODFTOOLKIT",
    "OFBIZ",
    "OJB",
    "OLINGO",
    "OLIO",
    "OODT",
    "OOZIE",
    "ORP",
    "OPENEJB",
    "OEP",
    "OPENJPA",
    "OPENMEETINGS",
    "OPENNLP",
    "OPENOFFICE",
    "OWB",
    "ORC",
    "PARQUET",
    "PDFBOX",
    "PHOENIX",
    "OMID",
    "TEPHRA",
    "PHOTARK",
    "PIG",
    "PIRK",
    "PIVOT",
    "PLUTO",
    "PODLINGNAMESEARCH",
    "POLYGENE",
    "PORTALS",
    "APA",
    "PB",
    "PIO",
    "PROVISIONR",
    "PRC",
    "HERMES",
    "PULSAR",
    "PYLUCENE",
    "QPID",
    "DISPATCH",
    "QPIDJMS",
    "PROTON",
    "RAMPART",
    "RAMPARTC",
    "RANGER",
    "RATIS",
    "RAVE",
    "REEF",
    "RIVER",
    "RYA",
    "S2GRAPH",
    "SAMOA",
    "SAMZA",
    "SAND",
    "SANDESHA2",
    "SANDESHA2C",
    "SANTUARIO",
    "SAVAN",
    "SCOUT",
    "SENTRY",
    "SERF",
    "SM",
    "SMX4",
    "SMXCOMP",
    "SMX4KNL",
    "SMX4NMR",
    "SHALE",
    "SHINDIG",
    "SHIRO",
    "CASSANDRASC",
    "SINGA",
    "SIRONA",
    "SLIDER",
    "SLING",
    "SOAP",
    "SOLR",
    "SPARK",
    "SIS",
    "SPOT",
    "SQOOP",
    "STANBOL",
    "STEVE",
    "STOMP",
    "STONEHENGE",
    "STRATOS",
    "STREAMPIPES",
    "STREAMS",
    "STR",
    "WW",
    "SB",
    "SITE",
    "SVN",
    "SUPERSET",
    "SYNAPSE",
    "SYNCOPE",
    "SYSTEMDS",
    "TAJO",
    "TAMAYA",
    "TAPESTRY",
    "TAP5",
    "TASHI",
    "TST",
    "TEXEN",
    "MMETRIC",
    "THRIFT",
    "TIKA",
    "TILES",
    "AUTOTAG",
    "TEVAL",
    "TREQ",
    "TILESSB",
    "TILESSHARED",
    "TILESSHOW",
    "TINKERPOP",
    "TOMEE",
    "TATPI",
    "TOREE",
    "TORQUE",
    "TORQUEOLD",
    "TC",
    "TS",
    "DIRTSEC",
    "TRIPLES",
    "TSIK",
    "TRB",
    "TUSCANY",
    "TUWENI",
    "UIMA",
    "USERGRID",
    "VCL",
    "VELOCITY",
    "VELOCITYSB",
    "VELTOOLS",
    "VXQUERY",
    "VYSPER",
    "WADI",
    "WAVE",
    "WEEX",
    "WHIMSY",
    "WICKET",
    "WINK",
    "WODEN",
    "WOOKIE",
    "WSCOMMONS",
    "APOLLO",
    "WSRP4J",
    "WSS",
    "ASFSITE",
    "XALANC",
    "XALANJ",
    "XAP",
    "XBEAN",
    "XERCESC",
    "XERCESP",
    "XERCESJ",
    "XMLCOMMONS",
    "XMLRPC",
    "XMLBEANS",
    "XGC",
    "XMLSCHEMA",
    "XW",
    "YETUS",
    "YOKO",
    "ZEPPELIN",
    "ZETACOMP",
    "ZOOKEEPER"
])
