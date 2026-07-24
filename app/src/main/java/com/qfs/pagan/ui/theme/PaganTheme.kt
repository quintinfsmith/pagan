package com.qfs.pagan.ui.theme


data class PaganTheme(
    val color_scheme: PaganColorScheme = PaganColorScheme(),
    val typography: PaganTypography = PaganTypography(),
    val dimensions: PaganDimensions = PaganDimensions(),
    val shapes: PaganShapes = PaganShapes()
)

object MasterTheme {
    var color_scheme: PaganColorScheme = PaganColorScheme()
    var typography: PaganTypography = PaganTypography()
    var dimensions = PaganDimensionsWrapper(PaganDimensions())
    var shapes: PaganShapes = PaganShapes()

    fun set(master_theme: PaganTheme) {
        this.color_scheme = master_theme.color_scheme
        this.typography = master_theme.typography
        this.dimensions = PaganDimensionsWrapper(master_theme.dimensions)
        this.shapes = master_theme.shapes
        Colors.active_color_scheme = this.color_scheme
    }
}