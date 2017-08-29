package br.com.jmsstudio.camel;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaJMSInOut {

    public static void main(String[] args) throws Exception {

        CamelContext context = new DefaultCamelContext();
        context.addComponent("activemq", ActiveMQComponent.activeMQComponent("tcp://localhost:61616"));

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("activemq:queue:fila.pedidos.req")
                    .log("Pattern: ${exchange.pattern}")
                    .log("${body}")
                    .setHeader(Exchange.FILE_NAME, constant("message.txt"))
                .to("file:saida");
            }
        });

        context.start();
        Thread.sleep(10000);
        context.stop();

    }
}
