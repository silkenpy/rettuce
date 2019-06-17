package ir.rkr.rettuce


import com.typesafe.config.ConfigFactory
import io.lettuce.core.RedisURI
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import io.lettuce.core.cluster.RedisClusterClient
import ir.rkr.rettuce.rest.JettyRestServer
import ir.rkr.rettuce.util.randomItem
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.random.Random


const val version = 0.1

/**
 * CacheService main entry point.
 */


fun getAlphaNumericString(n: Int): String {

    // chose a Character random from this String
    val AlphaNumericString = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789"
            + "abcdefghijklmnopqrstuvxyz")

    // create StringBuffer size of AlphaNumericString
    val sb = StringBuilder(n)

    for (i in 0 until n) {

        // generate a random number between
        // 0 to AlphaNumericString variable length
        val index = (AlphaNumericString.length * Math.random()).toInt()

        // add Character one by one in end of sb
        sb.append(AlphaNumericString[index])
    }

    return sb.toString()
}


fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    val config = ConfigFactory.defaultApplication()
    val randomString = ArrayList<String>()


    val minValueSize = config.getInt("redis.minValueSize")
    val maxValueSize = config.getInt("redis.maxValueSize")

    for (i in 0..1000){
        val randy = Math.random()
        randomString.add( getAlphaNumericString((randy*maxValueSize + minValueSize).toInt()))

    }


    JettyRestServer(config)
    logger.info { "BH V$version is ready :D" }

    val hosts = ArrayList<RedisURI>()
    config.getStringList("redis.cluster").forEach { host ->
        hosts.add(RedisURI.create(host, config.getInt("redis.port")))
    }

    val clusterClient = RedisClusterClient.create(hosts)

    val topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
            .dynamicRefreshSources(true)
//            .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT, ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS)
            .adaptiveRefreshTriggersTimeout(Duration.ofMillis(2000))
            .build()


    clusterClient.setOptions(ClusterClientOptions.builder()
//            .maxRedirects(500000)
            .autoReconnect(true)
            .topologyRefreshOptions(topologyRefreshOptions)
            .build())

    clusterClient.setDefaultTimeout(Duration.ofMillis(3000))


    val redis = clusterClient.connect().sync()


//    val Hosts = mutableSetOf(
//            HostAndPort("127.0.0.1", 7000),
//            HostAndPort("127.0.0.1", 7001),
//            HostAndPort("127.0.0.1", 7002),
//            HostAndPort("127.0.0.1", 7003),
//            HostAndPort("127.0.0.1", 7004),
//            HostAndPort("127.0.0.1", 7005))
//
//
//    val jedisCfg = GenericObjectPoolConfig<String>().apply {
//        maxIdle = 20
//        maxTotal = 100
//        minIdle = 5
//        maxWaitMillis = 10000
//
//    }
//
//    val redis = JedisCluster(HostAndPort("127.0.0.1", 7000), 30000, 10000,5,jedisCfg)


    val count = AtomicLong()
    val t1 = System.currentTimeMillis()

    val threadNum = config.getInt("threadNum")
    val recordPerThread = config.getInt("recordPerThread")
    val redisTtl = config.getLong("redis.ttl")

    for (i in 1..threadNum)
    thread {
        for (j in 1..recordPerThread) {
            try {

                val res = redis.setex("t$i:$j",redisTtl, randomString.randomItem().get())
                count.incrementAndGet()

                if (res != "OK") {
                    println(res)
                }

            } catch (e: Exception) {
                println(e)
            }
        }
    }

    while (count.get() != (threadNum * recordPerThread).toLong()) {
        Thread.sleep(100)
    }

    println("Total Time = ${System.currentTimeMillis() - t1}")
//    println(System.currentTimeMillis())
//    val cm = PoolingHttpClientConnectionManager()
//    cm.maxTotal = 200
//    val httpclient = HttpClients.custom().setConnectionManager(cm).build()
//    for (i in 0..100000) {
//        val httpget = HttpGet("http://localhost:7070/ali")
////        httpget.entity = StringEntity("salam")
//        val res = httpclient.execute(httpget)
//        val r = EntityUtils.toString(res.entity)
////        println(r)
//
//    }
//    println(System.currentTimeMillis())

}
