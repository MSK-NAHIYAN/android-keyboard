package org.futo.inputmethod.latin.uix.actions

import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.OllamaClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AiFixAction : ToolbarAction(
    id = "ai_fix",
    titleRes = R.string.action_ai_grammar_fix,
    iconRes = R.drawable.ic_ai_fix
) {
    override fun onClick(latinIME: LatinIME) {
        val ic = latinIME.currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        
        if (textBefore.isBlank()) return

        CoroutineScope(Dispatchers.Main).launch {
            val result = OllamaClient.fixGrammar(textBefore)
            result.onSuccess { corrected ->
                ic.beginBatchEdit()
                ic.deleteSurroundingText(textBefore.length, 0)
                ic.commitText(corrected, 1)
                ic.endBatchEdit()
            }
        }
    }
}
