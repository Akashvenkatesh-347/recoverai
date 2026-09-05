# RecoverAI

> **AI-powered subscription revenue recovery for failed recurring payments.**

RecoverAI is a Spring Boot backend that analyzes failed subscription payments, determines whether recovery is appropriate, applies deterministic safety policies, executes a bounded recovery action, and maintains an audit trail.

The system is designed around one core principle:

> **AI recommends. Deterministic policy decides.**

This prevents an AI recommendation from directly triggering an unsafe recovery action.

---

## 🚀 The Problem

Failed recurring payments create immediate revenue risk for subscription businesses.

A payment failure does not always mean a customer should be contacted or escalated. Some failures are potentially recoverable through a retry, while others require user action or manual intervention.

A recovery system therefore needs to answer:

1. **Why did the payment fail?**
2. **Is the failure potentially recoverable?**
3. **What intervention should be attempted?**
4. **Is that intervention allowed under business rules?**
5. **How many times has recovery already been attempted?**
6. **What happened after the decision?**

RecoverAI addresses these questions through an AI-assisted, policy-controlled recovery workflow.

---

# 💡 Solution

RecoverAI combines:

* AI-powered payment failure analysis
* Deterministic recovery policies
* Bounded retry limits
* Razorpay webhook integration
* Webhook signature verification
* Webhook idempotency
* Recovery execution
* Recovery history and audit records

The AI does **not** have unrestricted authority.

Instead:

```text
Payment Failure
      ↓
AI Recommendation
      ↓
Deterministic Policy
      ↓
Allowed Action
      ↓
Recovery Execution
      ↓
Audit Trail
```

---

# 🏗️ Architecture

```text
                         Razorpay
                            │
                            │ payment.failed
                            ▼
                ┌─────────────────────────┐
                │ Razorpay Webhook        │
                │ Controller              │
                └────────────┬────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │ HMAC-SHA256 Signature    │
                │ Verification             │
                └────────────┬────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │ Webhook Event            │
                │ Idempotency Check        │
                └────────────┬────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │ Webhook Processing       │
                │ Service                  │
                └────────────┬────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │ Find Local Payment       │
                └────────────┬────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │ AI Recovery Analysis     │
                │                          │
                │ Risk + Recommendation    │
                │ + Confidence             │
                └────────────┬────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │ Recovery Policy          │
                │                          │
                │ Retryability             │
                │ Subscription status      │
                │ Retry limit              │
                └────────────┬────────────┘
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
              RETRY_PAYMENT       ESCALATE
                    │                 │
                    └────────┬────────┘
                             ▼
                ┌─────────────────────────┐
                │ Recovery Execution       │
                └────────────┬────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │ Recovery Attempts /      │
                │ Audit History            │
                └─────────────────────────┘

                         PostgreSQL
```

---

# 🤖 AI Decisioning

RecoverAI uses a provider abstraction:

```text
RecoveryAiProvider
        │
        ├── OpenAiRecoveryAiProvider
        │
        └── MockRecoveryAiProvider
```

The AI receives payment and subscription information including:

* Payment amount
* Currency
* Payment status
* Failure reason
* Retry count
* Subscription status
* Plan name

It returns:

```json
{
  "riskLevel": "LOW",
  "reason": "The payment failed because of insufficient funds and may succeed on a later retry.",
  "recommendedAction": "RETRY_PAYMENT",
  "confidence": 0.92
}
```

Supported risk levels:

```text
LOW
MEDIUM
HIGH
```

Supported recommendations:

```text
RETRY_PAYMENT
USER_NOTIFICATION
ESCALATE
NO_ACTION
```

---

# 🛡️ Deterministic Safety Policy

The AI recommendation is only a suggestion.

`RecoveryPolicyService` independently determines what action is actually allowed.

Current policy checks include:

### Payment state

Recovery retry is only allowed when the payment is in:

```text
FAILED
```

### Subscription state

Automatic retry requires an:

```text
ACTIVE
```

subscription.

### Failure reason

Retry is allowed only for failure reasons marked as retryable.

Currently retryable reasons include:

