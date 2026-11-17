package com.arc_e_tect.payments.contract;

import com.arc_e_tect.payments.PaymentsApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

/**
 * The service, started on a random port as it is configured to run, which the generated contract
 * tests send their cases to: the contract's paths are relative to its server, {@code /payments}.
 */
@SpringBootTest(classes = PaymentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(RestDocumentationExtension.class)
abstract class RunningService {

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeEach
    void bindTheClient(RestDocumentationContextProvider documentation) {
        client = WebTestClient.bindToServer(new JdkClientHttpConnector())
                .baseUrl("http://localhost:" + port + "/payments")
                .filter(documentationConfiguration(documentation))
                .build();
    }

    public WebTestClient client() {
        return client;
    }
}
