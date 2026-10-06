package com.bromax.pestilence.entity;

/** A plague mob that only spawns naturally from a given Pestilence phase on. */
public interface PhaseGated {
    int minPhase();
}
