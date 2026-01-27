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

    val DialogTitle = TextStyle(fontFamily = Fonts.FiraSans)
    val DialogBody = TextStyle(fontFamily = Fonts.FiraSans)
    val DropdownMenu = TextStyle(fontFamily = Fonts.FiraSans)

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

    val NumberSelector = TextStyle(fontFamily = Fonts.FiraSans)
    val ProjectNotes = TextStyle(fontFamily = Fonts.FiraSans)

    object Settings {
        val Title = TextStyle(fontFamily = Fonts.FiraSans)
        val Label = TextStyle(fontFamily = Fonts.FiraSans)
    }


    val TopBar = TextStyle(fontFamily = Fonts.FiraSans)
}