package app.projectzero.launcher

import android.app.Application
import app.projectzero.notificationingest.IngestRuntime

class ProjectZeroApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph.create(this)
        IngestRuntime.attach(graph.coordinator)
    }
}
