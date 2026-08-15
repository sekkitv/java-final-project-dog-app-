package com.zuzdog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

// @EnableJms turns on scanning for @JmsListener methods (used later by SwipeConsumer).
@Configuration
@EnableJms
public class JmsConfig {

    //we define the swipe queue 
    public static final String SWIPE_QUEUE = "SWIPE_QUEUE";

    //give the value of the broker who we sent to , url=tcp://localhost:61616 
    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;


    //function that create the connection 
    @Bean
    public ConnectionFactory connectionFactory() {
        return new ActiveMQConnectionFactory(brokerUrl);
    }


    //set the jms set message converter and the destiniton name 
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter); //tell java take the message create json 
        template.setDefaultDestinationName(SWIPE_QUEUE);
        return template;
    }

    // Converts our SwipeMessage Java object <-> JSON automatically, so the producer
    // and consumer both just work with plain Java objects, never raw JMS text.
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();//convoter to json 
        converter.setObjectMapper(objectMapper); // the java obj converter 
        converter.setTargetType(MessageType.TEXT); //send the json as a txt not bytes 
        converter.setTypeIdPropertyName("_type"); //adds a secret filed that tell from which java obj he got it 
        return converter;
    }
}