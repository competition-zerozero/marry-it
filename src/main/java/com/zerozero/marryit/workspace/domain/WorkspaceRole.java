package com.zerozero.marryit.workspace.domain;

public enum WorkspaceRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean canInvite() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canGrant(WorkspaceRole targetRole) {
        return switch (this) {
            case OWNER -> targetRole != OWNER;
            case ADMIN -> targetRole == MEMBER;
            case MEMBER -> false;
        };
    }

    public int priority() {
        return switch (this) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
        };
    }

    public boolean isHigherThan(WorkspaceRole other) {
        return priority() < other.priority();
    }
}
