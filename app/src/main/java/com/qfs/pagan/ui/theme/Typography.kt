package com.qfs.pagan.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

object Typography {
    object About {
        val License = TextStyle(fontFamily = Fonts.FiraSans)
        val LinkTitle = TextStyle(
            fontFamily = Fonts.FiraSans,
            fontSize = 20.sp
        )
        val LinkUrl = TextStyle(
            fontFamily = Fonts.FiraMono,
            textDecoration = TextDecoration.Underline
        )
    }

    val Button = TextStyle(
        fontFamily = Fonts.FiraSans,
        letterSpacing = 1.sp,
        fontSize = 16.sp
    )

    val BeatLabel = TextStyle(
        fontFamily = Fonts.FiraSans,
        fontSize = 16.sp,
        lineHeight = 16.sp
    )
    val ContextMenuButton = TextStyle(
        fontFamily = Fonts.FiraMono,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 12.sp
    )

    val DialogTitle = TextStyle(fontFamily = Fonts.FiraSans)
    val DialogBody = TextStyle(fontFamily = Fonts.FiraSans)
    val DropdownMenu = TextStyle(fontFamily = Fonts.FiraSans)

    val LineLabel = TextStyle(
        fontFamily = Fonts.FiraSans,
        fontSize = 14.sp,
        lineHeight = 14.sp
    )

    object Leaf {
        val Octave = TextStyle(
            fontFamily = Fonts.FiraSans,
            fontSize = 14.sp
        )
        val Offset = TextStyle(
            fontFamily = Fonts.FiraSans,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }

    val NumberSelector = TextStyle(
        fontFamily = Fonts.FiraSans,
        fontSize = 18.sp
    )
    val ProjectNotes = TextStyle(fontFamily = Fonts.FiraSans)
    val RadioMenu = TextStyle(fontFamily = Fonts.FiraSans, fontSize = 22.sp, fontWeight = FontWeight.Bold)

    object Settings {
        val Title = TextStyle(fontFamily = Fonts.FiraSans)
        val Label = TextStyle(fontFamily = Fonts.FiraSans)
    }

    object SoundFontWarning {
        val Title = TextStyle(
            fontFamily = Fonts.FiraSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.W300
        )
        val Url = TextStyle(
            fontFamily = Fonts.FiraMono,
            lineHeight = 14.sp,
            fontSize = 12.sp
        )
        val Body = TextStyle(
            fontFamily = Fonts.FiraSans,
            lineHeight = 14.sp,
            fontSize = 12.sp
        )
    }

    val TopBar = TextStyle(fontFamily = Fonts.FiraSans)
    val TextField = TextStyle(
        fontFamily = Fonts.FiraMono,
        fontSize = 16.sp,
        lineHeight = 16.sp
    )

    val TinyTuningDialogLabel = TextStyle(
        fontFamily = Fonts.FiraSans,
        fontSize = 12.sp,
        lineHeight = 12.sp
    )
}