.PHONY: up down logs build test clean

# Avvia l'intero stack
up:
	docker compose up -d
	@echo "Stack avviato:"
	@echo "  App:    http://localhost:8080/swagger-ui.html"
	@echo "  Zipkin: http://localhost:9411"

# Ferma e rimuove container + volumi
down:
	docker compose down -v

# Ferma senza rimuovere i volumi (preserva il DB)
stop:
	docker compose down

# Log in real-time del solo order-service
logs:
	docker compose logs -f order-service

# Build dell'immagine senza cache
build:
	docker compose build --no-cache order-service

# Esegui solo i test (senza Docker)
test:
	mvn test -Dspring.profiles.active=local

# Esegui solo gli integration test
itest:
	mvn failsafe:integration-test -Dspring.profiles.active=test

# Rebuild e restart del solo order-service (hot redeploy)
redeploy:
	docker compose build order-service
	docker compose up -d --no-deps order-service

# Pulizia completa — rimuove container, volumi e immagini buildiate
clean:
	docker compose down -v --rmi local
	docker volume prune -f

# Connessione diretta al PostgreSQL nel container
psql:
	docker compose exec postgres \
		psql -U orderuser -d orderdb

# Health check manuale
health:
	curl -s http://localhost:8080/actuator/health | python3 -m json.tool