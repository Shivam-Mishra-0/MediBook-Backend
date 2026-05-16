# MediBook Backend

Public-facing README for the `main` branch.

MediBook Backend is a Spring Boot microservices platform for healthcare appointment management. It handles identity, provider onboarding, schedule publishing, appointment booking, payment verification, patient reviews, notifications, medical records, and admin operations behind a single API gateway.

## Overview

- 11 runtime services coordinated through Netflix Eureka and Spring Cloud Gateway
- Token-based security with JWT, OTP-assisted login, and Google OAuth support
- Payment-aware appointment flow where slots are locked only after payment success
- Event-driven notifications through RabbitMQ
- Follow-up automation with scheduled reminders and no-show / slot cleanup jobs
- Shared exception infrastructure through the `medibook-common` module

## Architecture

```mermaid
flowchart LR
    Client[Client App or Frontend] --> Gateway[API Gateway :8080]

    subgraph Platform
        Eureka[Eureka Server :8761]
        MQ[RabbitMQ :5672]
    end

    Gateway --> Auth[Auth Service :8081]
    Gateway --> Provider[Provider Service :8082]
    Gateway --> Schedule[Schedule Service :8083]
    Gateway --> Appointment[Appointment Service :8084]
    Gateway --> Payment[Payment Service :8085]
    Gateway --> Review[Review Service :8086]
    Gateway --> Notification[Notification Service :8087]
    Gateway --> Record[Record Service :8088]
    Gateway --> Admin[Admin Service :9090]

    Auth --> AuthDB[(auth_db)]
    Provider --> ProviderDB[(provider_db)]
    Schedule --> ScheduleDB[(schedule_db)]
    Appointment --> AppointmentDB[(appointment_db)]
    Payment --> PaymentDB[(payment_db)]
    Review --> ReviewDB[(review_db)]
    Notification --> NotificationDB[(notification_db)]
    Record --> RecordDB[(record_db)]
    Admin --> AuthDB

    Appointment -->|slot lookup / book / release| Schedule
    Payment -->|confirm booking state| Appointment
    Review -->|validate completed visit| Appointment
    Review -->|refresh provider rating| Provider
    Record -->|patient profile lookup| Auth
    Record -->|follow-up reminders| Notification
    Appointment -->|appointment events| MQ
    MQ --> Notification

    Gateway -. service discovery .-> Eureka
    Auth -. register .-> Eureka
    Provider -. register .-> Eureka
    Schedule -. register .-> Eureka
    Appointment -. register .-> Eureka
    Payment -. register .-> Eureka
    Review -. register .-> Eureka
    Notification -. register .-> Eureka
    Record -. register .-> Eureka
    Admin -. register .-> Eureka
```

## Booking Lifecycle

```mermaid
sequenceDiagram
    participant User as Patient
    participant Gateway as API Gateway
    participant Appointment as Appointment Service
    participant Schedule as Schedule Service
    participant Payment as Payment Service
    participant Razorpay as Razorpay
    participant MQ as RabbitMQ
    participant Notification as Notification Service

    User->>Gateway: Create appointment request
    Gateway->>Appointment: POST /appointments/book
    Appointment->>Schedule: Read slot
    Schedule-->>Appointment: Slot available
    Appointment-->>User: Appointment created as PENDING_PAYMENT

    User->>Gateway: Initiate payment
    Gateway->>Payment: POST /payments/initiate
    Payment->>Razorpay: Create order
    Razorpay-->>Payment: Order created

    User->>Gateway: Verify payment
    Gateway->>Payment: POST /payments/verify
    Payment->>Appointment: updateStatus(SCHEDULED)
    Appointment->>Schedule: bookSlot(slotId)
    Appointment->>MQ: publish appointment.booked
    MQ->>Notification: consume event
    Notification-->>User: App notification / email flow
```

## Service Landscape

