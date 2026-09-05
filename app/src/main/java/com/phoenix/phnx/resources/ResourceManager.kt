package com.phoenix.phnx.resources

class ResourceManager(
    private val policy: ResourcePolicy = ResourcePolicy(),
) {
    fun evaluate(
        profiles: List<ProfileResourceState>,
        snapshot: ResourceSnapshot,
    ): List<ProfileResourceDecision> = profiles.map { policy.decide(it, snapshot) }
}
