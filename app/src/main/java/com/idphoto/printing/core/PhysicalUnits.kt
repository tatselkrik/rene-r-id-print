package com.idphoto.printing.core

object PhysicalUnits {
    const val POINTS_PER_INCH = 72f
    const val MM_PER_INCH = 25.4f

    fun mmToPoints(value: Float): Float = value * POINTS_PER_INCH / MM_PER_INCH
}

