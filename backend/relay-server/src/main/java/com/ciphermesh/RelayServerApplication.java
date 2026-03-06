package com.ciphermesh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * CipherMesh Relay Server — Main Application Entry Point
 *
 * Security note: This server acts as a pure relay and prekey store.
 * It never decrypts messages. All E2E encryption is handled client-side.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@Slf4j
public class RelayServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RelayServerApplication.class, args);
        log.info("CipherMesh Relay Server started — acting as relay only, no message decryption.");
    }
}
