package com.tc.objectserver.persistence.gb;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import java.util.Map;

/**
 * @author tim
 */
public interface StorageManagerFactory {
  StorageManager createStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap);
}
