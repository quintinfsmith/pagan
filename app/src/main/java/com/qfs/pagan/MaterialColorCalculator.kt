/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import androidx.compose.ui.graphics.Color

class MaterialColorCalculator {
    companion object {
        fun get(input: Color): Array<Color> {
            val base_ratio = 1F / listOf(input.red, input.green, input.blue).max()

            val base = Color(
                red = input.red * base_ratio,
                green = input.green * base_ratio,
                blue = input.blue * base_ratio,
            )

            return arrayOf(
                // 0
                Color(0xFF000000),
                // 10
                Color(
                    red = base.red * .2F,
                    green = base.green * .2F,
                    blue = base.blue * .2F
                ),
                // 20
                Color(
                    red = base.red * .3F,
                    green = base.green * .3F,
                    blue = base.blue * .3F
                ),
                // 30
                Color(
                    red = base.red * .4F,
                    green = base.green * .4F,
                    blue = base.blue * .4F
                ),
                // 40
                Color(
                    red = base.red * .5F,
                    green = base.green * .5F,
                    blue = base.blue * .5F
                ),
                // 50
                Color(
                    red = base.red * .6F,
                    green = base.green * .6F,
                    blue = base.blue * .6F
                ),
                // 60
                Color(
                    red = .26F + (base.red * .44F),
                    green = .26F + (base.green * .44F),
                    blue = .26F + (base.blue * .44F)
                ),
                // 70
                Color(
                    red = .36F + (base.red * .44F),
                    green = .36F + (base.green * .44F),
                    blue = .36F + (base.blue * .44F)
                ),
                // 80
                Color(
                    red = .49F + (base.red * .41F),
                    green = .49F + (base.green * .41F),
                    blue = .49F + (base.blue * .41F)
                ),
                // 90
                Color(
                    red = .62F + (base.red * .28F),
                    green = .62F + (base.green * .28F),
                    blue = .62F + (base.blue * .28F)
                ),
                // 95
                Color(
                    red = .74F + (base.red * .21F),
                    green = .74F + (base.green * .21F),
                    blue = .74F + (base.blue * .21F)
                ),
                // 99
                Color(
                    red = .89F + (base.red * .1F),
                    green = .89F + (base.green * .1F),
                    blue = .89F + (base.blue * .1F)
                ),
                Color(0xFFFFFFFF),
            )
        }
    }
}