package com.sqljudge.judge.sandbox;

import java.util.Map;
import java.util.Optional;

public final class SandboxRegistry {

    private final Map<Dialect, Sandbox> sandboxes;

    public SandboxRegistry(Map<Dialect, Sandbox> sandboxes) {
        this.sandboxes = Map.copyOf(sandboxes);
    }

    public Optional<Sandbox> forDialect(Dialect dialect) {
        return Optional.ofNullable(sandboxes.get(dialect));
    }
}
