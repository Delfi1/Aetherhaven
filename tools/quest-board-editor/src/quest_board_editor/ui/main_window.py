from __future__ import annotations

import copy
from pathlib import Path
from typing import Dict, List, Optional

from PySide6.QtCore import Qt
from PySide6.QtGui import QAction, QKeySequence
from PySide6.QtWidgets import (
    QComboBox,
    QDialog,
    QDialogButtonBox,
    QFileDialog,
    QFormLayout,
    QGroupBox,
    QHBoxLayout,
    QInputDialog,
    QLabel,
    QLineEdit,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QSpinBox,
    QSplitter,
    QVBoxLayout,
    QWidget,
)

from ..config import AppConfig
from ..io_json import load_quest_board, save_quest_board
from ..io_lang import LangDocument, load_lang, save_lang
from ..lang_keys import json_key_to_lang_key
from ..quest_model import (
    QuestBoardDocument,
    QuestFilter,
    QuestRef,
    filter_quests,
    make_template,
    quest_description_lang_key,
    quest_title_lang_key,
    resolve_title,
    sync_lang_from_quests,
    validate_document,
    villager_short_label,
)
from ..villager_catalog import merged_villager_ids
from .quest_form import QuestForm
from .quest_list import QuestListView


class BoardSettingsDialog(QDialog):
    def __init__(self, doc: QuestBoardDocument, parent: Optional[QWidget] = None) -> None:
        super().__init__(parent)
        self.setWindowTitle("Board settings")
        self._doc = doc
        layout = QVBoxLayout(self)

        form = QFormLayout()
        self.slot_spin = QSpinBox()
        self.slot_spin.setRange(1, 12)
        self.slot_spin.setValue(int(doc.data.get("slotCount", 3)))
        form.addRow("Slot count", self.slot_spin)

        qt = doc.data.get("questTypes") or {}
        self.fetch_weight = QSpinBox()
        self.fetch_weight.setRange(0, 9999)
        self.fetch_weight.setValue(int((qt.get("fetch") or {}).get("weight", 65)))
        form.addRow("Fetch type weight", self.fetch_weight)
        self.hunt_weight = QSpinBox()
        self.hunt_weight.setRange(0, 9999)
        self.hunt_weight.setValue(int((qt.get("hunt") or {}).get("weight", 45)))
        form.addRow("Hunt type weight", self.hunt_weight)
        self.raid_weight = QSpinBox()
        self.raid_weight.setRange(0, 9999)
        self.raid_weight.setValue(int((qt.get("raid") or {}).get("weight", 20)))
        form.addRow("Raid type weight", self.raid_weight)
        layout.addLayout(form)

        buttons = QDialogButtonBox(
            QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel
        )
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def apply(self) -> None:
        self._doc.data["slotCount"] = self.slot_spin.value()
        self._doc.data["questTypes"] = {
            "fetch": {"weight": self.fetch_weight.value()},
            "hunt": {"weight": self.hunt_weight.value()},
            "raid": {"weight": self.raid_weight.value()},
        }


class MainWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self._config = AppConfig.load()
        self._json_path: Optional[Path] = None
        self._lang_path: Optional[Path] = None
        self._doc: Optional[QuestBoardDocument] = None
        self._lang_doc: Optional[LangDocument] = None
        self._all_refs: List[QuestRef] = []
        self._filtered_indices: List[int] = []
        self._all_villager_ids: List[str] = []
        self._dirty = False
        self._pending_lang: Dict[str, str] = {}
        self._removed_lang_json_keys: set[str] = set()

        self.setWindowTitle("Aetherhaven Quest Board Editor")
        self.resize(1200, 780)
        self._build_menu()
        self._build_ui()

        qb = self._config.resolved_quest_board_path()
        lg = self._config.resolved_lang_path()
        if qb.is_file():
            self._load_files(qb, lg)
        else:
            QMessageBox.warning(
                self,
                "Quest board not found",
                f"Could not find quest_board.json at:\n{qb}",
            )

    def _build_menu(self) -> None:
        file_menu = self.menuBar().addMenu("&File")
        open_act = QAction("&Open JSON...", self)
        open_act.setShortcut(QKeySequence.StandardKey.Open)
        open_act.triggered.connect(self._open_json)
        file_menu.addAction(open_act)
        save_act = QAction("&Save", self)
        save_act.setShortcut(QKeySequence.StandardKey.Save)
        save_act.triggered.connect(self._save)
        file_menu.addAction(save_act)
        save_as_act = QAction("Save &As...", self)
        save_as_act.setShortcut(QKeySequence.StandardKey.SaveAs)
        save_as_act.triggered.connect(self._save_as)
        file_menu.addAction(save_as_act)
        file_menu.addSeparator()
        reload_act = QAction("&Reload", self)
        reload_act.triggered.connect(self._reload)
        file_menu.addAction(reload_act)
        file_menu.addSeparator()
        settings_act = QAction("Board &settings...", self)
        settings_act.triggered.connect(self._board_settings)
        file_menu.addAction(settings_act)

    def _build_ui(self) -> None:
        central = QWidget()
        self.setCentralWidget(central)
        root = QHBoxLayout(central)
        splitter = QSplitter(Qt.Orientation.Horizontal)
        root.addWidget(splitter)

        left = QWidget()
        left_layout = QVBoxLayout(left)

        filt_box = QGroupBox("Filters")
        filt_form = QFormLayout(filt_box)
        self.villager_filter = QComboBox()
        self.villager_filter.currentIndexChanged.connect(self._refresh_list)
        filt_form.addRow("Villager", self.villager_filter)
        self.type_filter = QComboBox()
        self.type_filter.addItems(["All", "fetch", "hunt", "raid"])
        self.type_filter.currentIndexChanged.connect(self._refresh_list)
        filt_form.addRow("Quest type", self.type_filter)
        self.rank_filter = QComboBox()
        self.rank_filter.currentIndexChanged.connect(self._refresh_list)
        filt_form.addRow("Rank", self.rank_filter)
        self.search_edit = QLineEdit()
        self.search_edit.setPlaceholderText("Search id or title...")
        self.search_edit.textChanged.connect(self._refresh_list)
        filt_form.addRow("Search", self.search_edit)
        left_layout.addWidget(filt_box)

        self.count_label = QLabel("")
        left_layout.addWidget(self.count_label)

        self.quest_list = QuestListView()
        self.quest_list.selectionModel().selectionChanged.connect(self._on_selection_changed)
        left_layout.addWidget(self.quest_list)

        btn_row = QHBoxLayout()
        new_btn = QPushButton("New quest")
        new_btn.clicked.connect(self._new_quest)
        dup_btn = QPushButton("Duplicate")
        dup_btn.clicked.connect(self._duplicate_quest)
        del_btn = QPushButton("Delete")
        del_btn.clicked.connect(self._delete_quest)
        btn_row.addWidget(new_btn)
        btn_row.addWidget(dup_btn)
        btn_row.addWidget(del_btn)
        left_layout.addLayout(btn_row)

        splitter.addWidget(left)

        self.quest_form = QuestForm(
            ranks=[],
            villager_ids=[],
            lang_getter=self._lang_text,
            on_dirty=self._mark_dirty,
        )
        splitter.addWidget(self.quest_form)
        splitter.setStretchFactor(0, 2)
        splitter.setStretchFactor(1, 3)

    def _lang_text(self, lang_key: str) -> str:
        if self._lang_doc is None:
            return ""
        pending_json = {json_key_to_lang_key(k): v for k, v in self._pending_lang.items()}
        if lang_key in pending_json:
            return pending_json[lang_key]
        return self._lang_doc.get(lang_key, "")

    def _lang_getter_for_filter(self, lang_key: str, default: str = "") -> str:
        return self._lang_text(lang_key) or default

    def _load_files(self, json_path: Path, lang_path: Path) -> None:
        try:
            data = load_quest_board(json_path)
            lang_doc = load_lang(lang_path) if lang_path.is_file() else LangDocument()
        except Exception as e:
            QMessageBox.critical(self, "Load failed", str(e))
            return

        self._json_path = json_path
        self._lang_path = lang_path
        self._doc = QuestBoardDocument(data)
        self._lang_doc = lang_doc
        self._pending_lang.clear()
        self._removed_lang_json_keys.clear()
        self._dirty = False
        self._rebuild_index()
        self._update_filters()
        self._refresh_list()
        self._update_title()

        self._config.quest_board_path = str(json_path)
        self._config.lang_path = str(lang_path)
        self._config.save()

    def _rebuild_index(self) -> None:
        if self._doc is None:
            self._all_refs = []
            return
        self._all_refs = self._doc.flatten()

    def _update_filters(self) -> None:
        if self._doc is None:
            return
        self._all_villager_ids = merged_villager_ids(self._doc.villager_ids())
        self.villager_filter.blockSignals(True)
        self.rank_filter.blockSignals(True)
        cur_villager = self.villager_filter.currentText()
        self.villager_filter.clear()
        self.villager_filter.addItem("All")
        for vid in self._all_villager_ids:
            self.villager_filter.addItem(vid)
        if cur_villager and self.villager_filter.findText(cur_villager) >= 0:
            self.villager_filter.setCurrentText(cur_villager)
        self.rank_filter.clear()
        self.rank_filter.addItem("All")
        for r in self._doc.ranks:
            self.rank_filter.addItem(r)
        self.villager_filter.blockSignals(False)
        self.rank_filter.blockSignals(False)
        self.quest_form.set_ranks(self._doc.ranks)
        self.quest_form.set_villagers(self._all_villager_ids)

    def _current_filter(self) -> QuestFilter:
        vf = self.villager_filter.currentText()
        tf = self.type_filter.currentText()
        rf = self.rank_filter.currentText()
        return QuestFilter(
            villager_id=None if vf == "All" else vf,
            quest_type=None if tf == "All" else tf,
            rank=None if rf == "All" else rf,
            search=self.search_edit.text(),
        )

    def _refresh_list(self) -> None:
        if self._doc is None:
            return
        filt = self._current_filter()
        filtered = filter_quests(self._all_refs, filt, self._lang_getter_for_filter)
        self._filtered_indices = []
        rows = []
        for ref in filtered:
            try:
                idx = self._all_refs.index(ref)
            except ValueError:
                continue
            self._filtered_indices.append(idx)
            title = resolve_title(self._lang_getter_for_filter, ref)
            rows.append(
                (
                    ref.rank,
                    ref.quest_type,
                    villager_short_label(ref.villager_id),
                    ref.quest_id,
                    title,
                    idx,
                )
            )
        self.quest_list.quest_model().set_rows(rows)
        total = len(self._all_refs)
        self.count_label.setText(f"{len(rows)} / {total} quests")

    def _on_selection_changed(self) -> None:
        idx = self.quest_list.selected_ref_index()
        if idx < 0 or idx >= len(self._all_refs):
            self.quest_form.load_quest(None)
            return
        self.quest_form.load_quest(self._all_refs[idx])

    def _selected_ref(self) -> Optional[QuestRef]:
        idx = self.quest_list.selected_ref_index()
        if idx < 0 or idx >= len(self._all_refs):
            return None
        return self._all_refs[idx]

    def _mark_dirty(self) -> None:
        self._dirty = True
        self._update_title()
        pending = self.quest_form.pending_lang_texts()
        self._pending_lang.update(pending)
        for stale in self.quest_form.consume_stale_lang_keys():
            self._pending_lang.pop(stale, None)
            self._removed_lang_json_keys.add(stale)

    def _update_title(self) -> None:
        name = self._json_path.name if self._json_path else "untitled"
        star = " *" if self._dirty else ""
        self.setWindowTitle(f"{name}{star} — Aetherhaven Quest Board Editor")

    def _apply_form_to_selected(self) -> None:
        ref = self._selected_ref()
        if ref is None or self._doc is None:
            self.quest_form.apply()
            self._pending_lang.update(self.quest_form.pending_lang_texts())
            return
        old_villager = ref.villager_id
        old_type = ref.quest_type
        self.quest_form.apply()
        self._pending_lang.update(self.quest_form.pending_lang_texts())
        new_villager = self.quest_form.current_villager()
        new_type = self.quest_form.current_quest_type()
        if new_villager != old_villager or new_type != old_type:
            moved = self._doc.move_quest(ref, new_villager, new_type)
            self._rebuild_index()
            idx = self._all_refs.index(moved)
            self._refresh_list()
            self._select_ref_index(idx)

    def _save(self) -> bool:
        if self._doc is None or self._json_path is None or self._lang_doc is None:
            return False
        self._apply_form_to_selected()

        lang_getter = lambda k, d="": self._lang_doc.get(k, d) if self._lang_doc else d  # noqa: E731
        for json_key in self._removed_lang_json_keys:
            self._lang_doc.remove(json_key_to_lang_key(json_key))
        for json_key, text in self._pending_lang.items():
            self._lang_doc.set(json_key_to_lang_key(json_key), text)
        sync_lang_from_quests(self._doc, self._lang_doc, self._pending_lang)

        errors = validate_document(self._doc, lang_getter)
        if errors:
            QMessageBox.warning(
                self,
                "Validation failed",
                "Fix these issues before saving:\n\n" + "\n".join(errors[:20]),
            )
            return False

        try:
            save_quest_board(self._json_path, self._doc.data)
            if self._lang_path:
                save_lang(self._lang_path, self._lang_doc)
        except Exception as e:
            QMessageBox.critical(self, "Save failed", str(e))
            return False

        self._dirty = False
        self._pending_lang.clear()
        self._removed_lang_json_keys.clear()
        self._update_title()
        self._refresh_list()
        return True

    def _save_as(self) -> None:
        path, _ = QFileDialog.getSaveFileName(
            self,
            "Save quest board JSON",
            str(self._json_path or ""),
            "JSON (*.json)",
        )
        if not path:
            return
        self._json_path = Path(path)
        self._save()

    def _open_json(self) -> None:
        if not self._confirm_discard():
            return
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Open quest board JSON",
            str(self._config.resolved_quest_board_path().parent),
            "JSON (*.json)",
        )
        if not path:
            return
        json_path = Path(path)
        lang_path = self._config.resolved_lang_path()
        self._load_files(json_path, lang_path)

    def _reload(self) -> None:
        if self._json_path is None or self._lang_path is None:
            return
        if not self._confirm_discard():
            return
        self._load_files(self._json_path, self._lang_path)

    def _confirm_discard(self) -> bool:
        if not self._dirty:
            return True
        ans = QMessageBox.question(
            self,
            "Unsaved changes",
            "Discard unsaved changes?",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        return ans == QMessageBox.StandardButton.Yes

    def _board_settings(self) -> None:
        if self._doc is None:
            return
        dlg = BoardSettingsDialog(self._doc, self)
        if dlg.exec() == QDialog.DialogCode.Accepted:
            dlg.apply()
            self._mark_dirty()

    def _new_quest(self) -> None:
        if self._doc is None:
            return
        vf = self.villager_filter.currentText()
        tf = self.type_filter.currentText()
        villager_id = vf if vf != "All" else (
            self._all_villager_ids[0] if self._all_villager_ids else "Aetherhaven_Miner"
        )
        quest_type = tf if tf != "All" else "fetch"

        qid, ok = QInputDialog.getText(self, "New quest", "Quest id:", text="new_quest")
        if not ok or not qid.strip():
            return
        qid = qid.strip()

        entry = make_template(villager_id, quest_type, qid)
        ref = self._doc.insert_quest(villager_id, quest_type, entry)
        self._rebuild_index()

        title_key = entry["titleLangKey"]
        desc_key = entry["descriptionLangKey"]
        self._pending_lang[title_key] = "New quest title"
        self._pending_lang[desc_key] = "New quest description."
        if quest_type in ("hunt", "raid"):
            tk = entry.get("targetLabelLangKey")
            if isinstance(tk, str):
                self._pending_lang[tk] = "targets"

        self._mark_dirty()
        self._update_filters()
        self._refresh_list()
        idx = self._all_refs.index(ref)
        self._select_ref_index(idx)

    def _duplicate_quest(self) -> None:
        ref = self._selected_ref()
        if ref is None or self._doc is None:
            return
        self._apply_form_to_selected()
        qid, ok = QInputDialog.getText(
            self, "Duplicate quest", "New quest id:", text=f"{ref.quest_id}_copy"
        )
        if not ok or not qid.strip():
            return
        entry = copy.deepcopy(ref.entry)
        entry["id"] = qid.strip()
        entry["titleLangKey"] = quest_title_lang_key(ref.villager_id, qid.strip())
        entry["descriptionLangKey"] = quest_description_lang_key(ref.villager_id, qid.strip())
        new_ref = self._doc.insert_quest(ref.villager_id, ref.quest_type, entry)
        title_key = str(entry.get("titleLangKey", ""))
        desc_key = str(entry.get("descriptionLangKey", ""))
        old_title_key = str(ref.entry.get("titleLangKey", ""))
        old_desc_key = str(ref.entry.get("descriptionLangKey", ""))
        self._pending_lang[title_key] = self._lang_text(json_key_to_lang_key(old_title_key)) + " (copy)"
        self._pending_lang[desc_key] = self._lang_text(json_key_to_lang_key(old_desc_key))
        target_key = entry.get("targetLabelLangKey")
        old_target_key = ref.entry.get("targetLabelLangKey")
        if isinstance(target_key, str) and isinstance(old_target_key, str):
            self._pending_lang[target_key] = self._lang_text(json_key_to_lang_key(old_target_key))
        self._rebuild_index()
        self._mark_dirty()
        self._refresh_list()
        self._select_ref_index(self._all_refs.index(new_ref))

    def _delete_quest(self) -> None:
        ref = self._selected_ref()
        if ref is None or self._doc is None:
            return
        ans = QMessageBox.question(
            self,
            "Delete quest",
            f"Delete {ref.quest_id} ({ref.quest_type}) from {ref.villager_id}?",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if ans != QMessageBox.StandardButton.Yes:
            return
        self._doc.remove_quest(ref)
        self._rebuild_index()
        self._mark_dirty()
        self._refresh_list()
        self.quest_form.load_quest(None)

    def _select_ref_index(self, idx: int) -> None:
        for row, ref_idx in enumerate(self._filtered_indices):
            if ref_idx == idx:
                self.quest_list.selectRow(row)
                return
        self._refresh_list()
        for row, ref_idx in enumerate(self._filtered_indices):
            if ref_idx == idx:
                self.quest_list.selectRow(row)
                return

    def closeEvent(self, event) -> None:  # type: ignore[override]
        if self._dirty:
            ans = QMessageBox.question(
                self,
                "Unsaved changes",
                "Save before closing?",
                QMessageBox.StandardButton.Save
                | QMessageBox.StandardButton.Discard
                | QMessageBox.StandardButton.Cancel,
            )
            if ans == QMessageBox.StandardButton.Save:
                if not self._save():
                    event.ignore()
                    return
            elif ans == QMessageBox.StandardButton.Cancel:
                event.ignore()
                return
        event.accept()
