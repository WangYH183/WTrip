package com.baguetteui.btrip.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["tripId", "dateEpochDay"]),
        Index(value = ["tripId", "mainType"]),
        Index(value = ["tripId", "paymentMethod"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tripId: Long,

    /** 详情页标题：例如“春秋航空”“pho粉”“达美乐披萨” */
    val title: String,

    /** 花费（人民币，元） */
    val amountCny: Int,

    /** 四大类：TRANSPORT/FOOD/HOTEL/EXPERIENCE */
    val mainType: String,

    /** 支付方式：WECHAT/ALIPAY/VISA/CASH/OTHER */
    val paymentMethod: String,

    /** 可选：预定平台 */
    val reservationPlatform: String? = null,

    /** 可选：简评（UI限制20字） */
    val shortReview: String? = null,

    /** 可选：单图Uri（String） */
    val photoUri: String? = null,

    /** 饮食/住宿/体验：单个时间点（epoch millis） */
    val eventTimeMillis: Long? = null,

    /** 交通：起止时间（epoch millis） */
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,

    /** 子类别：交通/饮食/体验 等（字符串） */
    val subType: String? = null,

    /** 交通：出发地-到达地 */
    val route: String? = null,

    /** 交通：时长（手动，HH:mm，可空） */
    val durationHHmm: String? = null,

    /** 保留旧字段：按天 */
    val dateEpochDay: Long
)