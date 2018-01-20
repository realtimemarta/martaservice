package org.marta.realtime;

import org.marta.RealTimeBase;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RealtimeDataFlush extends RealTimeBase implements RequestHandler<Object, Object> {

	public Object handleRequest(final Object input, final Context context) {
		try {
			JedisPool pool = getPoolFromRedis();
			flushPool(pool);
		} catch (Exception e) {
			System.out.println("Unable to purge pool");
		}

		return null;
	}

	private void flushPool(JedisPool pool) {
		Jedis jedis = getJedisFromPool(pool);
		jedis.flushAll();
		System.out.println("Purge db passed!");
	}
}
