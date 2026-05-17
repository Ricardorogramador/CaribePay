package ricardo.estudio.caribepay.services;

import org.springframework.stereotype.Service;

@Service
public class PhoneService {


    public String normalizarCO(String raw) {
        if (raw == null) throw new IllegalArgumentException("Teléfono requerido");

        String s = raw.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Teléfono requerido");

        s = s.replaceAll("[\\s\\-()]", "");

        if (s.startsWith("+")) {
        } else {
        }

        s = s.replaceAll("[^\\d+]", "");

        if (s.startsWith("+57")) {
            String digits = s.substring(3);
            if (digits.length() != 10) {
                throw new IllegalArgumentException("Teléfono inválido (se esperan 10 dígitos después de +57)");
            }
            return "+57" + digits;
        }

        if (s.startsWith("57")) {
            String digits = s.substring(2);
            if (digits.length() != 10) {
                throw new IllegalArgumentException("Teléfono inválido (se esperan 10 dígitos después de 57)");
            }
            return "+57" + digits;
        }

        String digitsOnly = s.replace("+", "");
        if (digitsOnly.length() == 10) {
            return "+57" + digitsOnly;
        }

        throw new IllegalArgumentException("Teléfono inválido para Colombia");
    }
}