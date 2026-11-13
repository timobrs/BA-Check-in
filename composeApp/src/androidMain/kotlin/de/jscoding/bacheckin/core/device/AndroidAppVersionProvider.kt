package de.jscoding.bacheckin.core.device

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppVersionProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppVersionProvider {

    override val versionName: String
        get() {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName, 0
            )

            return packageInfo.versionName ?: "unknown"
        }

    override val versionCode: Long
        get() {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName, 0
            )

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        }
}
