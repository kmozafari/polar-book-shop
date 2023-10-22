package com.polarbookshop.orderservice.order.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class OrderRequestJsonTests {

    @Autowired
    private JacksonTester<OrderRequest> jacksonTester;

    @Test
    public void testSerialize() throws IOException {
        OrderRequest request = new OrderRequest("1234567890", 3);
        JsonContent<OrderRequest> jsonContent = jacksonTester.write(request);
        assertThat(jsonContent).extractingJsonPathStringValue("@.isbn").isEqualTo("1234567890");
        assertThat(jsonContent).extractingJsonPathNumberValue("@.quantity").isEqualTo(3);
    }

    @Test
    public void testDeserialize() throws IOException {
        String orderRequestJson = """
                {
                    "isbn":"1234567890",
                    "quantity":3
                }
                """;
        ObjectContent<OrderRequest> orderRequest = jacksonTester.parse(orderRequestJson);
        assertThat(orderRequest).usingRecursiveComparison()
                .isEqualTo(new OrderRequest("1234567890", 3));
    }
}
