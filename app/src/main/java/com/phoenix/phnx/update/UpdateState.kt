package com.phoenix.phnx.update

enum class UpdateState {
    IDLE,
    CHECKING,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    VERIFYING,
    VERIFIED,
    APPLYING,
    PENDING_RESTART,
    RESTARTING,
    UPDATED,
    CHECK_FAILED,
    DOWNLOAD_FAILED,
    VERIFICATION_FAILED,
    PATCH_FAILED,
    INSTALL_FAILED,
    ROLLBACK_REQUIRED,
}

object UpdateStateMachine {
    fun canTransition(from: UpdateState, to: UpdateState): Boolean = to in transitions[from].orEmpty()

    fun transition(from: UpdateState, to: UpdateState): UpdateState {
        require(canTransition(from, to)) { "Invalid update transition: $from -> $to" }
        return to
    }

    private val transitions = mapOf(
        UpdateState.IDLE to setOf(UpdateState.CHECKING),
        UpdateState.CHECKING to setOf(
            UpdateState.IDLE,
            UpdateState.UPDATE_AVAILABLE,
            UpdateState.CHECK_FAILED,
        ),
        UpdateState.UPDATE_AVAILABLE to setOf(UpdateState.IDLE, UpdateState.DOWNLOADING),
        UpdateState.DOWNLOADING to setOf(UpdateState.DOWNLOADED, UpdateState.DOWNLOAD_FAILED),
        UpdateState.DOWNLOADED to setOf(UpdateState.VERIFYING),
        UpdateState.VERIFYING to setOf(UpdateState.VERIFIED, UpdateState.VERIFICATION_FAILED),
        UpdateState.VERIFIED to setOf(UpdateState.APPLYING),
        UpdateState.APPLYING to setOf(
            UpdateState.PENDING_RESTART,
            UpdateState.PATCH_FAILED,
            UpdateState.INSTALL_FAILED,
        ),
        UpdateState.PENDING_RESTART to setOf(UpdateState.RESTARTING, UpdateState.ROLLBACK_REQUIRED),
        UpdateState.RESTARTING to setOf(UpdateState.UPDATED, UpdateState.ROLLBACK_REQUIRED),
        UpdateState.UPDATED to setOf(UpdateState.IDLE),
        UpdateState.CHECK_FAILED to setOf(UpdateState.IDLE, UpdateState.CHECKING),
        UpdateState.DOWNLOAD_FAILED to setOf(UpdateState.IDLE, UpdateState.DOWNLOADING),
        UpdateState.VERIFICATION_FAILED to setOf(UpdateState.IDLE, UpdateState.DOWNLOADING),
        UpdateState.PATCH_FAILED to setOf(UpdateState.DOWNLOADING, UpdateState.ROLLBACK_REQUIRED),
        UpdateState.INSTALL_FAILED to setOf(UpdateState.ROLLBACK_REQUIRED, UpdateState.IDLE),
        UpdateState.ROLLBACK_REQUIRED to setOf(UpdateState.IDLE),
    )
}
