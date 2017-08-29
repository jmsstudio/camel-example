package br.com.jmsstudio.camel;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.validation.SchemaValidationException;

public class RotaPedidos {

	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();

        context.addComponent("activemqJMS", ActiveMQComponent.activeMQComponent("tcp://localhost:61616"));


		context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                onException(SchemaValidationException.class)
                    .handled(true)
                    .maximumRedeliveries(3)
                    .redeliveryDelay(5000)
                    .onRedelivery(e -> {
                        int counter = (int) e.getIn().getHeader(Exchange.REDELIVERY_COUNTER);
                        int max = (int) e.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER);
                        System.out.println("Redelivering... " + counter + "/" + max);
                    });

                errorHandler(
//                    deadLetterChannel("file:erro")
                    deadLetterChannel("activemqJMS:queue:fila.pedidos.DLQ")
                    .redeliveryDelay(3000)
                    .maximumRedeliveries(3)
                    .onRedelivery(e -> {
                        int counter = (int) e.getIn().getHeader(Exchange.REDELIVERY_COUNTER);
                        int max = (int) e.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER);
                        System.out.println("Redelivering... " + counter + "/" + max);
                    })
                );

//                from("file://pedidos?delay=5s&noop=true")
                from("activemqJMS:queue:fila.pedidos")
                        .routeId("route-pedidos")
                .to("validator:pedido.xsd")
                .multicast()
                    .parallelProcessing()
                    .to("direct:http")
                    .to("direct:routeSoap");


                from("direct:http")
                .routeId("route-http")
                    .setProperty("pedidoId", xpath("/pedido/id"))
                    .setProperty("clientId", xpath("/pedido/pagamento/email-titular"))
                    .split()
                        .xpath("/pedido/itens/item")
                    .filter()
                        .xpath("/item/formato[text()='EBOOK']")
                        .setProperty("ebookId", xpath("/item/livro/codigo"))
                    .marshal()
                        .xmljson()
                        .log("${id} - ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
                .setHeader(Exchange.HTTP_QUERY, simple("clienteId=${property.clientId}&pedidoId=${property.pedidoId}&ebookId=${property.ebookId}"))
                .to("http4://localhost:8080/webservices/ebook/item");

                from("direct:routeSoap")
                .routeId("route-soap")
                    .to("xslt:pedido-para-soap.xslt")
                        .log("Template: ${body}")
                    .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
                    .to("http4://localhost:8080/webservices/financeiro");
            }
        });

        context.start();
        Thread.sleep(10000);
        context.stop();
	}	
}
