package com.ureka.play4change.core.network

import cnames.structs.__CFData
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecUseDataProtectionKeychain
import platform.Security.kSecValueData

private const val SERVICE = "com.ureka.play4change.tokens"
private const val KEY_ACCESS = "access_token"
private const val KEY_REFRESH = "refresh_token"

/**
 * Stores JWT tokens in the iOS Keychain using the **Data Protection Keychain**
 * ([kSecUseDataProtectionKeychain] = true), required on iOS 13 + and enforced on
 * iOS 26.2 simulator where the legacy System Keychain is disabled via a feature flag
 * ("System Keychain Always Supported set via feature flag to disabled").
 *
 * Token accessibility is [kSecAttrAccessibleAfterFirstUnlock], which allows background
 * token refresh after the first device unlock post-reboot.
 * Plain UserDefaults must never be used for tokens. See THREAT-LOG Phase 04 Security Note.
 *
 * Note on unit testing: [saveItem] calls SecItemAdd / SecItemUpdate from the Security
 * framework, which requires a running iOS Keychain (simulator or device). These APIs
 * cannot be intercepted in KMP commonTest without a full XCTest host. Correctness is
 * verified through the Phase 04 manual test recipe (Section 1 Keychain DB query and
 * Section 2 persistence-across-restart check). See ISSUES.md B7.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainTokenStorage : TokenStorage {

    override suspend fun getAccessToken(): String? = readItem(KEY_ACCESS)
    override suspend fun getRefreshToken(): String? = readItem(KEY_REFRESH)

    override suspend fun store(accessToken: String, refreshToken: String) {
        saveItem(KEY_ACCESS, accessToken)
        saveItem(KEY_REFRESH, refreshToken)
    }

    override suspend fun clear() {
        deleteItem(KEY_ACCESS)
        deleteItem(KEY_REFRESH)
    }

    private fun readItem(account: String): String? = memScoped {
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 6, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecUseDataProtectionKeychain, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecAttrService, cfString(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, cfString(account))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val resultRef = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, resultRef.ptr)
        if (status != errSecSuccess) return@memScoped null

        val opaquePtr = resultRef.value ?: return@memScoped null
        val cfData = interpretCPointer<__CFData>(opaquePtr.rawValue) ?: return@memScoped null
        val length = CFDataGetLength(cfData).toInt()
        if (length <= 0) return@memScoped null
        CFDataGetBytePtr(cfData)?.reinterpret<ByteVar>()?.readBytes(length)?.decodeToString()
    }

    /**
     * Saves [value] under [account] in the Data Protection Keychain.
     *
     * Strategy: attempt SecItemAdd first. If the item already exists
     * (errSecDuplicateItem — possible after app reinstall or a failed [clear])
     * fall back to SecItemUpdate. Any other non-success OSStatus throws
     * [IllegalStateException] so the caller surfaces a meaningful error instead
     * of silently proceeding with no stored tokens.
     */
    private fun saveItem(account: String, value: String) {
        val bytes = value.encodeToByteArray()
        val cfData = bytes.usePinned { pinned ->
            CFDataCreate(kCFAllocatorDefault, pinned.addressOf(0).reinterpret(), bytes.size.convert())
        } ?: throw IllegalStateException("Keychain: CFDataCreate returned null for key=$account")

        val addQuery = CFDictionaryCreateMutable(kCFAllocatorDefault, 6, null, null)!!
        CFDictionaryAddValue(addQuery, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(addQuery, kSecUseDataProtectionKeychain, kCFBooleanTrue)
        CFDictionaryAddValue(addQuery, kSecAttrService, cfString(SERVICE))
        CFDictionaryAddValue(addQuery, kSecAttrAccount, cfString(account))
        CFDictionaryAddValue(addQuery, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
        CFDictionaryAddValue(addQuery, kSecValueData, cfData)

        val addStatus = SecItemAdd(addQuery, null)
        when (addStatus) {
            errSecSuccess -> {}  // stored successfully

            errSecDuplicateItem -> {
                // Item already exists; update the value in place.
                val searchQuery = CFDictionaryCreateMutable(kCFAllocatorDefault, 4, null, null)!!
                CFDictionaryAddValue(searchQuery, kSecClass, kSecClassGenericPassword)
                CFDictionaryAddValue(searchQuery, kSecUseDataProtectionKeychain, kCFBooleanTrue)
                CFDictionaryAddValue(searchQuery, kSecAttrService, cfString(SERVICE))
                CFDictionaryAddValue(searchQuery, kSecAttrAccount, cfString(account))

                val updateAttrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 1, null, null)!!
                CFDictionaryAddValue(updateAttrs, kSecValueData, cfData)

                val updateStatus = SecItemUpdate(searchQuery, updateAttrs)
                if (updateStatus != errSecSuccess) {
                    throw IllegalStateException(
                        "Keychain: SecItemUpdate failed for key=$account status=$updateStatus"
                    )
                }
            }

            else -> throw IllegalStateException(
                "Keychain: SecItemAdd failed for key=$account status=$addStatus"
            )
        }
    }

    private fun deleteItem(account: String) {
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 4, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecUseDataProtectionKeychain, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecAttrService, cfString(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, cfString(account))
        SecItemDelete(query)
    }

    private fun cfString(value: String) =
        CFStringCreateWithCString(kCFAllocatorDefault, value, kCFStringEncodingUTF8)
}
