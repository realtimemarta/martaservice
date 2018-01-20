package org.marta;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RealTimeBase {
	
	private static final transient String REDIS_HOST = (System.getenv("ELASTICACHE_HOST") == null
			|| System.getenv("ELASTICACHE_HOST").isEmpty()) ? "localhost" : System.getenv("ELASTICACHE_HOST");
	private static final transient String REDIS_PORT = (System.getenv("ELASTICACHE_PORT") == null
			|| System.getenv("ELASTICACHE_PORT").isEmpty()) ? "6379" : System.getenv("ELASTICACHE_PORT");

	
	protected JedisPool getPoolFromRedis() {

		JedisPool pool = null;

		int i = 0;
		int connectionRetry = 3;

		while (pool == null && i < connectionRetry) {
			try {
				pool = new JedisPool(REDIS_HOST, Integer.parseInt(REDIS_PORT));
			} catch (Exception e) {
				System.out.println("Jedis connection exception: tentative n. " + i);
			}
			i++;
		}
		i = 0;
		Jedis j = null;
		while (pool != null && j == null && i < connectionRetry) {
			try {
				j = pool.getResource();
			} catch (Exception e) {
				System.out.println("Jedis connection exception: tentative n. " + i);
			}
			i++;
		}

		i = 0;
		while (j != null && !j.isConnected() && i < connectionRetry) {
			try {
				j.connect();
			} catch (Exception e) {
				System.out.println("Jedis connection exception: tentative n. " + i);
			}
			i++;
		}
		return pool;
	}

	protected Jedis getJedisFromPool(JedisPool pool) {
		int i = 0;
		int connectionRetry = 3;
		Jedis j = null;
		while (pool != null && j == null && i < connectionRetry) {
			try {
				j = pool.getResource();
			} catch (Exception e) {
				System.out.println("Jedis connection exception: tentative n. " + i);
			}
			i++;
		}

		i = 0;
		while (j != null && !j.isConnected() && i < connectionRetry) {
			try {
				j.connect();
			} catch (Exception e) {
				System.out.println("Jedis connection exception: tentative n. " + i);
			}
			i++;
		}
		return j;
	}

}
