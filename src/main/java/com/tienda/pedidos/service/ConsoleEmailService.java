package com.tienda.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.info("Notificación por email enviada a: {} | Asunto: {}\nContenido:\n{}", destinatario, asunto, cuerpo);
    }
}
