/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.dcsg.cpg.configuration;

import java.util.Set;

/**
 * Reads from one configuration source and writes to a different
 * configuration source.
 * 
 * Both reading and writing configurations must be provided in the 
 * constructor. 
 * 
 * @author jbuhacoff
 */
public class ValveConfiguration extends AbstractConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ValveConfiguration.class);
    private Configuration readFrom;
    private Configuration writeTo;

    public ValveConfiguration(Configuration readFrom, Configuration writeTo) {
        this.readFrom = readFrom;
        this.writeTo = writeTo;
    }

    public Configuration getReadFrom() {
        return readFrom;
    }

    public Configuration getWriteTo() {
        return writeTo;
    }

    /**
     * Delegates to the reading configuration.
     * @return 
     */
    @Override
    public Set<String> keys() {
        return readFrom.keys();
    }

    /**
     * Delegates to the reading configuration.
     * @param key
     * @param defaultValue
     * @return 
     */
    @Override
    public String get(String key, String defaultValue) {
        return readFrom.get(key, defaultValue);
    }

    /**
     * Delegates to the writing configuration
     * @param key
     * @param value 
     */
    @Override
    public void set(String key, String value) {
        writeTo.set(key, value);
    }

    /**
     * Delegates to the writing configuration.
     * @return 
     */
    @Override
    public boolean isEditable() {
        return writeTo.isEditable();
    }
    
    
}
