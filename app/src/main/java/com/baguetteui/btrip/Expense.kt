package com.baguetteui.btrip

import java.io.Serializable

data class Expense(
    val id: Long,
    val tripId: Long,
    val title: String,
    val amountCny: Int,
    val mainType: MainType,
    val paymentMethod: PaymentMethod,
    val reservationPlatform: String? = null,
    val shortReview: String? = null,
    val photoUri: String? = null,
    val eventTimeMillis: Long? = null,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val subType: String? = null,
    val route: String? = null,
    val durationHHmm: String? = null,
    val dateEpochDay: Long
) : Serializable

enum class MainType(val displayName: String) {
    TRANSPORT("交通"),
    FOOD("饮食"),
    HOTEL("住宿"),
    EXPERIENCE("体验")
}

enum class PaymentMethod(val displayName: String) {
    WECHAT("微信"),
    ALIPAY("支付宝"),
    VISA("VISA"),
    CASH("现金"),
    OTHER("其他")
}