from __future__ import annotations

import copy
from typing import Callable, List, Optional

from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QAbstractItemView,
    QGroupBox,
    QHBoxLayout,
    QHeaderView,
    QLabel,
    QPushButton,
    QSpinBox,
    QTableWidget,
    QTableWidgetItem,
    QVBoxLayout,
    QWidget,
)


class RaidSetsEditor(QWidget):
    def __init__(self, on_change: Optional[Callable[[], None]] = None) -> None:
        super().__init__()
        self._on_change = on_change
        self._loading = False
        self._sets: List[dict] = []
        self._current_set = 0

        layout = QVBoxLayout(self)
        sel_row = QHBoxLayout()
        sel_row.addWidget(QLabel("Raid set:"))
        self.set_spin = QSpinBox()
        self.set_spin.setMinimum(1)
        self.set_spin.valueChanged.connect(self._on_set_changed)
        sel_row.addWidget(self.set_spin)
        add_set = QPushButton("Add set")
        add_set.clicked.connect(self._add_set)
        rem_set = QPushButton("Remove set")
        rem_set.clicked.connect(self._remove_set)
        sel_row.addWidget(add_set)
        sel_row.addWidget(rem_set)
        sel_row.addStretch()
        layout.addLayout(sel_row)

        weight_row = QHBoxLayout()
        weight_row.addWidget(QLabel("Set weight:"))
        self.weight_spin = QSpinBox()
        self.weight_spin.setMinimum(1)
        self.weight_spin.setMaximum(9999)
        self.weight_spin.valueChanged.connect(self._weight_changed)
        weight_row.addWidget(self.weight_spin)
        weight_row.addStretch()
        layout.addLayout(weight_row)

        counts_box = QGroupBox("Mob counts by rank")
        counts_layout = QVBoxLayout(counts_box)
        self.counts_table = QTableWidget(0, 2)
        self.counts_table.setHorizontalHeaderLabels(["rank", "count"])
        self.counts_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        counts_layout.addWidget(self.counts_table)
        cnt_btns = QHBoxLayout()
        add_cnt = QPushButton("Add rank count")
        add_cnt.clicked.connect(self._add_count)
        rem_cnt = QPushButton("Remove rank count")
        rem_cnt.clicked.connect(self._remove_count)
        cnt_btns.addWidget(add_cnt)
        cnt_btns.addWidget(rem_cnt)
        cnt_btns.addStretch()
        counts_layout.addLayout(cnt_btns)
        layout.addWidget(counts_box)

        pool_box = QGroupBox("Mob pool")
        pool_layout = QVBoxLayout(pool_box)
        self.pool_table = QTableWidget(0, 2)
        self.pool_table.setHorizontalHeaderLabels(["roleId", "weight"])
        self.pool_table.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeMode.Stretch)
        pool_layout.addWidget(self.pool_table)
        pool_btns = QHBoxLayout()
        add_mob = QPushButton("Add mob")
        add_mob.clicked.connect(self._add_mob)
        rem_mob = QPushButton("Remove mob")
        rem_mob.clicked.connect(self._remove_mob)
        pool_btns.addWidget(add_mob)
        pool_btns.addWidget(rem_mob)
        pool_btns.addStretch()
        pool_layout.addLayout(pool_btns)
        layout.addWidget(pool_box)

        self.counts_table.cellChanged.connect(self._counts_changed)
        self.pool_table.cellChanged.connect(self._pool_changed)

    def set_raid_sets(self, raid_sets: List[dict]) -> None:
        self._loading = True
        self._sets = copy.deepcopy(raid_sets) if raid_sets else [
            {"weight": 1, "mobCountsByRank": {"D": 6}, "mobPool": [{"roleId": "Goblin_Scrapper", "weight": 3}]}
        ]
        self.set_spin.setMaximum(max(1, len(self._sets)))
        self._current_set = 0
        self.set_spin.setValue(1)
        self._refresh()
        self._loading = False

    def get_raid_sets(self) -> List[dict]:
        return copy.deepcopy(self._sets)

    def _current(self) -> dict:
        if not self._sets:
            self._sets = [{"weight": 1, "mobCountsByRank": {}, "mobPool": []}]
        return self._sets[self._current_set]

    def _refresh(self) -> None:
        self._loading = True
        cur = self._current()
        self.weight_spin.setValue(int(cur.get("weight", 1)))

        counts = cur.get("mobCountsByRank") or {}
        self.counts_table.setRowCount(0)
        for rank, count in counts.items():
            row = self.counts_table.rowCount()
            self.counts_table.insertRow(row)
            self.counts_table.setItem(row, 0, QTableWidgetItem(str(rank)))
            c = QTableWidgetItem(str(count))
            c.setTextAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
            self.counts_table.setItem(row, 1, c)

        pool = cur.get("mobPool") or []
        self.pool_table.setRowCount(0)
        for mob in pool:
            row = self.pool_table.rowCount()
            self.pool_table.insertRow(row)
            self.pool_table.setItem(row, 0, QTableWidgetItem(str(mob.get("roleId", ""))))
            w = QTableWidgetItem(str(mob.get("weight", 1)))
            w.setTextAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
            self.pool_table.setItem(row, 1, w)
        self._loading = False

    def _on_set_changed(self, one_based: int) -> None:
        if self._loading:
            return
        self._current_set = max(0, one_based - 1)
        if self._current_set >= len(self._sets):
            self._current_set = len(self._sets) - 1
        self._refresh()

    def _add_set(self) -> None:
        self._sets.append(
            {
                "weight": 1,
                "mobCountsByRank": {"D": 6},
                "mobPool": [{"roleId": "Goblin_Scrapper", "weight": 3}],
            }
        )
        self.set_spin.setMaximum(len(self._sets))
        self.set_spin.setValue(len(self._sets))
        self._emit_change()

    def _remove_set(self) -> None:
        if len(self._sets) <= 1:
            return
        self._sets.pop(self._current_set)
        self._current_set = min(self._current_set, len(self._sets) - 1)
        self.set_spin.setMaximum(len(self._sets))
        self.set_spin.setValue(self._current_set + 1)
        self._emit_change()

    def _weight_changed(self, val: int) -> None:
        if self._loading:
            return
        self._current()["weight"] = val
        self._emit_change()

    def _add_count(self) -> None:
        counts = self._current().setdefault("mobCountsByRank", {})
        counts["D"] = 6
        self._refresh()
        self._emit_change()

    def _remove_count(self) -> None:
        rows = sorted({i.row() for i in self.counts_table.selectedIndexes()}, reverse=True)
        counts = self._current().get("mobCountsByRank") or {}
        keys = list(counts.keys())
        for row in rows:
            if 0 <= row < len(keys):
                del counts[keys[row]]
                keys = list(counts.keys())
        self._current()["mobCountsByRank"] = counts
        self._refresh()
        self._emit_change()

    def _add_mob(self) -> None:
        pool = self._current().setdefault("mobPool", [])
        pool.append({"roleId": "Goblin_Scrapper", "weight": 3})
        self._refresh()
        self._emit_change()

    def _remove_mob(self) -> None:
        rows = sorted({i.row() for i in self.pool_table.selectedIndexes()}, reverse=True)
        pool = self._current().get("mobPool") or []
        for row in rows:
            if 0 <= row < len(pool):
                pool.pop(row)
        self._current()["mobPool"] = pool
        self._refresh()
        self._emit_change()

    def _counts_changed(self, row: int, col: int) -> None:
        if self._loading:
            return
        counts = self._current().setdefault("mobCountsByRank", {})
        keys = list(counts.keys())
        if row >= len(keys):
            item = self.counts_table.item(row, col)
            text = item.text() if item else ""
            if col == 0 and text:
                counts[text] = 1
                self._refresh()
                self._emit_change()
            return
        old_key = keys[row]
        item = self.counts_table.item(row, col)
        text = item.text() if item else ""
        if col == 0:
            val = counts.pop(old_key, 1)
            counts[text] = val
        else:
            try:
                counts[old_key] = int(text)
            except ValueError:
                counts[old_key] = 1
        self._current()["mobCountsByRank"] = counts
        self._emit_change()

    def _pool_changed(self, row: int, col: int) -> None:
        if self._loading:
            return
        pool = self._current().setdefault("mobPool", [])
        if row >= len(pool):
            return
        item = self.pool_table.item(row, col)
        text = item.text() if item else ""
        if col == 0:
            pool[row]["roleId"] = text
        else:
            try:
                pool[row]["weight"] = int(text)
            except ValueError:
                pool[row]["weight"] = 1
        self._emit_change()

    def _emit_change(self) -> None:
        if self._on_change:
            self._on_change()
