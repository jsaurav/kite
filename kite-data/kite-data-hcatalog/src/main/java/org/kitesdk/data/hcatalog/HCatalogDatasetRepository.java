/**
 * Copyright 2013 Cloudera Inc.
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
package org.kitesdk.data.hcatalog;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetRepository;
import org.kitesdk.data.spi.MetadataProvider;

/**
 * <p>
 * A {@link DatasetRepository} that uses the Hive/HCatalog metastore for metadata,
 * and stores data in a Hadoop {@link FileSystem}.
 * </p>
 * <p>
 * The location of the data directory is either chosen by Hive/HCatalog (so called
 * "managed tables"), or specified when creating an instance of this class by providing
 * a {@link FileSystem}, and a root directory in the constructor ("external tables").
 * </p>
 * <p>
 * The primary methods of interest will be
 * {@link #create(String, DatasetDescriptor)}, {@link #load(String)}, and
 * {@link #delete(String)} which create a new dataset, load an existing
 * dataset, or delete an existing dataset, respectively. Once a dataset has been created
 * or loaded, users can invoke the appropriate {@link Dataset} methods to get a reader
 * or writer as needed.
 * </p>
 *
 * @see DatasetRepository
 * @see Dataset
 */
public class HCatalogDatasetRepository extends HCatalogAbstractDatasetRepository {

  /**
   * Create an HCatalog dataset repository with managed tables.
   */
  HCatalogDatasetRepository(Configuration conf) {
    super(conf, new HCatalogManagedMetadataProvider(conf));
  }

  /**
   * Create an HCatalog dataset repository with managed tables.
   */
  HCatalogDatasetRepository(Configuration conf, MetadataProvider provider) {
    super(conf, provider);
  }

  @Override
  public <E> Dataset<E> create(String name, DatasetDescriptor descriptor) {
    // avoids calling fsRepository.create, which creates the data path
    getMetadataProvider().create(name, descriptor);
    return load(name);
  }

  /**
   * A fluent builder to aid in the construction of {@link HCatalogDatasetRepository}
   * instances.
   * @since 0.3.0
   */
  public static class Builder {

    private Path rootDirectory;
    private Configuration configuration;

    /**
     * The root directory for dataset files.
     */
    public Builder rootDirectory(Path path) {
      this.rootDirectory = path;
      return this;
    }

    /**
     * The root directory for dataset files.
     */
    public Builder rootDirectory(URI uri) {
      this.rootDirectory = new Path(uri);
      return this;
    }

    /**
     * The root directory for metadata and dataset files.
     *
     * @param uri a String to parse as a URI
     * @return this Builder for method chaining.
     * @throws URISyntaxException
     *
     * @since 0.8.0
     */
    public Builder rootDirectory(String uri) throws URISyntaxException {
      this.rootDirectory = new Path(new URI(uri));
      return this;
    }

    /**
     * The {@link Configuration} used to find the {@link FileSystem}. Optional. If not
     * specified, the default configuration will be used.
     */
    public Builder configuration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    /**
     * Build an instance of the configured {@link HCatalogDatasetRepository}.
     *
     * @since 0.9.0
     */
    @SuppressWarnings("deprecation")
    public DatasetRepository build() {

      if (configuration == null) {
        this.configuration = new Configuration();
      }

      if (rootDirectory != null) {
        return new HCatalogExternalDatasetRepository(configuration, rootDirectory);
      } else {
        return new HCatalogDatasetRepository(configuration);
      }
    }
  }
}
