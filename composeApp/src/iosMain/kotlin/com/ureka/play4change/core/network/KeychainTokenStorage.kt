package com.ureka.play4change.core.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
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
import platform.Security.kSecValueData

private const val SERVICE = "com.ureka.play4change.tokens"
private const val KEY_ACCESS = "access_token"
private const val KEY_REFRESH = "refresh_token"

/**
 * Stores JWT tokens in the iOS Keychain with [kSecAttrAccessibleAfterFirstUnlock]
 * accessibility, which allows background access after the first device unlock.
 *
 * Plain UserDefaults must never be used for tokens. See THREAT-LOG Phase 04 Security Note.
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
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 5, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfString(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, cfString(account))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val resultRef = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, resultRef.ptr)
        if (status != errSecSuccess) return@memScoped null

        @Suppress("UNCHECKED_CAST")
        val nsData = resultRef.value as? NSData ?: return@memScoped null
        NSString.create(nsData, NSUTF8StringEncoding) as? String
    }

    private fun saveItem(account: String, value: String) {
        deleteItem(account)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 5, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfString(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, cfString(account))
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
        CFDictionaryAddValue(query, kSecValueData, data)
        SecItemAdd(query, null)
    }

    private fun deleteItem(account: String) {
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 3, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfString(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, cfString(account))
        SecItemDelete(query)
    }

    private fun cfString(value: String) =
        CFStringCreateWithCString(kCFAllocatorDefault, value, kCFStringEncodingUTF8)
}
