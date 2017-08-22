package br.com.jmsstudio.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaPedidos {

	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();

		context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file://pedidos?delay=5s&noop=true")
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
            }
        });

        context.start();
        Thread.sleep(10000);
        context.stop();
	}	
}
