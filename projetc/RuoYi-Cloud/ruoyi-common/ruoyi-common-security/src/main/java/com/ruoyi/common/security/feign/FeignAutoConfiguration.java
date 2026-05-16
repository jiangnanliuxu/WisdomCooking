package com.ruoyi.common.security.feign;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.LoadBalancerFeignRequestTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import feign.Client;
import feign.RequestInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Feign 配置注册
 *
 * @author ruoyi
 **/
@Configuration
public class FeignAutoConfiguration
{
    private static final String LOCAL_NON_PROXY_HOSTS =
            "localhost|127.*|0.0.0.0|10.*|172.16.*|172.17.*|172.18.*|172.19.*|172.20.*|172.21.*|172.22.*|172.23.*|172.24.*|172.25.*|172.26.*|172.27.*|172.28.*|172.29.*|172.30.*|172.31.*|192.168.*|*.local|ruoyi-*";

    @Bean
    public static BeanFactoryPostProcessor feignLocalProxyBypassPostProcessor()
    {
        return beanFactory -> {
            System.setProperty("java.net.useSystemProxies", "false");
            mergeNonProxyHosts("http.nonProxyHosts");
            mergeNonProxyHosts("https.nonProxyHosts");
            ProxySelector current = ProxySelector.getDefault();
            if (!(current instanceof LocalServiceDirectProxySelector))
            {
                ProxySelector.setDefault(new LocalServiceDirectProxySelector(current));
            }
        };
    }

    @Bean
    public RequestInterceptor requestInterceptor()
    {
        return new FeignRequestInterceptor();
    }

    @Bean
    @ConditionalOnClass({ LoadBalancerClient.class, FeignBlockingLoadBalancerClient.class })
    @ConditionalOnBean(LoadBalancerClient.class)
    @ConditionalOnMissingBean(Client.class)
    public Client feignClient(LoadBalancerClient loadBalancerClient,
                              LoadBalancerClientFactory loadBalancerClientFactory,
                              List<LoadBalancerFeignRequestTransformer> transformers)
    {
        return new FeignBlockingLoadBalancerClient(new NoProxyFeignClient(), loadBalancerClient,
                loadBalancerClientFactory, transformers);
    }

    @Bean
    @ConditionalOnMissingBean(JacksonJsonHttpMessageConverter.class)
    public HttpMessageConverter<?> feignJacksonJsonHttpMessageConverter(ObjectProvider<JsonMapper> jsonMapperProvider)
    {
        // Spring Boot 4 no longer exposes the default MVC converters as beans, but OpenFeign 5
        // still collects HttpMessageConverter beans to build its decoder/encoder chain.
        JsonMapper jsonMapper = jsonMapperProvider.getIfAvailable();
        return (jsonMapper != null) ? new JacksonJsonHttpMessageConverter(jsonMapper) : new JacksonJsonHttpMessageConverter();
    }

    @Bean
    @ConditionalOnMissingBean(StringHttpMessageConverter.class)
    public HttpMessageConverter<?> feignStringHttpMessageConverter()
    {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    private static void mergeNonProxyHosts(String propertyName)
    {
        String configured = System.getProperty(propertyName);
        Set<String> hosts = new LinkedHashSet<>();
        if (configured != null && !configured.isBlank())
        {
            hosts.addAll(Arrays.asList(configured.split("\\|")));
        }
        hosts.addAll(Arrays.asList(LOCAL_NON_PROXY_HOSTS.split("\\|")));
        System.setProperty(propertyName, String.join("|", hosts));
    }

    private static final class LocalServiceDirectProxySelector extends ProxySelector
    {
        private final ProxySelector delegate;

        private LocalServiceDirectProxySelector(ProxySelector delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public List<Proxy> select(URI uri)
        {
            if (isLocalService(uri))
            {
                return Collections.singletonList(Proxy.NO_PROXY);
            }
            return delegate == null ? Collections.singletonList(Proxy.NO_PROXY) : delegate.select(uri);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe)
        {
            if (delegate != null)
            {
                delegate.connectFailed(uri, sa, ioe);
            }
        }

        private boolean isLocalService(URI uri)
        {
            String host = uri == null ? null : uri.getHost();
            if (host == null || host.isBlank())
            {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            return normalized.equals("localhost")
                    || normalized.equals("0.0.0.0")
                    || normalized.startsWith("127.")
                    || normalized.startsWith("10.")
                    || normalized.startsWith("192.168.")
                    || isPrivate172(normalized)
                    || normalized.endsWith(".local")
                    || normalized.startsWith("ruoyi-");
        }

        private boolean isPrivate172(String host)
        {
            if (!host.startsWith("172."))
            {
                return false;
            }
            String[] parts = host.split("\\.");
            if (parts.length < 2)
            {
                return false;
            }
            try
            {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }
    }

    private static final class NoProxyFeignClient extends Client.Default
    {
        private NoProxyFeignClient()
        {
            super(null, null);
        }

        @Override
        public HttpURLConnection getConnection(URL url) throws IOException
        {
            // Feign carries internal service traffic; global desktop proxies break Nacos-resolved calls.
            return (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        }
    }
}
