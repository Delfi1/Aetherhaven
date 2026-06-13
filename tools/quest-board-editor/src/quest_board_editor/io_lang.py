from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple


@dataclass
class LangLine:
    raw: str
    key: Optional[str] = None
    value: Optional[str] = None


@dataclass
class LangDocument:
    lines: List[LangLine] = field(default_factory=list)
    _index: Dict[str, int] = field(default_factory=dict, repr=False)

    def rebuild_index(self) -> None:
        self._index.clear()
        for i, line in enumerate(self.lines):
            if line.key is not None:
                self._index[line.key] = i

    def get(self, lang_key: str, default: str = "") -> str:
        idx = self._index.get(lang_key)
        if idx is None:
            return default
        val = self.lines[idx].value
        return val if val is not None else default

    def set(self, lang_key: str, value: str) -> None:
        idx = self._index.get(lang_key)
        if idx is not None:
            self.lines[idx].value = value
            self.lines[idx].raw = f"{lang_key}={value}"
            return
        if self.lines and self.lines[-1].key is not None:
            self.lines.append(LangLine(raw=""))
        self.lines.append(LangLine(raw=f"{lang_key}={value}", key=lang_key, value=value))
        self._index[lang_key] = len(self.lines) - 1


def parse_lang(text: str) -> LangDocument:
    lines: List[LangLine] = []
    for raw in text.splitlines():
        stripped = raw.strip()
        if not stripped or stripped.startswith("#"):
            lines.append(LangLine(raw=raw))
            continue
        if "=" not in raw:
            lines.append(LangLine(raw=raw))
            continue
        key, _, value = raw.partition("=")
        key = key.strip()
        if not key:
            lines.append(LangLine(raw=raw))
            continue
        lines.append(LangLine(raw=raw, key=key, value=value))
    doc = LangDocument(lines=lines)
    doc.rebuild_index()
    return doc


def serialize_lang(doc: LangDocument) -> str:
    out: List[str] = []
    for line in doc.lines:
        if line.key is not None:
            out.append(f"{line.key}={line.value or ''}")
        else:
            out.append(line.raw)
    return "\n".join(out) + "\n"


def load_lang(path: Path) -> LangDocument:
    return parse_lang(path.read_text(encoding="utf-8"))


def save_lang(path: Path, doc: LangDocument) -> None:
    path.write_text(serialize_lang(doc), encoding="utf-8")
