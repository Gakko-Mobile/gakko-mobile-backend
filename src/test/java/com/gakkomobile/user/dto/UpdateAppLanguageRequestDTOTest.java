package com.gakkomobile.user.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.gakkomobile.user.AppLanguage;
import org.junit.jupiter.api.Test;

class UpdateAppLanguageRequestDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReadPolishAndEnglish() throws Exception {

        assertThat(objectMapper.readValue("{\"language\":\"PL\"}", UpdateAppLanguageRequestDTO.class).language())
                .isEqualTo(AppLanguage.PL);
        assertThat(objectMapper.readValue("{\"language\":\"EN\"}", UpdateAppLanguageRequestDTO.class).language())
                .isEqualTo(AppLanguage.EN);
    }

    @Test
    void shouldRejectAnUnknownLanguage() {

        assertThatThrownBy(() -> objectMapper.readValue("{\"language\":\"DE\"}", UpdateAppLanguageRequestDTO.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
