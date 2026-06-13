from __future__ import annotations

import copy
from typing import Callable, List, Optional

from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QAbstractItemView,
    QHBoxLayout,
    QHeaderView,
    QLineEdit,
    QPushButton,
    QSpinBox,
    QTableWidget,
    QTableWidgetItem,
    QVBoxLayout,
    QWidget,
)


class KillSetsEditor(QWidget):
    def __init__(self, on_change: Optional[Callable[[], None]] = None) -> None:
        super().__init__()
        self._on_change = on_change
        self._loading = False
        self._sets: List[dict] = []

        layout = QVBoxLayout(self)
        self.table = QTableWidget(0, 3)
        self.table.setHorizontalHeaderLabels(["weight", "killCount", "entityTagsAny (comma-separated)"])
        self.table.horizontalHeader().setSectionResizeMode(2, QHeaderView.ResizeMode.Stretch)
        self.table.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows)
        layout.addWidget(self.table)

        row = QHBoxLayout()
        add_btn = QPushButton("Add kill set")
        add_btn.clicked.connect(self._add_row)
        rem_btn = QPushButton("Remove selected")
        rem_btn.clicked.connect(self._remove_selected)
        row.addWidget(add_btn)
        row.addWidget(rem_btn)
        row.addStretch()
        layout.addLayout(row)

        self.table.cellChanged.connect(self._cell_changed)

    def set_kill_sets(self, kill_sets: List[dict]) -> None:
        self._loading = True
        self._sets = copy.deepcopy(kill_sets) if kill_sets else []
        self.table.setRowCount(0)
        for s in self._sets:
            self._append_row(s)
        self._loading = False

    def get_kill_sets(self) -> List[dict]:
        return copy.deepcopy(self._sets)

    def _append_row(self, s: dict) -> None:
        row = self.table.rowCount()
        self.table.insertRow(row)
        w = QTableWidgetItem(str(s.get("weight", 1)))
        w.setTextAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
        self.table.setItem(row, 0, w)
        k = QTableWidgetItem(str(s.get("killCount", 5)))
        k.setTextAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
        self.table.setItem(row, 1, k)
        tags = s.get("entityTagsAny") or []
        self.table.setItem(row, 2, QTableWidgetItem(", ".join(tags)))

    def _add_row(self) -> None:
        self._sets.append({"weight": 1, "killCount": 5, "entityTagsAny": ["Vermin"]})
        self._append_row(self._sets[-1])
        self._emit_change()

    def _remove_selected(self) -> None:
        rows = sorted({i.row() for i in self.table.selectedIndexes()}, reverse=True)
        for row in rows:
            if 0 <= row < len(self._sets):
                self._sets.pop(row)
                self.table.removeRow(row)
        self._emit_change()

    def _cell_changed(self, row: int, col: int) -> None:
        if self._loading or row >= len(self._sets):
            return
        item = self.table.item(row, col)
        text = item.text() if item else ""
        if col == 0:
            try:
                self._sets[row]["weight"] = int(text)
            except ValueError:
                self._sets[row]["weight"] = 1
        elif col == 1:
            try:
                self._sets[row]["killCount"] = int(text)
            except ValueError:
                self._sets[row]["killCount"] = 1
        else:
            tags = [t.strip() for t in text.split(",") if t.strip()]
            self._sets[row]["entityTagsAny"] = tags
        self._emit_change()

    def _emit_change(self) -> None:
        if self._on_change:
            self._on_change()
