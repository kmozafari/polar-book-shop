package com.polarbookshop.orderservice.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BookClient {

    private final WebClient webClient;

    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient.get()
                .uri("/books/" + isbn)
                .retrieve()
                .bodyToMono(Book.class);
    }
}