```text
INSUFFICIENT_FUNDS
NETWORK_ERROR
TIMEOUT
```

Non-retryable examples include:

```text
CARD_EXPIRED
INVALID_CARD
PAYMENT_METHOD_DECLINED
UNKNOWN
```

### Retry limit

RecoverAI enforces:

```text
MAX_RETRIES = 3
```

Therefore, even if the AI recommends:

```text
RETRY_PAYMENT
```

the policy layer overrides it when:

```text
retryCount >= 3
```

and produces:

```text
ESCALATE
```

### Example

```text
AI Recommendation
       ↓
RETRY_PAYMENT
       ↓
Policy Check
       ↓
Retry Count = 3
       ↓
Retry Not Allowed
       ↓
Final Action
       ↓
ESCALATE
```

This separation between AI recommendation and deterministic policy is a core design feature of RecoverAI.

---

# 🔄 Recovery Execution

`RecoveryExecutionService` executes the action approved by the policy layer.

Supported actions:

| Action              | Behavior                                                     |
| ------------------- | ------------------------------------------------------------ |
| `RETRY_PAYMENT`     | Records/schedules a bounded retry and increments retry count |
| `USER_NOTIFICATION` | Records that user notification is required                   |
| `ESCALATE`          | Records the payment for manual review                        |
| `NO_ACTION`         | Records that no recovery action is required                  |

The current MVP does **not** initiate a live Razorpay payment charge. The retry implementation represents the recovery action by updating the payment's retry state and recording the attempt.

This keeps the prototype bounded and safe while demonstrating the recovery decision and execution workflow.

---

# 🔐 Razorpay Webhook Integration

RecoverAI exposes:

```text
POST /api/webhooks/razorpay
```

The webhook flow is:

```text
Razorpay
   ↓
Webhook
   ↓
Verify X-Razorpay-Signature
   ↓
Validate Event ID
   ↓
Check Duplicate
   ↓
Process payment.failed
```

## Signature Verification

RecoverAI verifies the Razorpay webhook using:

```text
HMAC-SHA256
```

The signature is calculated against the raw webhook payload using the configured webhook secret.

Invalid signatures return:

```text
401 Unauthorized
```

Valid signatures continue through the recovery pipeline.

## Idempotency

Razorpay webhook deliveries can be delivered more than once.

RecoverAI uses:

```text
x-razorpay-event-id
```

to identify previously processed events.

Processed event IDs are persisted in PostgreSQL with a unique constraint.

Therefore:

```text
First delivery
      ↓
Process event
      ↓
Save event ID


Duplicate delivery
      ↓
Event ID already exists
      ↓
Ignore event
```

The duplicate request returns:

```text
Webhook already processed
```

---

# 📊 Audit Trail

Recovery attempts are stored in PostgreSQL.

Each recovery attempt records information such as:

* Payment
* Action
* Status
* Retry count before execution
* Timestamp
* Result

RecoverAI also provides a recovery history endpoint so previous recovery actions can be inspected.

This provides visibility into what the system decided and what action was executed.

---

# 🔌 API Endpoints

## AI Recovery Analysis

```http
POST /api/ai/recovery
```

Request:

```json
{
  "paymentId": 1
}
```

Returns the AI recovery recommendation.

---

## Recovery Decision

```http
POST /api/recovery/payments/{paymentId}/decision
```

Returns:

* Payment ID
* Risk level
* AI recommended action
* Final allowed action
* Reason
* Confidence

Example:

```json
{
  "paymentId": 2,
  "riskLevel": "LOW",
  "aiRecommendedAction": "RETRY_PAYMENT",
  "finalAction": "ESCALATE",
  "reason": "The payment failed because of insufficient funds and may succeed on a later retry.",
  "confidence": 0.92
}
```

This demonstrates the distinction between:

```text
AI recommendation ≠ final decision
```

---

## Execute Recovery

```http
POST /api/recovery/payments/{paymentId}/execute
```

Request:

```json
{
  "action": "RETRY_PAYMENT"
}
```

The action is passed through the recovery policy before execution.

---

## Recovery History