| Module | Port | Data store | Responsibility |
| --- | --- | --- | --- |
| `eureka-server` | `8761` | - | Service registry and discovery |
| `api-gateway` | `8080` | - | Unified entry point, JWT enforcement, route forwarding |
| `auth-service` | `8081` | `auth_db` | Registration, OTP login, JWT issuance, profile and password flows |
| `provider-service` | `8082` | `provider_db` | Provider profiles, availability, verification, rating updates |
| `schedule-service` | `8083` | `schedule_db` | Slot creation, recurring availability, block/unblock, release logic |
| `appointment-service` | `8084` | `appointment_db` | Booking, rescheduling, completion, no-show and status orchestration |
| `payment-service` | `8085` | `payment_db` | Payment initiation, Razorpay verification, refunds, revenue tracking |
| `review-service` | `8086` | `review_db` | Patient reviews, provider averages, review eligibility checks |
| `notification-service` | `8087` | `notification_db` | In-app notifications, email dispatch, RabbitMQ consumers |
| `record-service` | `8088` | `record_db` | Medical records, attachments, follow-up tracking and reminders |
| `admin-service` | `9090` | `auth_db` | Seeded admins, user moderation, admin management APIs |
| `medibook-common` | - | - | Shared exception base classes and response helpers |

## Backend Behaviors Worth Calling Out

- Appointment creation starts in `PENDING_PAYMENT`, which prevents premature slot locking.
- Payment verification promotes the appointment to `SCHEDULED` and then books the slot.
- Completed visits unlock downstream actions such as reviews and medical record completion.
- Review submission recalculates the provider's average rating through a service-to-service call.
- Appointment events are published to RabbitMQ and consumed by notification-service.
- Scheduled jobs currently handle slot expiry, no-show detection, and same-day follow-up reminders.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Cloud Gateway and Netflix Eureka
- Spring Security with JWT
- Spring Data JPA
- MySQL
- RabbitMQ
- OpenFeign
- Razorpay
- Springdoc OpenAPI / Swagger UI
- JUnit 5, Mockito, JaCoCo

## Running Locally

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- RabbitMQ 3.x

### Environment variables

| Variable | Used by | Notes |
| --- | --- | --- |
| `DB_USERNAME` | most services | Defaults to `medibook_user` in several modules |
| `DB_PASSWORD` | most services | Shared local DB credential |
| `JWT_SECRET` | gateway + secured services | Must stay identical across services |
| `EUREKA_DEFAULT_ZONE` | all services | Defaults to `http://admin:medibook123@localhost:8761/eureka/` |
| `MAIL_USERNAME` | auth, notification, record | SMTP sender account |
| `MAIL_PASSWORD` | auth, notification, record | App password for SMTP |
| `GOOGLE_CLIENT_ID` | auth | Optional OAuth login support |
| `GOOGLE_CLIENT_SECRET` | auth | Optional OAuth login support |
| `RAZORPAY_API_KEY` | payment | Live or test key ID |
| `RAZORPAY_KEY_SECRET` | payment | Matching Razorpay secret |

### Build

```bash
mvn clean install
```

### Suggested startup order

```bash
# 1. Service discovery
cd eureka-server && mvn spring-boot:run

# 2. Domain services
cd ../auth-service && mvn spring-boot:run
cd ../provider-service && mvn spring-boot:run
cd ../schedule-service && mvn spring-boot:run
cd ../appointment-service && mvn spring-boot:run
cd ../payment-service && mvn spring-boot:run
cd ../review-service && mvn spring-boot:run
cd ../notification-service && mvn spring-boot:run
cd ../record-service && mvn spring-boot:run
cd ../admin-service && mvn spring-boot:run

# 3. Gateway
cd ../api-gateway && mvn spring-boot:run
```

Once everything is up:

- Eureka dashboard: `http://localhost:8761`
- Gateway base URL: `http://localhost:8080`
- Swagger pattern: `http://localhost:<service-port>/swagger-ui.html`

## API Surface

Gateway route prefixes mirror the service boundaries:

| Prefix | Destination |
| --- | --- |
| `/auth/**` | `auth-service` |
| `/providers/**` | `provider-service` |
| `/slots/**` | `schedule-service` |
| `/appointments/**` | `appointment-service` |
| `/payments/**` | `payment-service` |
| `/reviews/**` | `review-service` |
| `/notifications/**` | `notification-service` |
| `/records/**` | `record-service` |
| `/admin/**` | `admin-service` |

Common public access patterns include registration and login routes in auth-service, provider browsing routes, and available-slot lookup routes.

## Project Structure

```text
MediBook_Backend/
|-- pom.xml
|-- medibook-common/
|-- eureka-server/
|-- api-gateway/
|-- auth-service/
|-- provider-service/
|-- schedule-service/
|-- appointment-service/
|-- payment-service/
|-- review-service/
|-- notification-service/
|-- record-service/
|-- admin-service/
`-- scripts/
```

