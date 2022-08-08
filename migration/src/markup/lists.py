from pyparsing import (
    Char,
    Combine,
    LineEnd,
    LineStart,
    Literal,
    MatchFirst,
    OneOrMore,
    Optional,
    ParserElement,
    ParseResults,
    SkipTo,
    StringEnd,
    White,
)

from jira2markdown.markup.advanced import Panel
from jira2markdown.markup.base import AbstractMarkup
from jira2markdown.markup.text_effects import BlockQuote, Color
from jira2markdown.markup.lists import ListIndentState


class ListIndentTabSupport(ParserElement):
    def __init__(self, indent_state: ListIndentState, tokens: str, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.name = "ListIndent"
        self.indent_state = indent_state
        self.tokens = tokens

    def parseImpl(self, instring, loc, doActions=True):
        exprs = []
        for token in self.tokens:
            for indent in range(self.indent_state.indent + 1, max(0, self.indent_state.indent - 2), -1):
                # occasionally, the separator can be a tab (commitbot's comment)
                # https://github.com/apache/lucene-jira-archive/issues/112
                exprs.append(Literal(token * indent + " ") | Literal(token * indent + "\t"))

        loc, result = MatchFirst(exprs).parseImpl(instring, loc, doActions)
        self.indent_state.indent = len(result[0]) - 1
        return loc, result


class TweakedList(AbstractMarkup):
    is_inline_element = False

    def __init__(self, nested_token: str, nested_indent: int, tokens: str, indent: int, bullet: str, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.nested_token = nested_token
        self.nested_indent = nested_indent
        self.tokens = tokens
        self.indent = indent
        self.bullet = bullet

        self.indent_state = ListIndentState()

    def action(self, tokens: ParseResults) -> str:
        result = []

        for line in tokens:
            # print(repr(line))
            if line == "\n":
                # can't really explain but if this is the first item, an empty string should be added to preserve line feed
                if len(result) == 0:
                    result.append("")
                continue
            cols = line.split(" ", maxsplit=1)
            if len(cols) > 1:
                bullets, text = cols[0], cols[1]
            else:
                # occasionally, the separator can be a tab (commitbot's comment)
                cols = line.split("\t", maxsplit=1)
                if len(cols) > 1:
                    bullets, text = cols[0], cols[1]
                else:
                    continue

            nested_indent = 0
            while bullets[0] == self.nested_token:
                nested_indent += 1
                bullets = bullets[1:]

            count = nested_indent * self.nested_indent + len(bullets) * self.indent

            line_padding = " " * count
            item_padding = " " * (count - self.indent) + self.bullet + " "
            text = self.markup.transformString(text).splitlines() or [""]

            result.append(
                "\n".join([item_padding + line if i == 0 else line_padding + line for i, line in enumerate(text)]),
            )

        self.indent_state.reset()
        text_end = "\n" if (tokens[-1][-1] == "\n") else ""
        return "\n".join(result) + text_end

    @property
    def expr(self) -> ParserElement:
        NL = LineEnd()
        LIST_BREAK = NL + Optional(White(" \t")) + NL | StringEnd()
        IGNORE = BlockQuote(**self.init_kwargs).expr | Panel(**self.init_kwargs).expr | Color(**self.init_kwargs).expr
        ROW = (LineStart() ^ LineEnd()) + Combine(
            Optional(NL)
            + Optional(self.nested_token, default="")
            + ListIndentTabSupport(self.indent_state, self.tokens)
            + SkipTo(NL + Optional(White(" \t")) + Char(self.nested_token + self.tokens) | LIST_BREAK, ignore=IGNORE)
        )

        return OneOrMore(ROW, stopOn=LIST_BREAK).setParseAction(self.action)


class UnorderedTweakedList(TweakedList):
    def __init__(self, *args, **kwargs):
        super().__init__(nested_token="#", nested_indent=3, tokens="*-", indent=2, bullet="-", *args, **kwargs)

    def action(self, tokens: ParseResults) -> str:
        result = super().action(tokens)
        first_line = (result.splitlines() or [""])[0].strip()

        # Text with dashed below it turns into a heading. To prevent this
        # add a line break before an empty list.
        if first_line == "-":
            return "\n" + result
        else:
            return result


class OrderedTweakedList(TweakedList):
    def __init__(self, *args, **kwargs):
        super().__init__(nested_token="*", nested_indent=2, tokens="#", indent=3, bullet="1.", *args, **kwargs)
