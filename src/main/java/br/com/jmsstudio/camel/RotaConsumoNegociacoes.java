package br.com.jmsstudio.camel;

import br.com.jmsstudio.camel.model.Negociacao;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.thoughtworks.xstream.XStream;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import java.text.SimpleDateFormat;

public class RotaConsumoNegociacoes {

    public static void main(String[] args) throws Exception {
        SimpleRegistry simpleRegistry = new SimpleRegistry();
        simpleRegistry.put("mysql", createDataSource());

        DefaultCamelContext context = new DefaultCamelContext(simpleRegistry);

        final XStream xStream = new XStream();
        xStream.processAnnotations(Negociacao.class);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer://negociacoes?fixedRate=true&delay=1s&period=360s")
                    .to("http4://argentumws.caelum.com.br/negociacoes")
                    .convertBodyTo(String.class)
                    .unmarshal(new XStreamDataFormat(xStream))
                    .split(body())
                    .process(exchange -> {
                        Negociacao negociacao = exchange.getIn().getBody(Negociacao.class);
                        exchange.setProperty("preco", negociacao.getPreco());
                        exchange.setProperty("quantidade", negociacao.getQuantidade());

                        String data = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(negociacao.getData().getTime());
                        exchange.setProperty("data", data);
                    })
                    .setBody(simple("insert into negociacao(preco, quantidade, data) values (${property.preco}, ${property.quantidade}, '${property.data}')"))
                    .log("${id} - ${body}")
                .delay(1000)
                .to("jdbc:mysql");
//                .setHeader(Exchange.FILE_NAME, constant("negociacoes.xml"))
//                .to("file://saida");
            }
        });

        context.start();
        Thread.sleep(10000);
        context.stop();
    }

    private static MysqlConnectionPoolDataSource createDataSource() {
        MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setDatabaseName("camel");
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setUser("root");
        dataSource.setPassword("12345");

        return dataSource;
    }
}
