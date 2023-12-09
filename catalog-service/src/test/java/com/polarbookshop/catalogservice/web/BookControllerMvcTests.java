package com.polarbookshop.catalogservice.web;

import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarbookshop.catalogservice.config.SecurityConfig;
import com.polarbookshop.catalogservice.domain.Book;
import com.polarbookshop.catalogservice.domain.BookNotFoundException;
import com.polarbookshop.catalogservice.domain.BookService;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)
public class BookControllerMvcTests {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private BookService bookService;

    @MockBean private JwtDecoder jwtDecoder;

    @Test
    void whenGetBookExistingAndAuthenticatedThenShouldReturn200() throws Exception {
        String isbn = "1234567890";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.viewBookDetails(isbn)).willReturn(book);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/books/" + isbn)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority("employee"))))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void whenGetBookExistingAndUnauthenticatedThenShouldReturn200() throws Exception {
        String isbn = "1234567890";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.viewBookDetails(isbn)).willReturn(book);
        mockMvc.perform(MockMvcRequestBuilders.get("/books/" + isbn))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void whenGetBookNotExistingAndAuthenticatedThenShouldReturn404() throws Exception {
        String isbn = "1234567890";
        given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/books/" + isbn)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority("employee"))))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void whenGetBookNotExistingAndUnauthenticatedThenShouldReturn404() throws Exception {
        String isbn = "1234567890";
        given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
        mockMvc.perform(MockMvcRequestBuilders.get("/books/" + isbn))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void whenBootExistingThenShouldReturn404() throws Exception {
        String isbn = "1234567890";
        given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
        mockMvc.perform(MockMvcRequestBuilders.get("/books/" + isbn))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void whenDeleteBookWithEmployeeRoleThenReturn204() throws Exception {
        String isbn = "1234567543";
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/books/" + isbn)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_employee"))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void whenDeleteBookWithCustomerRoleThenReturn403() throws Exception {
        String isbn = "1234567543";
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/books/" + isbn)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_customer"))))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void whenDeleteBookNotAuthenticatedThenReturn401() throws Exception {
        String isbn = "1234567543";
        mockMvc.perform(MockMvcRequestBuilders.delete("/books/" + isbn))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void whenPostBookWithEmployeeRoleThenReturn201() throws Exception {
        String isbn = "1234567895";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.addBookToCatalog(book)).willReturn(book);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(book))
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_employee"))))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void whenPostBookWithCustomerRoleThenReturn403() throws Exception {
        String isbn = "1234567895";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.addBookToCatalog(book)).willReturn(book);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(book))
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_customer"))))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void whenPostBookAndUnauthenticatedThenReturn401() throws Exception {
        String isbn = "1234567895";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.addBookToCatalog(book)).willReturn(book);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(book)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void whenPutBookWithEmployeeRoleThenReturn200() throws Exception {
        String isbn = "1234567895";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.editBookDetails(isbn, book)).willReturn(book);
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/books/" + isbn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(book))
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_employee"))))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void whenPutBookWithCustomerRoleThenReturn403() throws Exception {
        String isbn = "1234567895";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.editBookDetails(isbn, book)).willReturn(book);
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/books/" + isbn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(book))
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_customer"))))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void whenPutBookAndUnauthenticatedThenReturn401() throws Exception {
        String isbn = "1234567895";
        Book book = Book.of(isbn, "book1", "author1", 12.3, "publisher1");
        given(bookService.editBookDetails(isbn, book)).willReturn(book);
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/books/" + isbn)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(book)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
