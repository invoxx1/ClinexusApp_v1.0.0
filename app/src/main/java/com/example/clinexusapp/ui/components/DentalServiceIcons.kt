package com.example.clinexusapp.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Baby
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.ClipboardCheck
import com.composables.icons.lucide.Drill
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ScanSearch
import com.composables.icons.lucide.Scissors
import com.composables.icons.lucide.Siren
import com.composables.icons.lucide.Smile
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Stethoscope
import com.composables.icons.lucide.Sun

fun dentalServiceIcon(serviceName: String?): ImageVector {
    val name = serviceName.orEmpty().lowercase()

    return when {
        "xray" in name || "x-ray" in name || "radiograph" in name -> Lucide.ScanSearch
        "extract" in name || "oral surgery" in name -> Lucide.Scissors
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
