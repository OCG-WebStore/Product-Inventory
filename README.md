# OCG WebStore: Product Service 🛍️

[![Scala Version](https://img.shields.io/badge/Scala-2.13-red.svg)](https://www.scala-lang.org/)
[![Play Framework](https://img.shields.io/badge/Play_Framework-3.3.3-blue.svg)](https://playframework.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE) <br>
[![codecov](https://codecov.io/gh/OCG-WebStore/Product-Inventory/graph/badge.svg?token=Q74C4E0VZD)](https://codecov.io/gh/OCG-WebStore/Product-Inventory)
#### Core component of the OCG WebStore high-end fashion platform. Handles product catalog management with real-time inventory updates.

## 📋 Features Checklist
### Core Features
- [x] Product CRUD operations (Admin only)
- [x] GraphQL API for product queries
- [x] Postgres DB for catalog data persistence
- [x] Redis caching layer
- [ ] Kafka event streaming
- [ ] Circuit Breaker pattern
- [ ] Bulk import/export

### Security
- [X] Admin and User role enforcement
- [ ] Secure image uploads (pre-signed S3 URLs)
- [ ] Audit logging
- [ ] Request signing

### Observability
- [x] Application logging
- [x] Health checks
- [ ] Prometheus metrics
- [ ] Loki logging
- [ ] Distributed tracing

### Infrastructure
- [x] Docker containerization
- [ ] Kubernetes deployment
- [ ] Auto-scaling

## Service Integrations Diagram
![Service Integrations Diagram should appear here](https://github.com/OCG-WebStore/Product-Inventory/blob/master/files/Integrations.png?raw=true)