package br.com.jmsstudio.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RotaTestVelocity {

    public static void main(String[] args) throws Exception {

        CamelContext context = new DefaultCamelContext();

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:entry")
                    .setHeader("data", constant(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                .to("velocity:velocity-template.vm")
                    .log("${body}");
            }
        });

        context.start();
        Thread.sleep(1000);

        ProducerTemplate producerTemplate = context.createProducerTemplate();
        producerTemplate.sendBody("direct:entry", "Testing apache camel with velocity");

        context.stop();
    }
}
