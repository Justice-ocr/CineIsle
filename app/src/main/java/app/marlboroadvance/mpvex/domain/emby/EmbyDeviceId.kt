package app.marlboroadvance.mpvex.domain.emby

// ════════════════════════════════════════════════════════════════════════
// 设备 ID（从 EmbyClient 末尾原样搬出，可见性 private → internal）
// ════════════════════════════════════════════════════════════════════════
//
// 取流（`EmbyStreamApi` 里拼 `DeviceId` 参数）与鉴权头（`EmbyClient`）都要用，
// 所以提到顶层。实现逐字未改。

/**
 * 简单生成稳定的设备 ID（基于主机 + 安卓 ANDROID_ID 哈希）。
 * Emby 用 DeviceId 标识设备，同一账号同一设备会复用会话。
 *
 * 形状对齐 Hills：真机上的 DeviceId 是 16 位小写 hex（如 `427872f35a85d7b5`），
 * 所以这里取 MD5 的前 16 位，而不是原来的 32 位。
 */
internal fun androidDeviceId(seed: String): String {
  // 不依赖 Context（避免本类需要 Application 注入），用稳定字符串哈希
  val raw = "hills-android-$seed"
  val md = java.security.MessageDigest.getInstance("MD5")
  val bytes = md.digest(raw.toByteArray())
  return bytes.joinToString("") { "%02x".format(it) }.take(16)
}
