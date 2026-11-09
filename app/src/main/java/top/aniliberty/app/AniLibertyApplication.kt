package top.aniliberty.app

import android.app.Application
import android.content.Context
import top.aniliberty.app.data.AniLibertyApi
import top.aniliberty.app.data.LocalStore
import top.aniliberty.app.data.Network
import top.aniliberty.app.data.Repository
import top.aniliberty.app.notifications.EpisodeNotifications

class AppContainer(context: Context) {
    val store = LocalStore(context.applicationContext)
    val api: AniLibertyApi = Network.create(context.cacheDir)
    val repository = Repository(context.applicationContext, api, store)
}
class AniLibertyApplication : Application() {
    val container by lazy { AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        EpisodeNotifications.ensureChannel(this)
    }
}
