import re

from pyparsing import (
    ParserElement,
    ParseResults,
    QuotedString,
    StringEnd,
    StringStart,
    SkipTo,
    Literal,
    LineEnd,
    Optional,
    Combine,
    White,
    OneOrMore,
    nums,
    replaceWith,

)

from jira2markdown.markup.base import AbstractMarkup

class TweakedBlockQuote(AbstractMarkup):
    def action(self, tokens: ParseResults) -> str:
        text = self.markup.transformString("\n".join([line.lstrip() for line in tokens[0].strip().splitlines()]))
        # escape numbered list in quotes.
        # e.g.,
        #   {quote}
        #   2. foo
        #   5. bar
        #   {quote}
        # should be
        #   > 2\\. foo
        #   > 5\\. bar
        pat_ordered_list = re.compile(r"((?<=^\d)||(?<=^\d\d))\.")
        return "\n".join(["> " + re.sub(pat_ordered_list, '\.', line) for line in text.splitlines()]) + "\n"  # needs additional line feed at the end of quotation to preserve indentation

    @property
    def expr(self) -> ParserElement:
        return QuotedString("{quote}", multiline=True).setParseAction(self.action)


class TweakedQuote(AbstractMarkup):
    is_inline_element = False

    @property
    def expr(self) -> ParserElement:
        return ("\n" | StringStart()) + Combine(
            Literal("bq. ").setParseAction(replaceWith("> "))
            + SkipTo(LineEnd()) + LineEnd().setParseAction(replaceWith("\n\n")) # needs additional line feed at the end of quotation to preserve indentation
        )
