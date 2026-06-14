from __future__ import annotations

from typing import Callable, List, Optional

from PySide6.QtCore import QAbstractTableModel, QModelIndex, Qt
from PySide6.QtWidgets import QHeaderView, QTableView


class QuestListModel(QAbstractTableModel):
    HEADERS = ["Rank", "Type", "Villager", "ID", "Title"]

    def __init__(self) -> None:
        super().__init__()
        self._rows: List[tuple] = []

    def rowCount(self, parent: QModelIndex = QModelIndex()) -> int:  # type: ignore[override]
        if parent.isValid():
            return 0
        return len(self._rows)

    def columnCount(self, parent: QModelIndex = QModelIndex()) -> int:  # type: ignore[override]
        if parent.isValid():
            return 0
        return len(self.HEADERS)

    def data(self, index: QModelIndex, role: int = Qt.ItemDataRole.DisplayRole):  # type: ignore[override]
        if not index.isValid() or index.row() >= len(self._rows):
            return None
        row = self._rows[index.row()]
        if role in (Qt.ItemDataRole.DisplayRole, Qt.ItemDataRole.ToolTipRole):
            return row[index.column()]
        return None

    def headerData(self, section: int, orientation: Qt.Orientation, role: int = Qt.ItemDataRole.DisplayRole):  # type: ignore[override]
        if role != Qt.ItemDataRole.DisplayRole or orientation != Qt.Orientation.Horizontal:
            return None
        if 0 <= section < len(self.HEADERS):
            return self.HEADERS[section]
        return None

    def set_rows(self, rows: List[tuple]) -> None:
        self.beginResetModel()
        self._rows = rows
        self.endResetModel()

    def ref_index_at(self, row: int) -> int:
        if 0 <= row < len(self._rows):
            return int(self._rows[row][-1])
        return -1


class QuestListView(QTableView):
    def __init__(self) -> None:
        super().__init__()
        self._model = QuestListModel()
        self.setModel(self._model)
        self.setSelectionBehavior(QTableView.SelectionBehavior.SelectRows)
        self.setSelectionMode(QTableView.SelectionMode.SingleSelection)
        self.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Interactive)
        self.horizontalHeader().setStretchLastSection(True)
        self.verticalHeader().setVisible(False)
        self.setAlternatingRowColors(True)
        self.setColumnWidth(0, 50)
        self.setColumnWidth(1, 55)
        self.setColumnWidth(2, 110)
        self.setColumnWidth(3, 140)

    def quest_model(self) -> QuestListModel:
        return self._model

    def selected_ref_index(self) -> int:
        indexes = self.selectionModel().selectedRows()
        if not indexes:
            return -1
        return self._model.ref_index_at(indexes[0].row())
