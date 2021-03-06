/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;

/**
 * A base class for all {@link com.tc.config.schema.setup.ConfigurationSetupManagerFactory} instances.
 */
public abstract class BaseConfigurationSetupManagerFactory implements ConfigurationSetupManagerFactory {

  protected final ConfigBeanFactory beanFactory;

  public BaseConfigurationSetupManagerFactory() {
    this.beanFactory = new TerracottaDomainConfigurationDocumentBeanFactory();
  }

  @Override
  public L2ConfigurationSetupManager createL2TVSConfigurationSetupManager(String l2Name) throws ConfigurationSetupException {
    return this.createL2TVSConfigurationSetupManager(l2Name, ClassLoader.getSystemClassLoader());
  }
  
  
}
