package com.example.orderservice.config.security;

import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Bean usato nelle espressioni @PreAuthorize.
 * Separato da OrderService per evitare dipendenze circolari.
 */
@Service("orderSecurityService")
@RequiredArgsConstructor
public class OrderSecurityService {

    private final OrderRepository orderRepository;

    /**
     * Verifica che l'ordine appartenga al customer autenticato.
     * Restituisce false (invece di lanciare eccezione) perché
     * Spring Security intercetterà il false e risponderà 403.
     */
    @Transactional(readOnly = true)
    public boolean isOwner(UUID orderId, Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return false;
        }

        UUID currentCustomerId = jwtAuth.getPrincipal().customerId();

        return orderRepository.findById(orderId)
                .map(order -> order.getCustomerId().equals(currentCustomerId))
                .orElse(false);     // ordine non trovato → 403, non 404
        // il 404 lo gestisce il service dopo l'auth check
    }
}