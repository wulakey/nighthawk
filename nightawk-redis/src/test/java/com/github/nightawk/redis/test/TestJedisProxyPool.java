package com.github.nightawk.redis.test;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler;
import com.github.kristofa.brave.Sampler;
import com.github.kristofa.brave.http.HttpSpanCollector;
import com.github.nightawk.redis.JaRedis;
import com.github.nightawk.redis.JaRedisInterceptor;
import com.github.nightawk.redis.JaRedisPool;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

public class TestJedisProxyPool {

    @Test
    public void test_JaRedis(){
        JaRedis.Builder builder = new JaRedis.Builder();
        Jedis jedis = builder.build();
        String key = UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        jedis.set(key, value);
        Assert.assertEquals(jedis.get(key), value);
        jedis.close();
    }

    @Test
    public void test_jedis_proxy() {
        JedisPoolConfig config = new JedisPoolConfig();
        JaRedisPool pool = new JaRedisPool(config, "127.0.0.1", 6379);
        Jedis jedis = pool.getResource();
        jedis.set("hello", "world");
        Assert.assertEquals(jedis.get("hello"), "world");
        jedis.close();
    }

    @Test
    public void test_jedis_tracer() throws Exception {
        Brave.Builder builder = new Brave.Builder("jedis-interceptor-test");
        builder.spanCollector(HttpSpanCollector.create("http://192.168.150.132:9411", new EmptySpanCollectorMetricsHandler()));
        builder.traceSampler(Sampler.ALWAYS_SAMPLE);
        Brave brave = builder.build();
        JaRedisInterceptor.setClientTracer(brave.clientTracer());

        JedisPoolConfig config = new JedisPoolConfig();
        JaRedisPool proxyPool = new JaRedisPool(config, "127.0.0.1", 6379);
        Jedis jedis = proxyPool.getResource();
        jedis.set("hello", "world");
        Assert.assertEquals(jedis.get("hello"), "world");
        jedis.hgetAll("hello-map");
        jedis.close();
        // sleep 3s in case spanCollector not flushed
        Thread.sleep(3000);
    }
}
