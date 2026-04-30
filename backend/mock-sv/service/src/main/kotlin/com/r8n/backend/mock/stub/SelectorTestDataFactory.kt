package com.r8n.backend.mock.stub

import com.r8n.backend.mock.api.dto.about.SelectorDto
import java.util.UUID

object SelectorTestDataFactory {
    fun getSelector() =
        SelectorDto(
            UUID.randomUUID(),
            UUID.fromString("14141414-1414-1414-1414-141414141414"),
            "maps.google.com/*",
            "maps.google.com",
            """
            const labels = [...document.querySelectorAll('span')]
                .filter(s => s.textContent.trim() === name);

            labels.forEach(label => {
                let card = label.closest('div');
                while (card && card !== document.body && card.querySelectorAll('img').length === 0) {
                  card = card.parentElement;
            }
            if (!card || card === document.body) return;

            const img = card.querySelector('img');
            if (!img) return;
            return img;
            """.trimIndent(),
        )
}
