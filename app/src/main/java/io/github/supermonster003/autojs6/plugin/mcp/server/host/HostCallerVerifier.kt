package io.github.supermonster003.autojs6.plugin.mcp.server.host

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import java.security.MessageDigest

/**
 * Admits only the installed AutoJs6 host to the session Binder (roadmap P2.3, decision D18):
 * the calling UID must be the host package's UID, the host must be at least the version that
 * ships the contract, and both APKs must carry the same signer set. The manifest permission
 * `org.autojs.permission.PLUGIN` keeps strangers from binding at all; this check is the second
 * line for a caller that obtained the permission anyway.
 */
internal class HostCallerVerifier(context: Context) {

    private val packageManager = context.applicationContext.packageManager
    private val pluginPackageName = context.applicationContext.packageName

    /** The calling UID once it passed every check. */
    fun enforceHost(): Int = Binder.getCallingUid().also(::enforceHostUid)

    fun enforceSessionOwner(expectedUid: Int) {
        val callingUid = Binder.getCallingUid()
        if (callingUid != expectedUid) throw SecurityException("MCP session owner changed")
        enforceHostUid(callingUid)
    }

    private fun enforceHostUid(callingUid: Int) {
        val hostPackage = McpServerPlugin.HOST_PACKAGE_NAME
        val installedUid = try {
            packageManager.getApplicationInfo(hostPackage, 0).uid
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val packagesForUid = packageManager.getPackagesForUid(callingUid)?.toSet().orEmpty()
        val pluginSigners = signerDigests(pluginPackageName)
        val hostSigners = signerDigests(hostPackage)
        val hostVersionCode = installedVersionCode(hostPackage)
        if (
            installedUid == null ||
            callingUid != installedUid ||
            hostPackage !in packagesForUid ||
            hostVersionCode == null ||
            hostVersionCode < McpServerPlugin.REQUIRED_HOST_VERSION ||
            pluginSigners.isEmpty() ||
            pluginSigners != hostSigners
        ) {
            throw SecurityException("Caller is not the installed same-signer AutoJs6 host")
        }
    }

    private fun installedVersionCode(packageName: String): Long? = try {
        val packageInfo = packageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun signerDigests(packageName: String): Set<String> {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return emptySet()
        }
        val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures.orEmpty()
        }
        return signers.mapTo(linkedSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }
    }

    private fun packageInfo(packageName: String, flags: Int): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }
}
