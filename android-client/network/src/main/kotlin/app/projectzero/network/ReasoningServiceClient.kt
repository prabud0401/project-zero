package app.projectzero.network

import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.SummarizedCluster

interface ReasoningServiceClient {
    suspend fun summarizeBatch(events: List<NotificationEvent>): List<SummarizedCluster>
}

class DefaultReasoningServiceClient : ReasoningServiceClient {
    override suspend fun summarizeBatch(events: List<NotificationEvent>): List<SummarizedCluster> {
        // network logic
        return emptyList()
    }
}
