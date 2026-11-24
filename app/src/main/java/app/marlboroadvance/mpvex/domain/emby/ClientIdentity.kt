package app.marlboroadvance.mpvex.domain.emby

import android.os.Build

// ════════════════════════════════════════════════════════════════════════
// 客户端标识（对外自报的「我是谁」）
// ════════════════════════════════════════════════════════════════════════
//
// Emby 服务端并不校验客户端真伪，它只把客户端「自报」的这几个字段记进会话：
// `Client` / `Device` / `DeviceId` / `Version`，以及请求的 `User-Agent`。
// 有些服务端/反代会拿这几个字段做访问策略（客户端白名单、按端限速、
// 转码策略），所以这些值必须集中一处维护，不能散落在各个调用点。
//
// 这里的取值逐字段对齐 Hills 1.11.0 在真机上发出的登录请求：
//   POST /emby/Users/AuthenticateByName
//     ?X-Emby-Authorization=Emby Client="Hills", Device="PJX110", DeviceId="<16位hex>", Version="1.11.0"
//     &X-Emby-Client=Hills&X-Emby-Device-Name=PJX110&X-Emby-Device-Id=<16位hex>
//     &X-Emby-Client-Version=1.11.0&X-Emby-Language=zh-cn
//   x-emby-authorization: Emby Client="Hills", Device="PJX110", DeviceId="<16位hex>", Version="1.11.0"
//   user-agent: Hills/1.11.0 (android; 16)
//   body: {"Username":"…","Password":"…","Pw":"…"}
//
// 注意前缀是 `Emby Client=`（不是 `MediaBrowser Client=`），设备名是机器型号
// （Hills 取 `Build.MODEL`），UA 末尾是 Android 主版本号。
internal object ClientIdentity {

  /** 上报的客户端名。服务端会话列表里显示的就是它。 */
  const val CLIENT_NAME = "Hills"

  /** 上报的客户端版本（与标识里 Client-Version / Version 同源）。 */
  const val CLIENT_VERSION = "1.11.0"

  /** 上报的语言。 */
  const val LANGUAGE = "zh-cn"

  /** 设备名：Hills 用机器型号（真机上是 `PJX110` 这种），取不到时退回 `Android`。 */
  val deviceName: String
    get() = Build.MODEL?.trim()?.takeIf { it.isNotEmpty() } ?: "Android"

  /** UA：`Hills/1.11.0 (android; 16)`，末尾是 Android 版本主号。 */
  val userAgent: String
    get() = "$CLIENT_NAME/$CLIENT_VERSION (android; ${androidMajorVersion()})"

  /** Android 主版本号（`Build.VERSION.RELEASE` 是 `16` / `15` / `14` 这种，少数是 `8.1.0`）。 */
  private fun androidMajorVersion(): String =
    Build.VERSION.RELEASE?.substringBefore('.')?.takeIf { it.isNotEmpty() } ?: "0"

  /**
   * `X-Emby-Authorization` 的值：`Emby Client="Hills", Device="…", DeviceId="…", Version="1.11.0"`。
   *
   * 登录时既放 header，也放 query（服务端两种都认，Hills 两个都发）。
   * 默认前缀是 `Emby`（真机实测值）；个别只认老格式的服务端要 `MediaBrowser`，
   * 由 [authorizationHeaderWithScheme] 指定。
   */
  fun authorizationHeader(deviceId: String): String = authorizationHeaderWithScheme("Emby", deviceId)

  /** 指定 scheme 前缀的标识串（`Emby` / `MediaBrowser`）。 */
  fun authorizationHeaderWithScheme(scheme: String, deviceId: String): String =
    "$scheme Client=\"$CLIENT_NAME\", Device=\"$deviceName\", DeviceId=\"$deviceId\", Version=\"$CLIENT_VERSION\""

  /**
   * 登录请求附带的客户端标识 query 参数（Hills 的真实形态：6 个参数全在 URL 上）。
   */
  fun loginQuery(deviceId: String): Map<String, String> = mapOf(
    "X-Emby-Authorization" to authorizationHeader(deviceId),
    "X-Emby-Client" to CLIENT_NAME,
    "X-Emby-Device-Name" to deviceName,
    "X-Emby-Device-Id" to deviceId,
    "X-Emby-Client-Version" to CLIENT_VERSION,
    "X-Emby-Language" to LANGUAGE,
  )

  /**
   * 普通 API 请求附带的客户端标识 query 参数（不含 `X-Emby-Authorization`）。
   *
   * 原来这里是硬编码的 `Emby Web` / `Chrome Windows` / `4.10.1.0`（对齐 Emby 网页端），
   * 现在改成同一份自报标识，服务端看到的客户端前后一致，不会出现
   * 「登录是 A、收藏却变成 Emby Web」这种分裂。
   */
  fun apiQuery(deviceId: String): Map<String, String> = mapOf(
    "X-Emby-Client" to CLIENT_NAME,
    "X-Emby-Device-Name" to deviceName,
    "X-Emby-Device-Id" to deviceId,
    "X-Emby-Client-Version" to CLIENT_VERSION,
    "X-Emby-Language" to LANGUAGE,
  )
}