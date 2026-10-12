package org.agripedia.app

import android.app.Application
import org.agripedia.app.notifications.Reminders

class AgriPediaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannel(this)
        Reminders.schedule(this)
    }
}
