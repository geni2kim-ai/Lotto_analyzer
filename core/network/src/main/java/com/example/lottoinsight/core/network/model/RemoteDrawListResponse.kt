package com.example.lottoinsight.core.network.model

import com.google.gson.annotations.SerializedName

data class RemoteDrawListResponse(
    @SerializedName("resultCode")
    val resultCode: String? = null,
    @SerializedName("resultMessage")
    val resultMessage: String? = null,
    @SerializedName("data")
    val data: RemoteDrawListData? = null
)

data class RemoteDrawListData(
    @SerializedName("list")
    val list: List<NewRemoteDrawDto>? = null
)

data class NewRemoteDrawDto(
    @SerializedName("ltEpsd")
    val ltEpsd: Int? = null,
    @SerializedName("tm1WnNo")
    val tm1WnNo: Int? = null,
    @SerializedName("tm2WnNo")
    val tm2WnNo: Int? = null,
    @SerializedName("tm3WnNo")
    val tm3WnNo: Int? = null,
    @SerializedName("tm4WnNo")
    val tm4WnNo: Int? = null,
    @SerializedName("tm5WnNo")
    val tm5WnNo: Int? = null,
    @SerializedName("tm6WnNo")
    val tm6WnNo: Int? = null,
    @SerializedName("bnsWnNo")
    val bnsWnNo: Int? = null,
    @SerializedName("ltRflYmd")
    val ltRflYmd: String? = null,
    @SerializedName("rnk1WnNope")
    val rnk1WnNope: Long? = null,
    @SerializedName("rnk1WnAmt")
    val rnk1WnAmt: Long? = null,
    @SerializedName("rnk1SumWnAmt")
    val rnk1SumWnAmt: Long? = null,
    @SerializedName("rnk2WnNope")
    val rnk2WnNope: Long? = null,
    @SerializedName("rnk2WnAmt")
    val rnk2WnAmt: Long? = null,
    @SerializedName("rnk3WnNope")
    val rnk3WnNope: Long? = null,
    @SerializedName("rnk3WnAmt")
    val rnk3WnAmt: Long? = null,
    @SerializedName("rlvtEpsdSumNtslAmt")
    val rlvtEpsdSumNtslAmt: Long? = null
)
