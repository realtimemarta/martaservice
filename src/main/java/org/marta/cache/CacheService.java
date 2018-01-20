package org.marta.cache;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

public class CacheService {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final JedisPool pool;
	
	public CacheService(JedisPool pool) {
		this.pool = pool;
	}

	public void put(String key, Object value) {
		put(key, value, null);
	}

	public void put(String key, Object value, Integer ttl) {
		try (Jedis jedis = pool.getResource()) {
			String json = serialize(value);
			jedis.set(key, json);
			if (ttl != null) {
				jedis.expire(key, ttl);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void addToSet(String key, Set<Object> values) {
		try (Jedis jedis = pool.getResource()) {
			String[] jsonValues = toStringArray(values);
			jedis.sadd(key, jsonValues);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public <T> Set<T> getMembers(String key, Supplier<Set<T>> loader, Class<T> out) {
		Set<T> cached = (Set<T>) getMembers(key, out);
		if (cached != null && cached.size() > 0) {
			return cached;
		}
		Set<T> loaded = loader.get();
		if (!loaded.isEmpty()) {
			addToSet(key, (Set<Object>) loaded);
		}
		return loaded;

	}

	public <T> Set<T> getMembers(String key, Class<T> out) {
		try (Jedis jedis = pool.getResource()) {
			Set<String> smembers = jedis.smembers(key);
			if (smembers == null) {
				return null;
			}
			return smembers.stream().map(member -> deserialize(member, out)).collect(toSet());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T get(String key, Class<T> out) {
		try (Jedis jedis = pool.getResource()) {
			String json = jedis.get(key);
			if (json == null) {
				return null;
			}
			return deserialize(json, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T get(String key, Supplier<T> loader, Class<T> out) {
		return get(key, loader, out, null);
	}

	public <T> T get(String key, Supplier<T> loader, Class<T> out, Integer ttl) {
		T cached = get(key, out);
		if (cached != null) {
			return cached;
		}

		T loaded = loader.get();
		put(key, loaded, ttl);
		return loaded;
	}

	public void evict(String... keys) {
		try (Jedis jedis = pool.getResource()) {
			jedis.del(keys);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void bulkSetInsertAndDelete(Map<String, Set<Object>> deletes, Map<String, Set<Object>> inserts) {
		try (Jedis jedis = pool.getResource()) {
			Pipeline pipeline = jedis.pipelined();
			deletes.entrySet().forEach(entry -> pipeline.srem(entry.getKey(), toStringArray(entry.getValue())));
			inserts.entrySet().forEach(entry -> pipeline.sadd(entry.getKey(), toStringArray(entry.getValue())));
			pipeline.sync();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void bulkSetInsert(Map<String, Set<Object>> inserts) {
		try (Jedis jedis = pool.getResource()) {
			Pipeline pipeline = jedis.pipelined();
			inserts.entrySet().forEach(entry -> pipeline.sadd(entry.getKey(), toStringArray(entry.getValue())));
			pipeline.sync();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void bulkObjectInsert(Map<String, Object> inserts) {
		try (Jedis jedis = pool.getResource()) {
			Pipeline pipeline = jedis.pipelined();
			inserts.entrySet().forEach(entry -> pipeline.sadd(entry.getKey(), serialize(entry.getValue())));
			pipeline.sync();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String serialize(Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private String[] toStringArray(Set<Object> values) {
		return values.stream().map(this::serialize).toArray(String[]::new);
	}
	
	private <T> T deserialize(String value, Class<T> out) {
		try {
			return OBJECT_MAPPER.readValue(value, out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
