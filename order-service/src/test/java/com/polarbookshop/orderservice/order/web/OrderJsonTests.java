package com.polarbookshop.orderservice.order.web;

import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class OrderJsonTests {

    @Autowired
    private JacksonTester<Order> jacksonTester;

    @Test
    public void testSerialize() throws IOException {
        Instant now = Instant.now();
        Order order = new Order(1L, "1234567890", "book name", 12.3, 3, OrderStatus.ACCEPTED, now, now, 1);
        JsonContent<Order> orderJson = jacksonTester.write(order);
        assertThat(orderJson).extractingJsonPathNumberValue("@.id").isEqualTo(order.id().intValue());
        assertThat(orderJson).extractingJsonPathStringValue("@.bookIsbn").isEqualTo(order.bookIsbn());
        assertThat(orderJson).extractingJsonPathStringValue("@.bookName").isEqualTo(order.bookName());
        assertThat(orderJson).extractingJsonPathNumberValue("@.bookPrice").isEqualTo(order.bookPrice());
        assertThat(orderJson).extractingJsonPathNumberValue("@.quantity").isEqualTo(order.quantity());
        assertThat(orderJson).extractingJsonPathStringValue("@.status").isEqualTo(order.status().name());
        assertThat(orderJson).extractingJsonPathStringValue("@.createdDate").isEqualTo(order.createdDate().toString());
        assertThat(orderJson).extractingJsonPathStringValue("@.lastModifiedDate").isEqualTo(order.lastModifiedDate().toString());
        assertThat(orderJson).extractingJsonPathNumberValue("@.version").isEqualTo(order.version());
    }

    @Test
    public void testDeserialize() throws IOException {
        Instant now = Instant.now();
        String orderJson = """
                {
                    "id":1,
                    "bookIsbn":"1234567890",
                    "bookName":"book name",
                    "bookPrice":12.3,
                    "quantity":3,
                    "status":"REJECTED",
                    "createdDate":"%s",
                    "lastModifiedDate":"%s",
                    "version":1
                }
                """.formatted(now.toString(), now.toString());
        ObjectContent<Order> order = jacksonTester.parse(orderJson);
        assertThat(order)
                .usingRecursiveComparison()
                .isEqualTo(new Order(1L, "1234567890", "book name", 12.3, 3, OrderStatus.REJECTED, now, now, 1));
    }
}
