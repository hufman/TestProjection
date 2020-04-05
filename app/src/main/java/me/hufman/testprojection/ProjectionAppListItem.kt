package me.hufman.testprojection

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import me.hufman.carprojection.ProjectionAppInfo

class ProjectionAppListItem(context: Context, apps: MutableList<ProjectionAppInfo>): ArrayAdapter<ProjectionAppInfo>(context, R.layout.projectionapp_listitem, apps) {
	@SuppressLint("SetTextI18n")
	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val packageManager = context.packageManager
		val layoutInflater = context.getSystemService(LayoutInflater::class.java)
		val appInfo = getItem(position)
		return if (packageManager != null && layoutInflater != null) {
			val layout = convertView
					?: layoutInflater.inflate(R.layout.projectionapp_listitem, parent, false)
			return if (appInfo != null) {
				val className = appInfo.className.removePrefix("com.google.android.")
				layout.findViewById<ImageView>(R.id.imgAppIcon).setImageDrawable(appInfo.icon)
				layout.findViewById<TextView>(R.id.txtAppName).setText("${appInfo.name} - ${className}")
				layout
			} else {
				layout.findViewById<TextView>(R.id.txtAppName).setText("Error")
				layout
			}
		} else {
			TextView(context).apply {
				this.text = "Bad context: pm:$packageManager li:$layoutInflater"
			}
		}
	}
}