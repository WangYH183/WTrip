package com.baguetteui.wtrip.db

data class MainTypeSumRow(
    val mainType: String,
    val totalAmountCny: Int?
)

data class PaymentMethodSumRow(
    val paymentMethod: String,
    val totalAmountCny: Int?
)