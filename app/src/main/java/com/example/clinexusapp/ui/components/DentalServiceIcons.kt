package com.example.clinexusapp.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Baby
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.ClipboardCheck
import com.composables.icons.lucide.Drill
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Siren
import com.composables.icons.lucide.Smile
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Stethoscope
import com.composables.icons.lucide.Sun

fun dentalServiceIcon(serviceName: String?): ImageVector {
    val name = serviceName.orEmpty().lowercase()

    return when {
        "xray" in name || "x-ray" in name || "radiograph" in name -> PeriapicalXrayIcon
        "extract" in name || "oral surgery" in name -> ToothExtractionIcon
        "clean" in name || "scaling" in name || "prophylaxis" in name -> Lucide.Sparkles
        "whiten" in name || "bleach" in name -> Lucide.Sun
        "root canal" in name || "endodont" in name -> Lucide.Activity
        "filling" in name || "restoration" in name -> Lucide.CirclePlus
        "denture" in name || "prosthodont" in name -> Lucide.Smile
        "implant" in name -> Lucide.Drill
        "pediatric" in name || "children" in name || "child" in name -> Lucide.Baby
        "emergency" in name || "urgent" in name -> Lucide.Siren
        "consult" in name || "checkup" in name || "examination" in name -> Lucide.ClipboardCheck
        else -> Lucide.Stethoscope
    }
}

/** Dental film containing a tooth root: specific to periapical radiography. */
private val PeriapicalXrayIcon: ImageVector by lazy {
    ImageVector.Builder("Periapical X-ray", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 3.5f); lineTo(19f, 3.5f)
            curveTo(19.8f, 3.5f, 20.5f, 4.2f, 20.5f, 5f)
            lineTo(20.5f, 19f); curveTo(20.5f, 19.8f, 19.8f, 20.5f, 19f, 20.5f)
            lineTo(5f, 20.5f); curveTo(4.2f, 20.5f, 3.5f, 19.8f, 3.5f, 19f)
            lineTo(3.5f, 5f); curveTo(3.5f, 4.2f, 4.2f, 3.5f, 5f, 3.5f)
            close()
            moveTo(8f, 7.2f)
            curveTo(9.2f, 6.2f, 10.6f, 6.8f, 12f, 6.8f)
            curveTo(13.4f, 6.8f, 14.8f, 6.2f, 16f, 7.2f)
            curveTo(17.4f, 8.5f, 16.3f, 11.1f, 15.4f, 12.2f)
            curveTo(14.5f, 13.4f, 14.5f, 17.4f, 13.1f, 17.4f)
            curveTo(12.3f, 17.4f, 12.6f, 14.1f, 12f, 14.1f)
            curveTo(11.4f, 14.1f, 11.7f, 17.4f, 10.9f, 17.4f)
            curveTo(9.5f, 17.4f, 9.5f, 13.4f, 8.6f, 12.2f)
            curveTo(7.7f, 11.1f, 6.6f, 8.5f, 8f, 7.2f)
        }
    }.build()
}

/** Tooth with an upward removal arrow: specific to tooth extraction. */
private val ToothExtractionIcon: ImageVector by lazy {
    ImageVector.Builder("Tooth extraction", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7.2f, 8.2f)
            curveTo(8.5f, 7f, 10.3f, 7.7f, 12f, 7.7f)
            curveTo(13.7f, 7.7f, 15.5f, 7f, 16.8f, 8.2f)
            curveTo(18.4f, 9.7f, 17.1f, 12.7f, 16.1f, 14f)
            curveTo(15f, 15.4f, 15f, 20f, 13.4f, 20f)
            curveTo(12.4f, 20f, 12.8f, 16.2f, 12f, 16.2f)
            curveTo(11.2f, 16.2f, 11.6f, 20f, 10.6f, 20f)
            curveTo(9f, 20f, 9f, 15.4f, 7.9f, 14f)
            curveTo(6.9f, 12.7f, 5.6f, 9.7f, 7.2f, 8.2f)
            moveTo(12f, 13f); lineTo(12f, 2.8f)
            moveTo(8.8f, 6f); lineTo(12f, 2.8f); lineTo(15.2f, 6f)
        }
    }.build()
}
