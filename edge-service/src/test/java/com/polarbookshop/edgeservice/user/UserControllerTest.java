package com.polarbookshop.edgeservice.user;

import com.polarbookshop.edgeservice.config.SecurityConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@WebFluxTest(UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    public void whenNotAuthorizedThenReturn401() {
        webClient
                .get()
                .uri("/user")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void whenAuthenticatedThenReturnUser() {
        var expectedUser = new User("user", "name", "lastname", List.of("employee", "customer"));
        webClient
                .mutateWith(configureMockOidcLogin(expectedUser))
                .get()
                .uri("/user")
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> Assertions.assertThat(user).isEqualTo(expectedUser));
    }

    private SecurityMockServerConfigurers.OidcLoginMutator configureMockOidcLogin(User expectedUser) {
        return SecurityMockServerConfigurers.mockOidcLogin()
                .idToken(builder -> {
                    builder.claim(StandardClaimNames.PREFERRED_USERNAME, expectedUser.username());
                    builder.claim(StandardClaimNames.GIVEN_NAME, expectedUser.firstName());
                    builder.claim(StandardClaimNames.FAMILY_NAME, expectedUser.lastName());
                });
    }
}
