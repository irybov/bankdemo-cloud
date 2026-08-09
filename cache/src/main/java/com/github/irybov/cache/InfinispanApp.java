// package com.github.irybov.cache;

// import org.infinispan.configuration.cache.CacheMode;
// import org.infinispan.configuration.cache.Configuration;
// import org.infinispan.configuration.cache.ConfigurationBuilder;
// import org.infinispan.manager.EmbeddedCacheManager;
// import org.infinispan.server.hotrod.HotRodServer;
// import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
// import org.infinispan.spring.starter.embedded.InfinispanCacheConfigurer;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.cache.autoconfigure.metrics.CacheMetricsAutoConfiguration;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.context.annotation.Bean;

// @SpringBootApplication(exclude = {CacheMetricsAutoConfiguration.class})
// @EnableCaching
// public class InfinispanApp {

// 	public static void main(String[] args) 
// 	{
// 		SpringApplication.run(App.class, args);
// 	}
// /* 
//     @Bean
//     public CommandLineRunner CommandLineRunnerBean() {
//         return (args) -> {Thread.currentThread().join();};
//     }
// */
//     @Bean
//     public InfinispanCacheConfigurer cacheConfigurer() {
//         return cacheManager -> {

// 			Configuration accountsCacheConfig = new ConfigurationBuilder()
// 			.clustering().cacheMode(CacheMode.LOCAL)
// 			.build();
// 			cacheManager.defineConfiguration("accounts", accountsCacheConfig);
            
//             Configuration billsCacheConfig = new ConfigurationBuilder()
//                     .clustering().cacheMode(CacheMode.LOCAL)
//                     .build();
//             cacheManager.defineConfiguration("bills", billsCacheConfig);

//             Configuration dtosDataConfig = new ConfigurationBuilder()
//                     .clustering().cacheMode(CacheMode.LOCAL)
//                     .build();
//             cacheManager.defineConfiguration("dtos", dtosDataConfig);
            
//             Configuration setsDataConfig = new ConfigurationBuilder()
//                     .clustering().cacheMode(CacheMode.LOCAL)
//                     .build();
//             cacheManager.defineConfiguration("sets", setsDataConfig);
//         };
//     }

//     @Bean(destroyMethod = "stop")
//     public HotRodServer hotRodServer(EmbeddedCacheManager cacheManager) {

//         HotRodServerConfigurationBuilder serverBuilder = new HotRodServerConfigurationBuilder();
//         serverBuilder.host("0.0.0.0").port(11222); 
        
//         HotRodServer server = new HotRodServer();
//         server.start(serverBuilder.build(), cacheManager);
        
//         return server;
//     }

// }
