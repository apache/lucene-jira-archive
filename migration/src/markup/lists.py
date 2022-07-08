from pyparsing import (
    Char,
    Combine,
    LineEnd,
    LineStart,
    Literal,
    MatchFirst,
    OneOrMore,
    ZeroOrMore,
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
from jira2markdown.markup.lists import ListIndentState, ListIndent


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
            bullets, text = line.split(" ", maxsplit=1)

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
        ROW = Optional(LineStart()) + Combine(
            Optional(White(" \t"))
            + Optional(self.nested_token, default="")
            + ListIndent(self.indent_state, self.tokens)
            + SkipTo(NL + Optional(White(" \t")) + Char(self.nested_token + self.tokens) | LIST_BREAK, ignore=IGNORE)
            + Optional(NL),
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
