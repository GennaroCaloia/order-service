package com.example.orderservice.controller;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderStatusRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.OrderSummaryResponse;
import com.example.orderservice.dto.response.PagedResponse;
import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.repository.specification.OrderFilter;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Gestione ordini")
public class OrderController {

    private final OrderService orderService;

    // ── POST /api/v1/orders ───────────────────────────────────

    @Operation(
            summary = "Crea un nuovo ordine",
            description = "Crea un ordine in stato PENDING con gli item specificati."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordine creato",
                    headers = @Header(name = "Location",
                            description = "URI del nuovo ordine",
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Request non valida",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "422", description = "Errore di business")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // ── GET /api/v1/orders ────────────────────────────────────

    @Operation(
            summary = "Lista ordini paginata",
            description = "Restituisce gli ordini filtrabili per customerId, status e intervallo di date."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista ordini"),
            @ApiResponse(responseCode = "400", description = "Parametri non validi")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getOrders(

            @Parameter(description = "Filtra per customer")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Filtra per status")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Data inizio (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @Parameter(description = "Data fine (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @Parameter(description = "Numero pagina (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Dimensione pagina (max 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Campo di ordinamento")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Direzione ordinamento")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        // Costruzione Pageable — cap a 100 per evitare query troppo grandi
        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(
                page, cappedSize,
                Sort.by(direction, sortBy));

        OrderFilter filter = OrderFilter.builder()
                .customerId(customerId)
                .status(status)
                .from(from)
                .to(to)
                .build();

        Page<OrderSummaryResponse> result =
                orderService.getOrders(filter, pageable);

        return ResponseEntity.ok(PagedResponse.from(result));
    }

    // ── GET /api/v1/orders/{id} ───────────────────────────────

    @Operation(summary = "Dettaglio ordine")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordine trovato"),
            @ApiResponse(responseCode = "404", description = "Ordine non trovato")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "ID dell'ordine")
            @PathVariable UUID id) {

        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // ── PATCH /api/v1/orders/{id}/status ─────────────────────

    @Operation(
            summary = "Aggiorna lo stato di un ordine",
            description = "Applica una transizione di stato secondo la state machine."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stato aggiornato"),
            @ApiResponse(responseCode = "404", description = "Ordine non trovato"),
            @ApiResponse(responseCode = "409", description = "Transizione di stato non consentita"),
            @ApiResponse(responseCode = "422", description = "Stato non cancellabile")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(id, request));
    }

    // ── DELETE /api/v1/orders/{id} ────────────────────────────

    @Operation(
            summary = "Cancella un ordine",
            description = "Cancella logicamente un ordine. Solo PENDING e CONFIRMED sono cancellabili."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordine cancellato"),
            @ApiResponse(responseCode = "404", description = "Ordine non trovato"),
            @ApiResponse(responseCode = "422", description = "Ordine non cancellabile nello stato attuale")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @Parameter(description = "Motivo della cancellazione")
            @RequestParam(required = false) String reason) {

        orderService.cancelOrder(id, reason);
        return ResponseEntity.noContent().build();
    }
}