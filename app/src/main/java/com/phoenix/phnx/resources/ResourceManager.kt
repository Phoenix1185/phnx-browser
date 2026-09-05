package com.phoenix.phnx.resources

class ResourceManager(
    private val policy: ResourcePolicy = ResourcePolicy(),
    private val snapshotProvider: (() -> ResourceSnapshot)? = null,
) {
    fun evaluate(profiles: List<ProfileResourceState>): List<ProfileResourceDecision> =
        evaluate(profiles, snapshotProvider?.invoke() ?: ResourceSnapshot())

    fun evaluate(
        profiles: List<ProfileResourceState>,
        snapshot: ResourceSnapshot,
    ): List<ProfileResourceDecision> = profiles.map { policy.decide(it, snapshot) }
}
