package com.springkafka.app8;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.springkafka.CommonConfiguration;
import com.springkafka.ConfigProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;

@SpringBootApplication
@Import({ CommonConfiguration.class, ConfigProperties.class })
@EnableKafka
public class S1pKafkaApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(S1pKafkaApplication.class)
			.web(WebApplicationType.SERVLET)
			.run(args);
		TestBean testBean = context.getBean(TestBean.class);
		testBean.send("foo");
		context.getBean(Listener.class).latch.await(60, TimeUnit.SECONDS);
		context.close();
	}

	@Bean
	public TestBean test() {
		return new TestBean();
	}

	@Bean
	public Listener listener() {
		return new Listener();
	}

	@Bean
	public KafkaMessageListenerContainer<String, String> container(
			ConsumerFactory<String, String> consumerFactory,
			ConfigProperties config) {
		ContainerProperties containerProperties = new ContainerProperties(config.getTopic());
		containerProperties.setMessageListener(listener());
		return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
	}

	public static class TestBean {

		@Autowired
		private ConfigProperties configProperties;

		@Autowired
		private KafkaTemplate<String, String> template;

		public void send(String foo) {
			this.template.send(this.configProperties.getTopic(), foo);
		}

	}

	public static class Listener implements MessageListener<String, String> {

		private final CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void onMessage(ConsumerRecord<String, String> record) {
			System.out.println("Received: " + record.value());
			this.latch.countDown();
		}

	}

}
