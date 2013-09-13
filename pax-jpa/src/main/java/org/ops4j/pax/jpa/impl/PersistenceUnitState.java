package org.ops4j.pax.jpa.impl;

/**
 * State of a persistence unit with respect to its OSGi service dependencies.
 * 
 * @author Harald Wellmann
 *
 */
public enum PersistenceUnitState {
    
    /**
     * Persistence unit is not assigned to a provider.
     */
    UNASSIGNED,
    
    /**
     * Persistence unit is assigned to a provider but not bound to a data source.
     */
    READY,
    
    /**
     * Persistence unit is assigned to a provider and bound to a data source.
     */
    COMPLETE
}
