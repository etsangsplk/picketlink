/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.picketlink.idm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.picketlink.idm.credential.spi.CredentialHandler;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.Partition;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.spi.ContextInitializer;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.picketlink.idm.IDMMessages.MESSAGES;
import static org.picketlink.idm.config.IdentityStoreConfiguration.IdentityOperation;

/**
 * <p>Base class for {@link IdentityStoreConfigurationBuilder} implementations.</p>
 *
 * @author Pedro Igor
 */
public abstract class IdentityStoreConfigurationBuilder<T extends IdentityStoreConfiguration, S extends IdentityStoreConfigurationBuilder<T, S>>
        extends AbstractIdentityConfigurationChildBuilder<T>
        implements IdentityStoreConfigurationChildBuilder {

    private final Map<Class<? extends AttributedType>, Set<IdentityOperation>> supportedTypes;
    private final Map<Class<? extends AttributedType>, Set<IdentityOperation>> unsupportedTypes;
    private final Set<Class<? extends Relationship>> globalRelationshipTypes;
    private final Set<Class<? extends Relationship>> selfRelationshipTypes;
    private final List<Class<? extends CredentialHandler>> credentialHandlers;
    private final Map<String, Object> credentialHandlerProperties;
    private final List<ContextInitializer> contextInitializers;
    private final IdentityStoresConfigurationBuilder identityStoresConfigurationBuilder;
    private boolean supportCredentials;

    protected IdentityStoreConfigurationBuilder(IdentityStoresConfigurationBuilder builder) {
        super(builder);
        this.supportedTypes = new HashMap<Class<? extends AttributedType>, Set<IdentityOperation>>();
        this.unsupportedTypes = new HashMap<Class<? extends AttributedType>, Set<IdentityOperation>>();
        this.globalRelationshipTypes = new HashSet<Class<? extends Relationship>>();
        this.selfRelationshipTypes = new HashSet<Class<? extends Relationship>>();
        this.credentialHandlers = new ArrayList<Class<? extends CredentialHandler>>();
        this.credentialHandlerProperties = new HashMap<String, Object>();
        this.contextInitializers = new ArrayList<ContextInitializer>();
        this.identityStoresConfigurationBuilder = builder;
    }

    @Override
    public FileStoreConfigurationBuilder file() {
        return this.identityStoresConfigurationBuilder.file();
    }

    @Override
    public JPAStoreConfigurationBuilder jpa() {
        return this.identityStoresConfigurationBuilder.jpa();
    }

    @Override
    public LDAPStoreConfigurationBuilder ldap() {
        return this.identityStoresConfigurationBuilder.ldap();
    }

    /**
     * <p>Defines which types should be supported by this configuration.</p>
     *
     * @param types
     * @return
     */
    public S supportType(Class<? extends AttributedType>... attributedTypes) {
        if (attributedTypes == null) {
            throw MESSAGES.nullArgument("Attributed Types");
        }

        for (Class<? extends AttributedType> attributedType : attributedTypes) {
            if (!this.supportedTypes.containsKey(attributedType)) {
                List<IdentityOperation> defaultTypeOperations = Arrays.asList(IdentityOperation.values());
                HashSet<IdentityOperation> supportedOperations =
                        new HashSet<IdentityOperation>(defaultTypeOperations);
                this.supportedTypes.put(attributedType, supportedOperations);
            }
        }

        return (S) this;
    }

    /**
     * <p>Defines which type should not be supported by this configuration.</p>
     * <p>If the operation was not provided, the type should be completely removed from the supported types.
     * Otherwise, only the provided operations should not be supported.</p>
     *
     * @param type
     * @param operation
     * @return
     */
    public S unsupportType(Class<? extends AttributedType> type, IdentityOperation... operations) {
        if (!this.unsupportedTypes.containsKey(type)) {
            this.unsupportedTypes.put(type, new HashSet<IdentityOperation>());
        }

        if (operations != null && operations.length == 0) {
            operations = IdentityOperation.values();
        }

        for (IdentityOperation op : operations) {
            this.unsupportedTypes.get(type).add(op);
        }

        return (S) this;
    }

    /**
     * <p>Defines which types should be supported by this configuration.</p>
     *
     * @param types
     * @return
     */
    public S supportGlobalRelationship(Class<? extends Relationship>... types) {
        this.globalRelationshipTypes.addAll(Arrays.asList(types));
        supportType(types);
        return (S) this;
    }

    /**
     * <p>Defines which types should be supported by this configuration.</p>
     *
     * @param types
     * @return
     */
    public S supportSelfRelationship(Class<? extends Relationship>... types) {
        this.selfRelationshipTypes.addAll(Arrays.asList(types));
        supportType(types);
        return (S) this;
    }

    /**
     * <p>Enables the default feature set for this configuration.</p>
     *
     * @return
     */
    public S supportAllFeatures() {
        supportType(getDefaultIdentityModelClasses());
        supportCredentials(true);

        return (S) this;
    }

    /**
     * <p>Adds a {@link ContextInitializer}.</p>
     *
     * @param contextInitializer
     * @return
     */
    public S addContextInitializer(ContextInitializer contextInitializer) {
        this.contextInitializers.add(contextInitializer);
        return (S) this;
    }

    /**
     * <p>Sets a configuration property for a {@link CredentialHandler}.</p>
     *
     * @param propertyName
     * @param value
     * @return
     */
    public S setCredentialHandlerProperty(String propertyName, Object value) {
        this.credentialHandlerProperties.put(propertyName, value);
        return (S) this;
    }

    /**
     * <p>Adds a custom {@CredentialHandler}.</p>
     *
     * @param credentialHandler
     * @return
     */
    public S addCredentialHandler(Class<? extends CredentialHandler> credentialHandler) {
        this.credentialHandlers.add(credentialHandler);
        return (S) this;
    }

    /**
     * <p>Enable/Disable credentials support</p>
     *
     * @param supportCredentials
     * @return
     */
    public S supportCredentials(boolean supportCredentials) {
        this.supportCredentials = supportCredentials;
        return (S) this;
    }

    @Override
    protected void validate() {
        if (this.supportedTypes.isEmpty()) {
            throw new SecurityConfigurationException("The store configuration must have at least one supported type.");
        }
    }

    @Override
    protected Builder<T> readFrom(T configuration) {
        return this;
    }

    protected List<ContextInitializer> getContextInitializers() {
        return unmodifiableList(this.contextInitializers);
    }

    protected Map<String, Object> getCredentialHandlerProperties() {
        return unmodifiableMap(this.credentialHandlerProperties);
    }

    protected List<Class<? extends CredentialHandler>> getCredentialHandlers() {
        return unmodifiableList(this.credentialHandlers);
    }

    protected Map<Class<? extends AttributedType>, Set<IdentityOperation>> getSupportedTypes() {
        return unmodifiableMap(this.supportedTypes);
    }

    protected Map<Class<? extends AttributedType>, Set<IdentityOperation>> getUnsupportedTypes() {
        return unmodifiableMap(this.unsupportedTypes);
    }

    protected Set<Class<? extends Relationship>> getGlobalRelationshipTypes() {
        return this.globalRelationshipTypes;
    }

    protected Set<Class<? extends Relationship>> getSelfRelationshipTypes() {
        return this.selfRelationshipTypes;
    }

    private static Class<? extends AttributedType>[] getDefaultIdentityModelClasses() {
        List<Class<? extends AttributedType>> classes = new ArrayList<Class<? extends AttributedType>>();

        // identity types
        classes.add(IdentityType.class);

        // relationship types
        classes.add(Relationship.class);

        // partition types
        classes.add(Partition.class);

        return (Class<? extends AttributedType>[]) classes.toArray(new Class<?>[classes.size()]);
    }
}