package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sun.security.util.SecurityConstants;

import java.io.File;
import java.io.FilePermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link SecurityManager} to spotlight and minimize IO access while allowing
 * fine-grained control access to IO resources.
 *
 * <i>Usage.</i> To use the {@link ChrootSecurityManager}, you must set the
 * "java.security.manager" system property to
 * "org.apache.lucene.util.ChrootSecurityManager", or a subclass of this.
 *
 * <i>Usage via command-line arguments.</i> You may add
 * "-Djava.security.manager=org.apache.lucene.util.ChrootSecurityManager"
 * to your command-line invocation of the JVM to use this class as your
 * {@link SecurityManager}.
 *
 * <i>Usage via Ant.</i> You may declare the "java.security.manager" system
 * property in the "junit" element of your "build.xml" file. You <b>must</b> set
 * the "fork" property to ensure a new JVM, with this class as the
 * {@link SecurityManager} is utilized.
 *
 * <pre>
 * {@code
 * <junit fork="true">
 *   <sysproperty key="java.security.manager" value="org.apache.lucene.util.ChrootSecurityManager" />
 *   ...
 * </junit>
 * }
 * </pre>
 *
 * This security manager works by <em>replacing</em> the built in security policy, this means that
 * if you have a custom policy enabled, and then enable this class it will simply overwrite the
 * established policy with its own.
 */
public class ChrootSecurityManager extends SecurityManager {

  public ChrootSecurityManager(File... allowedPaths) {
    super();
    Policy.setPolicy(new ChrootSecurityPolicy(allowedPaths));
  }

  private class ChrootSecurityPolicy extends Policy {

    private final CompositePermissionCollection permissions;

    public ChrootSecurityPolicy(File... allowedPaths) {
      super();

      Permission[] perms = new Permission[allowedPaths.length + 1];
      for (int i=0; i<allowedPaths.length; i++) {
        String rootPath = new File(allowedPaths[i], "*").getAbsolutePath();
        perms[i] = new FilePermission(rootPath, "read,write,delete,readlink,execute");
      }

      perms[perms.length] = new AllButFilePermission();
      permissions = new CompositePermissionCollection(perms);
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
      return permissions;
    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
      return permissions;
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
      PermissionCollection collection = this.getPermissions(domain);
      return collection.implies(permission);
    }

  }


  private final class AllButFilePermission extends Permission {

    public AllButFilePermission() {
      this("<all but file permissions>");
    }

    public AllButFilePermission(String name) {
      super(name);
    }

    @Override
    public boolean implies(Permission permission) {
      return !(permission instanceof FilePermission);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof AllButFilePermission;
    }

    @Override
    public int hashCode() {
      return 2;
    }

    @Override
    public String getActions() {
      return SecurityConstants.ALL_PERMISSION.getActions();
    }

  }

  private final class CompositePermissionCollection extends PermissionCollection {
    private final List<Permission> permissions;
    public CompositePermissionCollection(Permission... permissions) {
      this.permissions = Collections.unmodifiableList(Arrays.asList(permissions));
    }

    @Override
    public void add(Permission permission) {
      throw new UnsupportedOperationException("Immutable Permissions collection, disallowing");
    }

    @Override
    public boolean implies(Permission toTest) {
      for (Permission permission : permissions) {
        if (permission.implies(toTest)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Enumeration<Permission> elements() {
      return new Enumeration<Permission>() {
        private Iterator<Permission> iterator = permissions.iterator();
        @Override
        public boolean hasMoreElements() {
          return iterator.hasNext();
        }

        @Override
        public Permission nextElement() {
          return iterator.next();
        }
      };
    }
  }
}
