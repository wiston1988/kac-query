package com.kac.test.service;

import com.kac.common.DispatchException;
import com.kac.server.Service;

public abstract class ServiceBase implements Service {
    public void init() throws DispatchException {
        // Nothing to do
    }

    public void reload() throws DispatchException {
        // Nothing to do
    }

    public void close() {
        // Nothing to do
    }
}
