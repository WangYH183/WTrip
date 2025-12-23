package com.baguetteui.btrip.db

data class MainTypeSumRow(
    val mainType: String,
    val totalAmountCny: Int?
)

data class PaymentMethodSumRow(
    val paymentMethod: String,
    val totalAmountCny: Int?
)