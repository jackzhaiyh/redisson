/**
 * Copyright 2016 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.redisson.api.RedissonNodeInitializer;
import org.redisson.client.codec.Codec;
import org.redisson.cluster.ClusterConnectionManager;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.ElasticacheConnectionManager;
import org.redisson.connection.MasterSlaveConnectionManager;
import org.redisson.connection.SentinelConnectionManager;
import org.redisson.connection.SingleConnectionManager;
import org.redisson.connection.balancer.LoadBalancer;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.redisson.codec.CodecProvider;
import org.redisson.liveobject.provider.ResolverProvider;

public class ConfigSupport {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
    @JsonFilter("classFilter")
    public static class ClassMixIn {

    }

    public abstract static class SingleSeverConfigMixIn {

        @JsonProperty
        List<URI> address;

        @JsonIgnore
        abstract SingleServerConfig setAddress(String address);

        @JsonIgnore
        abstract URI getAddress();

        @JsonIgnore
        abstract void setAddress(URI address);

    }

    public abstract static class MasterSlaveServersConfigMixIn {

        @JsonProperty
        List<URI> masterAddress;

        @JsonIgnore
        abstract MasterSlaveServersConfig setMasterAddress(String masterAddress);

        @JsonIgnore
        abstract URI getMasterAddress();

        @JsonIgnore
        abstract void setMasterAddress(URI masterAddress);

    }

    @JsonIgnoreProperties("clusterConfig")
    public static class ConfigMixIn {

        @JsonProperty
        SentinelServersConfig sentinelServersConfig;

        @JsonProperty
        MasterSlaveServersConfig masterSlaveServersConfig;

        @JsonProperty
        SingleServerConfig singleServerConfig;

        @JsonProperty
        ClusterServersConfig clusterServersConfig;

        @JsonProperty
        ElasticacheServersConfig elasticacheServersConfig;

    }

    private final ObjectMapper jsonMapper = createMapper(null);
    private final ObjectMapper yamlMapper = createMapper(new YAMLFactory());

    public <T> T fromJSON(String content, Class<T> configType) throws IOException {
        return jsonMapper.readValue(content, configType);
    }

    public <T> T fromJSON(File file, Class<T> configType) throws IOException {
        return jsonMapper.readValue(file, configType);
    }

    public <T> T fromJSON(URL url, Class<T> configType) throws IOException {
        return jsonMapper.readValue(url, configType);
    }

    public <T> T fromJSON(Reader reader, Class<T> configType) throws IOException {
        return jsonMapper.readValue(reader, configType);
    }

    public <T> T fromJSON(InputStream inputStream, Class<T> configType) throws IOException {
        return jsonMapper.readValue(inputStream, configType);
    }

    public String toJSON(Config config) throws IOException {
        return jsonMapper.writeValueAsString(config);
    }

    public <T> T fromYAML(String content, Class<T> configType) throws IOException {
        return yamlMapper.readValue(content, configType);
    }

    public <T> T fromYAML(File file, Class<T> configType) throws IOException {
        return yamlMapper.readValue(file, configType);
    }

    public <T> T fromYAML(URL url, Class<T> configType) throws IOException {
        return yamlMapper.readValue(url, configType);
    }

    public <T> T fromYAML(Reader reader, Class<T> configType) throws IOException {
        return yamlMapper.readValue(reader, configType);
    }

    public <T> T fromYAML(InputStream inputStream, Class<T> configType) throws IOException {
        return yamlMapper.readValue(inputStream, configType);
    }

    public String toYAML(Config config) throws IOException {
        return yamlMapper.writeValueAsString(config);
    }
    
    public static ConnectionManager createConnectionManager(Config configCopy) {
        if (configCopy.getMasterSlaveServersConfig() != null) {
            validate(configCopy.getMasterSlaveServersConfig());
            return new MasterSlaveConnectionManager(configCopy.getMasterSlaveServersConfig(), configCopy);
        } else if (configCopy.getSingleServerConfig() != null) {
            validate(configCopy.getSingleServerConfig());
            return new SingleConnectionManager(configCopy.getSingleServerConfig(), configCopy);
        } else if (configCopy.getSentinelServersConfig() != null) {
            validate(configCopy.getSentinelServersConfig());
            return new SentinelConnectionManager(configCopy.getSentinelServersConfig(), configCopy);
        } else if (configCopy.getClusterServersConfig() != null) {
            validate(configCopy.getClusterServersConfig());
            return new ClusterConnectionManager(configCopy.getClusterServersConfig(), configCopy);
        } else if (configCopy.getElasticacheServersConfig() != null) {
            validate(configCopy.getElasticacheServersConfig());
            return new ElasticacheConnectionManager(configCopy.getElasticacheServersConfig(), configCopy);
        } else {
            throw new IllegalArgumentException("server(s) address(es) not defined!");
        }
    }

    private static void validate(SingleServerConfig config) {
        if (config.getConnectionPoolSize() < config.getConnectionMinimumIdleSize()) {
            throw new IllegalArgumentException("connectionPoolSize can't be lower than connectionMinimumIdleSize");
        }
    }
    
    private static void validate(BaseMasterSlaveServersConfig<?> config) {
        if (config.getSlaveConnectionPoolSize() < config.getSlaveConnectionMinimumIdleSize()) {
            throw new IllegalArgumentException("slaveConnectionPoolSize can't be lower than slaveConnectionMinimumIdleSize");
        }
        if (config.getMasterConnectionPoolSize() < config.getMasterConnectionMinimumIdleSize()) {
            throw new IllegalArgumentException("masterConnectionPoolSize can't be lower than masterConnectionMinimumIdleSize");
        }
        if (config.getSlaveSubscriptionConnectionPoolSize() < config.getSlaveSubscriptionConnectionMinimumIdleSize()) {
            throw new IllegalArgumentException("slaveSubscriptionConnectionMinimumIdleSize can't be lower than slaveSubscriptionConnectionPoolSize");
        }
    }

    private ObjectMapper createMapper(JsonFactory mapping) {
        ObjectMapper mapper = new ObjectMapper(mapping);
        mapper.addMixIn(MasterSlaveServersConfig.class, MasterSlaveServersConfigMixIn.class);
        mapper.addMixIn(SingleServerConfig.class, SingleSeverConfigMixIn.class);
        mapper.addMixIn(Config.class, ConfigMixIn.class);
        mapper.addMixIn(CodecProvider.class, ConfigMixIn.class);
        mapper.addMixIn(ResolverProvider.class, ConfigMixIn.class);
        mapper.addMixIn(Codec.class, ClassMixIn.class);
        mapper.addMixIn(RedissonNodeInitializer.class, ClassMixIn.class);
        mapper.addMixIn(LoadBalancer.class, ClassMixIn.class);
        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("classFilter", SimpleBeanPropertyFilter.filterOutAllExcept());
        mapper.setFilterProvider(filterProvider);
        mapper.setSerializationInclusion(Include.NON_NULL);
        return mapper;
    }

}
