package com.polarbookshop.orderservice.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OrderRequestValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    public void whenAllFieldsCorrectThenValidationSucceeds() {
        OrderRequest request = new OrderRequest("1234567890", 3);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    public void whenIsbnFieldIsBlankThenValidationFails() {
        OrderRequest request = new OrderRequest("", 3);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book ISBN must be defined.");
    }

    @Test
    public void whenIsbnFieldIsNullThenValidationFails() {
        OrderRequest request = new OrderRequest(null, 3);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book ISBN must be defined.");
    }

    @Test
    public void whenQuantityFieldIsNullThenValidationFails() {
        OrderRequest request = new OrderRequest("1234567890", null);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book quantity must be defined.");
    }

    @Test
    public void whenQuantityFieldIsLessThanMinThenValidationFails() {
        OrderRequest request = new OrderRequest("1234567890", 0);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("You must order at least 1 item.");
    }

    @Test
    public void whenQuantityFieldIsGreaterThanMaxThenValidationFails() {
        OrderRequest request = new OrderRequest("1234567890", 6);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("You cannot order more than 5 items.");
    }
}
