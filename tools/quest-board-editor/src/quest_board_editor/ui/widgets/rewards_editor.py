from __future__ import annotations

import copy
from typing import Callable, List, Optional

from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QAbstractItemView,
    QHBoxLayout,
    QHeaderView,
    QPushButton,
    QTableWidget,
    QTableWidgetItem,
    QVBoxLayout,
    QWidget,
)


class RewardsEditor(QWidget):
    def __init__(self, on_change: Optional[Callable[[], None]] = None) -> None:
        super().__init__()
        self._on_change = on_change
        self._loading = False
        self._data: List[dict] = []

        layout = QVBoxLayout(self)
        self.table = QTableWidget(0, 4)
        self.table.setHorizontalHeaderLabels(["kind", "itemId", "count", "grantTo"])
        self.table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        self.table.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows)
        layout.addWidget(self.table)

        row = QHBoxLayout()
        add_btn = QPushButton("Add reward")
        add_btn.clicked.connect(self._add_row)
        rem_btn = QPushButton("Remove selected")
        rem_btn.clicked.connect(self._remove_selected)
        row.addWidget(add_btn)
        row.addWidget(rem_btn)
        row.addStretch()
        layout.addLayout(row)

        self.table.cellChanged.connect(self._cell_changed)

    def set_rewards(self, rewards: List[dict]) -> None:
        self._loading = True
        self._data = copy.deepcopy(rewards)
        self.table.setRowCount(0)
        for r in self._data:
            self._append_row(r)
        self._loading = False

    def get_rewards(self) -> List[dict]:
        return copy.deepcopy(self._data)

    def _append_row(self, reward: dict) -> None:
        row = self.table.rowCount()
        self.table.insertRow(row)
        for col, key in enumerate(("kind", "itemId", "count", "grantTo")):
            val = reward.get(key, "")
            item = QTableWidgetItem(str(val) if val is not None else "")
            if key == "count":
                item.setTextAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
            self.table.setItem(row, col, item)

    def _add_row(self) -> None:
        self._data.append(
            {"kind": "item", "itemId": "Aetherhaven_Gold_Coin", "count": 8, "grantTo": "player"}
        )
        self._append_row(self._data[-1])
        self._emit_change()

    def _remove_selected(self) -> None:
        rows = sorted({i.row() for i in self.table.selectedIndexes()}, reverse=True)
        for row in rows:
            if 0 <= row < len(self._data):
                self._data.pop(row)
                self.table.removeRow(row)
        self._emit_change()

    def _cell_changed(self, row: int, col: int) -> None:
        if self._loading or row >= len(self._data):
            return
        keys = ("kind", "itemId", "count", "grantTo")
        key = keys[col]
        item = self.table.item(row, col)
        text = item.text() if item else ""
        if key == "count":
            try:
                self._data[row][key] = int(text)
            except ValueError:
                self._data[row][key] = 1
        else:
            self._data[row][key] = text
        self._emit_change()

    def _emit_change(self) -> None:
        if self._on_change:
            self._on_change()
