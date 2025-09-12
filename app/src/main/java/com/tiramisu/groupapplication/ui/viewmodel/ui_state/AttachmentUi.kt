package com.tiramisu.groupapplication.ui.viewmodel.ui_state

import androidx.core.net.toUri
import com.tiramisu.groupapplication.domain.model.InspectionAttachment
import com.tiramisu.groupapplication.util.AttachmentLinkResolver

data class AttachmentUi(
    val id: String,
    val coilModel: Any, // Uri for local, String URL for remote
    val raw: InspectionAttachment
)

fun toUiList(
    list: List<InspectionAttachment>,
    resolver: AttachmentLinkResolver
): List<AttachmentUi> =
    list.map { a ->
        val model: Any = when {
            a.remoteKey != null -> resolver.publicUrl(a.remoteKey)
            a.localUri != null -> a.localUri.toUri()          // works offline
            else -> "" // or some placeholder key; your AsyncImage can handle empty
        }
        AttachmentUi(id = a.id, coilModel = model, raw = a)
    }

fun toUi(att: InspectionAttachment, resolver: AttachmentLinkResolver): AttachmentUi {
    val model: Any = when {
        att.remoteKey != null -> resolver.publicUrl(att.remoteKey)
        att.localUri != null -> att.localUri.toUri()          // works offline
        else -> "" // or some placeholder key; your AsyncImage can handle empty
    }
    return AttachmentUi(id = att.id, coilModel = model, raw = att)
}