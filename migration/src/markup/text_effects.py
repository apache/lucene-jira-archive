import re

from pyparsing import (
    ParserElement,
    ParseResults,
    QuotedString,
    StringStart,
    SkipTo,
    Literal,
    LineStart,
    LineEnd,
    Combine,
    Literal,
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

    def action(self, tokens: ParseResults) -> str:
        # escape HTML tag
        token = tokens[0].replace("<", "&lt;")
        token = token.replace(">", "&gt;")
        return token

    @property
    def expr(self) -> ParserElement:
        return ("\n" | StringStart()) + Combine(
            Literal("bq. ").setParseAction(replaceWith("> "))
            + SkipTo(LineEnd()).setParseAction(self.action)
            + LineEnd().setParseAction(replaceWith("\n\n")) # needs additional line feed at the end of quotation to preserve indentation
        )


class TweakedMonospaced(AbstractMarkup):
    def action(self, tokens: ParseResults) -> str:
        # remove extra brackets in {{monospaced}}
        # e.g. {{{}BooleanScorer{}}}
        token = re.sub(r"^[{}]+", "", tokens[0])
        token = re.sub(r"[{}]+$", "", token)
        return f"`{token}`"

    @property
    def expr(self) -> ParserElement:
        return QuotedString("{{", endQuoteChar="}}").setParseAction(self.action)


class EscapeHtmlTag(AbstractMarkup):
    """
    Escapes HTML characters that are not a part of any expression grammar
    """

    @property
    def expr(self) -> ParserElement:
        return Literal("<").setParseAction(replaceWith("&lt;")) + SkipTo(Literal(">")) + Literal(">").setParseAction(replaceWith("&gt;"))


class EscapeQuoteMD(AbstractMarkup):
    """
    Escapes '>' at the start of the line that are not a part of any expression grammar
    """

    @property
    def expr(self) -> ParserElement:
        return LineStart() + Literal(">").setParseAction(replaceWith("&gt;"))

