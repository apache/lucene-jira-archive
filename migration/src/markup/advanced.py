from pyparsing import (
    Combine,
    FollowedBy,
    Literal,
    Optional,
    ParserElement,
    ParseResults,
    SkipTo,
    Word,
    alphanums,
)

from jira2markdown.markup.base import AbstractMarkup


class TweakedCode(AbstractMarkup):
    def action(self, tokens: ParseResults) -> str:
        lang = tokens.lang or "Java"
        text = tokens.text.strip("\n")
        # insert '\n' before and after the code block.
        return f"\n```{lang}\n{text}\n```\n"

    @property
    def expr(self) -> ParserElement:
        return Combine(
            "{code"
            + Optional(
                ":" + Word(alphanums + "#+").setResultsName("lang") + FollowedBy(Literal("}") | Literal("|")),
            )
            + ...
            + "}"
            + SkipTo("{code}").setResultsName("text")
            + "{code}",
        ).setParseAction(self.action)
