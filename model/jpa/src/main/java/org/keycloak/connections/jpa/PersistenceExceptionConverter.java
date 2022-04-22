/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.jpa;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;
import org.keycloak.models.utils.RetryUtil;
/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PersistenceExceptionConverter implements InvocationHandler {
    private static final Logger logger = Logger.getLogger(PersistenceExceptionConverter.class);

    private static final Pattern WRITE_METHOD_NAMES = Pattern.compile("persist|merge");

    private final EntityManager em;
    private final boolean batchEnabled;
    private final int batchSize;
    private int changeCount = 0;

    public static EntityManager create(KeycloakSession session, EntityManager em) {
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, new PersistenceExceptionConverter(session, em));
    }

    private PersistenceExceptionConverter(KeycloakSession session, EntityManager em) {
        batchEnabled = session.getAttributeOrDefault(Constants.STORAGE_BATCH_ENABLED, false);
        batchSize = session.getAttributeOrDefault(Constants.STORAGE_BATCH_SIZE, 100);
        this.em = em;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.debugf("invoking %s on EntityManager", method.getName());
        try {
            flushInBatchIfEnabled(method);
            return method.invoke(em, args);
        } catch (InvocationTargetException e) {
          boolean retry = RetryUtil.shouldRetry(e.getTargetException());
          logger.warn(String.format("error during %s, retryable %b", method.getName(), retry), e.getTargetException());
          throw convert(e.getCause());
        } catch (Throwable t) {
          boolean retry = RetryUtil.shouldRetry(t);
          logger.warn(String.format("error during %s, retryable %b", method.getName(), retry), t);
          throw t;
        }
    }

    private void flushInBatchIfEnabled(Method method) {
        if (batchEnabled) {
            if (WRITE_METHOD_NAMES.matcher(method.getName()).matches()) {
                if (changeCount++ > batchSize) {
                    em.flush();
                    em.clear();
                    changeCount = 0;
                }
            }
        }
    }

    public static ModelException convert(Throwable t) {
        if (t.getCause() != null && t.getCause() instanceof ConstraintViolationException) {
            throw new ModelDuplicateException(t);
        } if (t instanceof EntityExistsException || t instanceof ConstraintViolationException) {
            throw new ModelDuplicateException(t);
        } else {
            throw new ModelException(t);
        }
    }

}
