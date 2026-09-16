package app.projectzero.domain.ports

import app.projectzero.domain.action.AppAffinityContext
import app.projectzero.domain.action.IntentAction
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.SummarizedCluster
import app.projectzero.domain.routing.ExecutionResult
import app.projectzero.domain.routing.FilterDecision
import app.projectzero.domain.routing.ParsedIntent
import app.projectzero.domain.routing.PolicyDecision
import app.projectzero.domain.routing.RouteResult
import kotlinx.coroutines.flow.Flow

interface NotificationIngestor {
    fun events(): Flow<NotificationEvent>
}

interface NotificationFilter {
    suspend fun evaluate(event: NotificationEvent): FilterDecision
}

interface SummaryEngine {
    suspend fun summarize(events: List<NotificationEvent>): List<SummarizedCluster>
}

interface SemanticRouter {
    suspend fun route(text: String, context: AppAffinityContext): RouteResult
}

interface DeepLinkResolver {
    suspend fun resolve(parsed: ParsedIntent, context: AppAffinityContext): RouteResult
}

interface ActionPolicyGate {
    suspend fun validate(action: IntentAction): PolicyDecision
}

interface ActionExecutor {
    suspend fun execute(action: IntentAction, previewDigest: String): ExecutionResult
}
