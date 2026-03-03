package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.repository.DefaultFileClipboardManager
import com.jonecx.ibex.data.repository.FileMoveManager

class FakeFileClipboardManager(
    fileMoveManager: FileMoveManager = FakeFileMoveManager(),
) : DefaultFileClipboardManager(fileMoveManager)
