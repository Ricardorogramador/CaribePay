package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PhoneService {

    public String normalizarCO(String raw) {
        if (raw == null) {
            log.warn("Intento de normalizar teléfono nulo");
            throw new IllegalArgumentException("Teléfono requerido");
        }

        String s = raw.trim();
        if (s.isEmpty()) {
            log.warn("Intento de normalizar teléfono vacío");
            throw new IllegalArgumentException("Teléfono requerido");
        }

        log.debug("Normalizando teléfono: {}", raw);

        s = s.replaceAll("[\\s\\-()]", "");
        s = s.replaceAll("[^\\d+]", "");

        if (s.startsWith("+57")) {
            String digits = s.substring(3);
            if (digits.length() != 10) {
                log.warn("Teléfono inválido (formato +57): se esperan 10 dígitos, se obtuvieron {}", digits.length());
                throw new IllegalArgumentException("Teléfono inválido (se esperan 10 dígitos después de +57)");
            }
            log.debug("Teléfono normalizado a: +57{}", digits);
            return "+57" + digits;
        }

        if (s.startsWith("57")) {
            String digits = s.substring(2);
            if (digits.length() != 10) {
                log.warn("Teléfono inválido (formato 57): se esperan 10 dígitos, se obtuvieron {}", digits.length());
                throw new IllegalArgumentException("Teléfono inválido (se esperan 10 dígitos después de 57)");
            }
            log.debug("Teléfono normalizado a: +57{}", digits);
            return "+57" + digits;
        }

        String digitsOnly = s.replace("+", "");
        if (digitsOnly.length() == 10) {
            log.debug("Teléfono normalizado a: +57{}", digitsOnly);
            return "+57" + digitsOnly;
        }

        log.warn("Teléfono inválido para Colombia: {} (longitud: {})", raw, digitsOnly.length());
        throw new IllegalArgumentException("Teléfono inválido para Colombia");
    }
}