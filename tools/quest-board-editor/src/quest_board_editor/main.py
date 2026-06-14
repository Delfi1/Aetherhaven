from __future__ import annotations

import os
import sys
import traceback
from pathlib import Path


def _quiet_qt_image_warnings() -> None:
    rule = "qt.gui.imageio.warning=false"
    prev = os.environ.get("QT_LOGGING_RULES", "").strip()
    os.environ["QT_LOGGING_RULES"] = f"{prev};{rule}" if prev else rule


def main() -> int:
    _quiet_qt_image_warnings()
    try:
        from PySide6.QtWidgets import QApplication

        from .ui.main_window import MainWindow
    except ImportError as e:
        sys.stderr.write(
            "quest_board_editor: missing dependency (install from tools/quest-board-editor):\n"
            "  pip install -r requirements.txt\n"
            f"ImportError: {e}\n"
        )
        return 1

    try:
        app = QApplication(sys.argv)
        app.setApplicationName("Aetherhaven Quest Board Editor")
        w = MainWindow()
        w.show()
        return int(app.exec())
    except Exception:
        sys.stderr.write("quest_board_editor: failed during startup:\n")
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    root = Path(__file__).resolve().parent.parent
    if str(root) not in sys.path:
        sys.path.insert(0, str(root))
    raise SystemExit(main())
