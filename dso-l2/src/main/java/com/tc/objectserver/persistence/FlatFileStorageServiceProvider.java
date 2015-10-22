package com.tc.objectserver.persistence;

import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.persistence.IPersistentStorage;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 * This service provides a very simple key-value storage persistence system.  It allows key-value data to be serialized to
 * a file in the working directory using Java serialization.
 * 
 * The initial use was to test/support platform restart without depending on CoreStorage.
 */
public class FlatFileStorageServiceProvider implements ServiceProvider {
  private static final TCLogger logger = TCLogging.getLogger(FlatFileStorageServiceProvider.class);
  private Path directory;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    if (configuration instanceof FlatFileStorageProviderConfiguration) {
      this.directory = ((FlatFileStorageProviderConfiguration)configuration).getBasedir().toPath();
    } else {
      this.directory = Paths.get(".").toAbsolutePath().normalize();
    }
    logger.info("Initialized flat file storage to: " + this.directory);
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    String filename = "consumer_" + consumerID + ".dat";
    return configuration.getServiceType().cast(new FlatFilePersistentStorage(this.directory.resolve(filename).toString()));
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(IPersistentStorage.class);
  }

  @Override
  public void close() {
    
  }
  
}