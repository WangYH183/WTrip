package com.baguetteui.btrip.db

import androidx.room.ColumnInfo

data class TripSummaryRow(
    @ColumnInfo(name = "tripId") val tripId: Long,
    @ColumnInfo(name = "itemCount") val itemCount: Int,
    @ColumnInfo(name = "totalAmount") val totalAmount: Int?
)