```http
GET /api/recovery/payments/{paymentId}/history
```

Returns the recovery attempts for the payment, ordered from newest to oldest.

---

## Razorpay Webhook

```http
POST /api/webhooks/razorpay
```

Used to receive Razorpay webhook events such as:

```text
payment.failed
```

The webhook is authenticated using the Razorpay signature and protected against duplicate event processing.

---

# 🧪 Testing

The project includes tests for the recovery policy.

Run:

```bash
mvn clean test
```

The test suite verifies important policy scenarios including:

* Retryable failed payments
* Non-retryable failures
* Retry limits
* Subscription state
* Missing information
* Allowed recovery actions

---

# 🛠️ Tech Stack

| Technology        | Purpose                         |
| ----------------- | ------------------------------- |
| Java 25           | Backend language                |
| Spring Boot 4.1.1 | Application framework           |
| Spring Web MVC    | REST APIs and webhook endpoint  |
| Spring Data JPA   | Persistence                     |
| Hibernate         | ORM                             |
| PostgreSQL        | Database                        |
| Spring AI 2.0.0   | AI integration                  |
| OpenAI            | AI recovery analysis            |
| Maven             | Build and dependency management |
| Lombok            | Boilerplate reduction           |
| Postman           | API and webhook testing         |
| Git / GitHub      | Version control                 |

---

# 📁 Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── recoverai/
│   │           ├── ai/
│   │           │   ├── MockRecoveryAiProvider.java
│   │           │   ├── OpenAiRecoveryAiProvider.java
│   │           │   ├── RecoveryAiController.java
│   │           │   ├── RecoveryAiProvider.java
│   │           │   ├── RecoveryAiRequest.java
│   │           │   ├── RecoveryAiResponse.java
│   │           │   └── RecoveryAiService.java
│   │           │
│   │           ├── customer/
│   │           │
│   │           ├── exception/
│   │           │
│   │           ├── payment/
│   │           │
│   │           ├── recovery/
│   │           │   ├── RecoveryDecisionController.java
│   │           │   ├── RecoveryExecutionService.java
│   │           │   ├── RecoveryPolicyService.java
│   │           │   ├── RecoveryAttempt.java
│   │           │   └── ...
│   │           │
│   │           ├── subscription/
│   │           │
│   │           └── webhook/
│   │               ├── RazorpayWebhookController.java
│   │               ├── RazorpayWebhookService.java
│   │               ├── RazorpayWebhookProcessingService.java
│   │               ├── WebhookEvent.java
│   │               └── WebhookEventRepository.java
│   │
│   └── resources/
│       └── application.properties
│
└── test/
    └── java/
        └── com/
            └── recoverai/
                └── recovery/
                    └── RecoveryPolicyServiceTest.java
```

---

# ⚙️ Getting Started

## Prerequisites

Install:

* Java 25
* Maven
* PostgreSQL
* Git

An OpenAI API key is required for the real AI provider.

For local demonstration, RecoverAI also provides a mock AI profile that does not require live AI API calls.

---

## 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd RecoverAI
```

---

## 2. Create the PostgreSQL database

Create a PostgreSQL database named:

```text
recoverai
```

The application expects:

```text
Host: localhost
Port: 5432
Database: recoverai
```

---

## 3. Configure environment variables

Create the required environment variables:

```text
DB_PASSWORD=your_postgresql_password
OPENAI_API_KEY=your_openai_api_key
RAZORPAY_WEBHOOK_SECRET=your_razorpay_webhook_secret
```

The repository includes:

```text
.env.example
```

as a configuration template.

**Never commit real credentials or webhook secrets to GitHub.**

---

## 4. Run with the real AI provider

Start the application normally:

```bash
mvn spring-boot:run
```

The application will use the OpenAI provider when the `mock-ai` profile is not active.

---

## 5. Run with Mock AI

For local demonstrations without making live AI API calls, activate:

```text
mock-ai
```

