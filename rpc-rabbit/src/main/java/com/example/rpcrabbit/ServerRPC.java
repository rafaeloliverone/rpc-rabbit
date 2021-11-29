package com.example.rpcrabbit;

import com.rabbitmq.client.*;

public class ServerRPC {
    private static final String NAME_FILA = "fila_rpc";

    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(NAME_FILA, false, false, false, null);
            channel.queuePurge(NAME_FILA);

            channel.basicQos(1);

            System.out.println(" [x] Esperando requests RPC");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";

                try {
                    String message = new String(delivery.getBody(), "UTF-8");
                    int n = Integer.parseInt(message);

                    System.out.println(" [] Quantidade iterações(" + message + ") RAFAEL OLIVEIRA");
                    response += fib(n);
                    response += "\nRAFAEL OLIVEIRA";
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e.toString());
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            channel.basicConsume(NAME_FILA, false, deliverCallback, (consumerTag -> {
            }));

            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
