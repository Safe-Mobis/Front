package com.bs.inavitest.DataClass

import com.bs.inavitest.Response.Intersection
import com.inavi.mapsdk.style.shapes.InvMarker

data class IntersectionsInfo(
    val intersection: Intersection,
    var warningCode: String,
    val marker: InvMarker
)
