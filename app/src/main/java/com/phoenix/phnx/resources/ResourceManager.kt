package com.phoenix.phnx.resources

class ResourceManager(
    private val policy: ResourcePolicy = ResourcePolicy(),
    private val snapshotProvider: (() -> ResourceSnapshot)? = null,
    private val lifecycleAdapter: ((ProfileResourceDecision) -> Unit)? = null,
) {
    fun evaluate(profiles: List<ProfileResourceState>): List<ProfileResourceDecision> =
        evaluate(profiles, snapshotProvider?.invoke() ?: ResourceSnapshot())

    fun evaluate(
        profiles: List<ProfileResourceState>,
        snapshot: ResourceSnapshot,
    ): List<ProfileResourceDecision> = profiles.map { policy.decide(it, snapshot) }

    fun reconcile(profiles: List<ProfileResourceState>): List<ProfileResourceDecision> =
        reconcile(profiles, snapshotProvider?.invoke() ?: ResourceSnapshot())

    fun reconcile(
        profiles: List<ProfileResourceState>,
        snapshot: ResourceSnapshot,
    ): List<ProfileResourceDecision> = evaluate(profiles, snapshot).also { decisions ->
        decisions.forEach { lifecycleAdapter?.invoke(it) }
    }
}
