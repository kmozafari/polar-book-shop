package com.polarbookshop.catalogservice.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class BookValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void whenAllFieldsCorrectThenValidateSucceeds() {
        Book book = new Book("1234567890", "Book", "Author of this book", 12.3);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    void whenIsbnNotDefinedThenValidationFails() {
        Book book = new Book(null, "Book", "Author of this book", 12.3);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book ISBN must be defined.");
    }

    @Test
    void whenIsbnDefinedButIncorrectThenValidationFailed() {
        Book book = new Book("123456789", "Book", "Author of this book", 12.3);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        Assertions.assertEquals(1, violations.size());
        Assertions.assertEquals("The ISBN format must be valid.", violations.iterator().next().getMessage());
    }

    @Test
    void whenTitleNotDefinedThenValidationFails() {
        Book book = new Book("1234567890", null, "Author of this book", 12.3);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book title must be defined.");
    }

    @Test
    void whenAuthorNotDefinedThenValidationFails() {
        Book book = new Book("1234567890", "title", null, 12.3);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book author must be defined.");
    }

    @Test
    void whenPriceNotDefinedThenValidationFails() {
        Book book = new Book("1234567890", "title", "author", null);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book price must be defined.");
    }

    @Test
    void whenPriceDefinedButZeroThenValidationFails() {
        Book book = new Book("1234567890", "title", "author", 0.0);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book price must be greater than zero.");
    }

    @Test
    void whenPriceDefinedButNegativeThenValidationFails() {
        Book book = new Book("1234567890", "title", "author", -2.3);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book price must be greater than zero.");
    }
}