For example:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=mock-ai"
```

The mock provider provides deterministic responses based on payment failure conditions.

This makes the recovery workflow reproducible during development and demonstrations.

---

# 🧪 Demonstration Flow

A typical RecoverAI demonstration can be performed using Postman.

### Scenario 1 — Recoverable payment

```text
payment.failed
       ↓
Webhook signature verified
       ↓
New event
       ↓
Payment found
       ↓
AI → RETRY_PAYMENT
       ↓
Policy → RETRY_PAYMENT
       ↓
Recovery executed
       ↓
Attempt recorded
```

### Scenario 2 — Retry limit reached

```text
payment.failed
       ↓
AI → RETRY_PAYMENT
       ↓
Retry count = 3
       ↓
Policy blocks retry
       ↓
Final action → ESCALATE
       ↓
Manual review recorded
```

The second scenario demonstrates the safety boundary between the AI recommendation and the final action.

---

# 🔒 Safety Principles

RecoverAI follows several safeguards:

### 1. AI is advisory

The AI cannot directly choose an unrestricted execution path.

### 2. Retry limit

Automatic retries are capped at:

```text
3 attempts
```

### 3. Failure classification

Only explicitly retryable failure reasons can proceed toward automatic retry.

### 4. Subscription validation

Automatic retry requires an active subscription.

### 5. Webhook authentication

Incoming Razorpay webhooks are verified using HMAC-SHA256 signatures.

### 6. Webhook idempotency

Previously processed Razorpay event IDs are ignored.

### 7. Auditability

Recovery actions are persisted for later inspection.

---

# 📈 Revenue Recovery Model

RecoverAI treats every failed payment as potential revenue at risk.

For a subscription with:

```text
Payment amount = ₹649
```

the system can evaluate whether the payment is suitable for recovery.

The broader objective is:

```text
Revenue at Risk
       ↓
AI Analysis
       ↓
Recovery Decision
       ↓
Bounded Intervention
       ↓
Recovered Revenue
```

For a production system, recovery metrics could include:

* Total revenue at risk
* Recovery attempts
* Successful recoveries
* Recovery rate
* Revenue recovered
* Escalation rate
* Retry success rate

---

## Architecture

![RecoverAI Architecture](docs/architecture.png)

# 🚧 Current MVP Limitations

RecoverAI is currently a backend MVP.

The current implementation does not yet include:

* Live payment charging through the Razorpay Payments API
* Email/SMS notification delivery
* Production job scheduling
* Kafka-based event streaming
* Distributed locking
* Authentication and authorization
* Production observability dashboards
* A frontend dashboard

The current retry execution represents the recovery workflow by updating retry state and recording the attempt.

These are natural next steps for a production deployment.

---

# 🔮 Future Improvements

Potential extensions include:

### Intelligent recovery timing

Learn the best time to retry based on historical payment behavior.

### Customer-level recovery strategies

Adapt interventions based on customer payment history and subscription value.

### Revenue prioritization

Prioritize recovery attempts based on:

```text
Revenue at Risk × Recovery Probability
```

### Real payment execution

Integrate bounded retry actions with the appropriate Razorpay payment APIs.

### Notification channels

Automatically send customer notifications through email, SMS, or other channels.

### Event-driven architecture

Introduce Kafka for high-volume payment event processing.

### Recovery analytics

Build dashboards showing:

* Revenue at risk
* Revenue recovered
* Recovery rate
* Failed payment trends
* Retry performance
* Escalations

---

# 🎯 Design Philosophy

RecoverAI is intentionally designed as more than an AI wrapper.

The architecture separates:

```text
AI Intelligence
       +
Business Rules
       +
Execution
       +
Auditability
```

The AI provides contextual reasoning.

The policy layer provides deterministic control.

The execution layer performs only the permitted action.

The audit layer records what happened.

This makes the system easier to reason about, test, and extend toward production.

---

# 📌 Summary

RecoverAI provides an AI-assisted revenue recovery workflow for failed subscription payments:

```text
Detect
  ↓
Analyze
  ↓
Recommend
  ↓
Validate
  ↓
Recover
  ↓
Audit
```

Its core principle is:

> **AI recommends. Policy decides. Recovery is bounded. Every action is auditable.**
