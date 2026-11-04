package com.example.data.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID

object DeviceSecurityManager {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "finpulse_device_identity_key"
  private const val PREFS_NAME = "finpulse_device_security"
  private const val KEY_DEVICE_ID = "keystore_device_id"

  /**
   * Retrieves or generates a cryptographically anchored device identifier.
   * Utilizes Android Keystore to generate an RSA/EC keypair so the identity is
   * hardware/key-backed, combined with a persistent UUID that resists casual tampering.
   */
  fun getOrCreateDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existingId = prefs.getString(KEY_DEVICE_ID, null)
    if (!existingId.isNullOrBlank()) {
      return existingId
    }

    // Generate or retrieve Keystore backed key
    val keyDigest = try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      if (!keyStore.containsAlias(KEY_ALIAS)) {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        kpg.initialize(
          KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
          )
          .setDigests(KeyProperties.DIGEST_SHA256)
          .build()
        )
        kpg.generateKeyPair()
      }
      val certificate = keyStore.getCertificate(KEY_ALIAS)
      val pubKeyBytes = certificate?.publicKey?.encoded ?: byteArrayOf()
      sha256Hex(pubKeyBytes)
    } catch (e: Exception) {
      UUID.randomUUID().toString()
    }

    val persistentUuid = UUID.randomUUID().toString().replace("-", "").take(16)
    val compositeDeviceId = "FP-${Build.MANUFACTURER.take(3).uppercase()}-${keyDigest.take(12)}-${persistentUuid}"

    prefs.edit().putString(KEY_DEVICE_ID, compositeDeviceId).apply()
    return compositeDeviceId
  }

  fun getDeviceModel(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
  }

  fun getOsVersion(): String {
    return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
  }

  fun getDeviceFingerprint(context: Context): String {
    val raw = "${Build.BRAND}:${Build.DEVICE}:${Build.BOARD}:${Build.HARDWARE}:${getOrCreateDeviceId(context)}"
    return sha256Hex(raw.toByteArray()).take(32)
  }

  private fun sha256Hex(bytes: ByteArray): String {
    if (bytes.isEmpty()) return UUID.randomUUID().toString().replace("-", "")
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
  }
}
