package me.hufman.carprojection

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager

class AppDiscovery(val context: Context) {
	fun discoverApps(): List<ProjectionAppInfo> {
		val filter = Intent("android.intent.action.MAIN")
		filter.addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION")
		val services = context.packageManager.queryIntentServices(filter, PackageManager.GET_RESOLVED_FILTER)
		return services.map {
			ProjectionAppInfo(it.serviceInfo.packageName, it.serviceInfo.name)
		}
	}

	fun connectApp(appInfo: ProjectionAppInfo, serviceConnection: ServiceConnection): Boolean {
		val component = ComponentName(appInfo.packageName, appInfo.className)
		val intent = Intent()
		intent.component = component
		return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
	}
}