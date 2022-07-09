from pyparsing import Literal, LineEnd, ParserElement, StringStart, replaceWith

from jira2markdown.markup.base import AbstractMarkup
from jira2markdown.markup.text_breaks import LineBreak


class LongRuler(AbstractMarkup):
    is_inline_element = False

    @property
    def expr(self) -> ParserElement:
        # Text with dashed below it turns into a heading. To prevent this
        # add a line break before the dashes.
        return (
            ("\n" | StringStart() | LineBreak(**self.init_kwargs).expr)
            + Literal("-")[5, ...].setParseAction(replaceWith("\n-----"))
            + LineEnd()
        )
