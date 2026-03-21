# WhatsApp Financial Advisor — Specification

## Overview
MyFinancial WhatsApp Bot is a conversational financial advisor that brings the platform's 6-step financial discovery wizard to WhatsApp. Users get financial health analysis, Form 16 parsing, tax savings insights, and ITR filing capability — all within a chat interface.

**Goal**: Meet users where they already spend time (WhatsApp) → deliver financial analysis → convert to ITR filing via TaxBuddy.

---

## Architecture

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   WhatsApp   │────→│  WhatsApp Cloud  │────→│   Spring Boot    │
│   (User)     │←────│  API / OpenClaw   │←────│   Backend (8081) │
└──────────────┘     └────────┬─────────┘     └────────┬─────────┘
                              │                        │
                        ┌─────┴──────┐          ┌──────┴──────┐
                        │  Webhook   │          │ PostgreSQL  │
                        │  Handler   │          │   (prod)    │
                        └────────────┘          └─────────────┘
                              │
                        ┌─────┴──────┐
                        │ AWS Bedrock│
                        │  (AI/LLM)  │
                        └────────────┘
```

### Component Responsibilities

| Component | Role |
|-----------|------|
| **WhatsApp Cloud API** | Message delivery channel (send/receive text, PDFs, images) |
| **Webhook Handler** | Receives incoming messages, routes to appropriate flow |
| **Conversation State Machine** | Tracks user session: which flow, which step, collected data |
| **Spring Boot Backend** | Business logic: health score calculation, tax computation, data persistence |
| **AWS Bedrock** | AI-powered: intent detection, Form 16 parsing, personalized advice generation |
| **PostgreSQL** | Stores user data, session state, conversation history |

### Data Flow
1. User sends message on WhatsApp
2. WhatsApp Cloud API sends webhook `POST` to our backend
3. Webhook handler identifies user session (by phone number)
4. Conversation state machine determines current flow + step
5. Backend processes the input (direct logic or AI via Bedrock)
6. Response is sent back via WhatsApp Cloud API
7. User data is persisted — accessible on both WhatsApp and web app

---

## Conversation Flows

### Flow 0: Welcome / Main Menu

**Trigger**: User sends any initial message ("Hi", "Hello", etc.)

```
🤖 "Welcome to MyFinancial! 🎉
    I'm your personal financial advisor.
    
    What would you like to do?
    
    1️⃣ Quick Financial Health Check
    2️⃣ Upload Form 16 for Analysis
    3️⃣ Tax Savings Calculator
    4️⃣ File ITR"
```

**Implementation**: WhatsApp Interactive List Message with 4 options.

---

### Flow 1: Quick Financial Health Check

**Duration**: ~3-5 minutes  
**Steps**: 6 conversational questions → Financial Health Score

| Step | Bot Asks | User Replies | Data Captured |
|------|----------|-------------|---------------|
| 1 | "What's your monthly salary? (just the number)" | "85000" | `monthlySalary: 85000` |
| 2 | "Monthly expenses approximately?" | "55000" | `monthlyExpenses: 55000` |
| 3 | "Any loan EMIs? (home/car/personal)" | "25000 home loan" | `totalEmi: 25000`, `loanType: HOME_LOAN` |
| 4 | "Any investments? (MF/SIP/FD/PPF)" | "SIP 10000, PPF 5000" | `investments: [{type: SIP, amount: 10000}, {type: PPF, amount: 5000}]` |
| 5 | "Do you have health insurance?" | "Yes, 5 lakh cover" | `healthInsurance: 500000` |
| 6 | "Your age?" | "30" | `age: 30` |

**Result Message**:
```
🤖 📊 YOUR FINANCIAL HEALTH SCORE: 68/100

   ✅ Savings Rate: 35% (Good!)
   ⚠️ DTI Ratio: 29% (Watch this)
   ✅ Emergency Fund: 5.4 months (Healthy)
   ❌ Insurance: Coverage may be low
   💡 Tax Savings: ₹15,600 more possible!
   
   ━━━━━━━━━━━━━━━━━━━
   📱 Full analysis → myfinancial.in/u/{userId}
   📄 Upload Form 16 → Reply "FORM16"
   📋 File ITR → Reply "FILE"
```

**Health Score Calculation**:
```
Score = weighted_average(
  savingsRate      * 0.30,   // (salary - expenses) / salary
  debtSafety       * 0.20,   // 1 - (EMI / salary), capped
  emergencyFund    * 0.15,   // months of expenses covered
  insuranceCoverage * 0.15,  // coverage vs recommended
  taxOptimization  * 0.20    // deductions used vs available
)
```

---

### Flow 2: Form 16 Upload & Analysis

**Trigger**: User selects option 2 or replies "FORM16"

| Step | Bot Message | User Action |
|------|-------------|-------------|
| 1 | "Send me your Form 16 PDF 📎" | Uploads PDF |
| 2 | "Analyzing your Form 16... ⏳" | (bot processes) |
| 3 | Result with extracted data + insights | User reads |

**Form 16 Parsing** (via AWS Bedrock):
```json
// Input: PDF text content sent to Bedrock Claude
// Output: Structured extraction
{
  "grossSalary": 1200000,
  "totalTds": 105000,
  "standardDeduction": 75000,
  "section80C": 150000,
  "section80D": 25000,
  "hra": 240000,
  "netTaxableIncome": 750000,
  "employerName": "TCS Ltd",
  "financialYear": "2025-26"
}
```

**Result Message**:
```
🤖 ✅ Form 16 Analyzed!

   🏢 Employer: TCS Ltd
   💰 Gross Salary: ₹12,00,000
   🧾 TDS Deducted: ₹1,05,000
   📊 80C Used: ₹1,50,000 / ₹1,50,000

   ━━━ Tax Regime Comparison ━━━
   Old Regime Tax: ₹1,17,000
   New Regime Tax: ₹1,08,800
   💡 New Regime saves you ₹8,200!

   ━━━ Optimization Tips ━━━
   • NPS (80CCD): Invest ₹50K → Save ₹15,600
   • Health Insurance: ₹25K deduction available
   
   Total additional savings possible: ₹23,800
   
   📋 Ready to file ITR? Reply "FILE"
```

---

### Flow 3: Tax Savings Calculator

**Trigger**: User selects option 3

Interactive Q&A asking about current deductions → shows which sections have unused limits → recommends specific investments.

| Question | Purpose |
|----------|---------|
| "Annual salary?" | Base income |
| "80C investments? (PF/ELSS/PPF/LIC)" | Check 80C utilization |
| "Health insurance premium?" | 80D check |
| "Home loan interest?" | Section 24 check |
| "NPS contribution?" | 80CCD(1B) check |

**Output**: Gap analysis showing unused deduction capacity and recommended actions.

---

### Flow 4: File ITR

**Trigger**: User selects option 4 or replies "FILE"

```
🤖 "Great! Let's get your ITR filed. 🎯
    
    We partner with TaxBuddy for secure ITR filing.
    
    ✅ Your MyFinancial data will be pre-filled
    ✅ Expert-verified filing
    ✅ Refund tracking included
    
    📱 Click to start: https://taxbuddy.com/file?ref=myfinancial&data={encryptedPayload}
    
    Need help? Reply HELP for our support team."
```

**Pre-filled Data Payload** (encrypted, sent via URL param):
```json
{
  "name": "...",
  "pan": "...",
  "grossSalary": 1200000,
  "totalTds": 105000,
  "deductions": { "80C": 150000, "80D": 25000 },
  "recommendedRegime": "NEW",
  "source": "myfinancial_whatsapp"
}
```

---

## API Endpoints

### WhatsApp Webhook

```
POST /api/v1/whatsapp/webhook
```
- **Purpose**: Receives incoming messages from WhatsApp Cloud API
- **Validation**: Verifies webhook signature from Meta
- **Payload**: WhatsApp Cloud API message format (text, document, image, interactive)

```
GET /api/v1/whatsapp/webhook
```
- **Purpose**: Webhook verification challenge from Meta
- **Params**: `hub.mode`, `hub.verify_token`, `hub.challenge`

### Session Management

```
GET /api/v1/whatsapp/session/{phoneNumber}
```
- **Returns**: `WhatsAppSessionDTO`
```json
{
  "phoneNumber": "+919876543210",
  "currentFlow": "HEALTH_CHECK",
  "currentStep": 3,
  "collectedData": { "monthlySalary": 85000, "monthlyExpenses": 55000 },
  "userId": "uuid-linked-to-web-account",
  "lastActivity": "2026-03-21T10:00:00",
  "language": "en"
}
```

### Health Score

```
POST /api/v1/whatsapp/health-score
```
- **Payload**: `QuickHealthCheckDTO`
```json
{
  "monthlySalary": 85000,
  "monthlyExpenses": 55000,
  "totalEmi": 25000,
  "investments": [
    { "type": "SIP", "amount": 10000 },
    { "type": "PPF", "amount": 5000 }
  ],
  "healthInsuranceCover": 500000,
  "age": 30
}
```
- **Returns**: `HealthScoreDTO`
```json
{
  "overallScore": 68,
  "savingsRate": { "value": 35.3, "score": 78, "status": "GOOD" },
  "debtSafety": { "value": 29.4, "score": 65, "status": "WARNING" },
  "emergencyFund": { "value": 5.4, "score": 72, "status": "HEALTHY" },
  "insuranceCoverage": { "value": 500000, "score": 40, "status": "LOW" },
  "taxOptimization": { "value": 68, "score": 68, "status": "MODERATE" },
  "tips": [
    "Invest ₹50,000 in NPS → Save ₹15,600 in tax",
    "Increase health insurance to ₹10L for family"
  ],
  "webDashboardUrl": "https://myfinancial.in/u/abc123"
}
```

### Form 16 Parsing

```
POST /api/v1/whatsapp/form16/parse
Content-Type: multipart/form-data
```
- **Payload**: Form 16 PDF file
- **Returns**: `Form16DataDTO`
```json
{
  "employerName": "TCS Ltd",
  "financialYear": "2025-26",
  "grossSalary": 1200000,
  "totalTds": 105000,
  "standardDeduction": 75000,
  "section80C": 150000,
  "section80D": 25000,
  "hra": 240000,
  "netTaxableIncome": 750000,
  "taxOldRegime": 117000,
  "taxNewRegime": 108800,
  "recommendedRegime": "NEW",
  "savingsOpportunities": [
    { "section": "80CCD(1B)", "available": 50000, "potentialSaving": 15600 }
  ]
}
```

### Send Message (Outbound)

```
POST /api/v1/whatsapp/send
```
- **Payload**: `WhatsAppMessageDTO`
```json
{
  "to": "+919876543210",
  "type": "text|interactive|template",
  "body": "Your financial health score is 68/100",
  "buttons": [
    { "id": "form16", "title": "Upload Form 16" },
    { "id": "file_itr", "title": "File ITR" }
  ]
}
```

---

## Database Schema

### `whatsapp_sessions` table

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `phone_number` | VARCHAR(15) | User's WhatsApp number (unique) |
| `user_id` | UUID | FK to `users` table (linked web account) |
| `current_flow` | ENUM | NONE, HEALTH_CHECK, FORM16, TAX_CALC, FILE_ITR |
| `current_step` | INT | Step within current flow |
| `collected_data` | JSONB | Partial data collected during conversation |
| `language` | VARCHAR(5) | Preferred language (en, hi, ta, etc.) |
| `last_activity` | TIMESTAMP | Last message timestamp |
| `created_at` | TIMESTAMP | Session creation time |

### `whatsapp_messages` table (audit log)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `session_id` | UUID | FK to `whatsapp_sessions` |
| `direction` | ENUM | INBOUND, OUTBOUND |
| `message_type` | ENUM | TEXT, DOCUMENT, IMAGE, INTERACTIVE |
| `content` | TEXT | Message content |
| `metadata` | JSONB | WhatsApp message metadata |
| `created_at` | TIMESTAMP | Message timestamp |

---

## Conversation State Machine

```
                    ┌──────────┐
                    │  START   │
                    └────┬─────┘
                         │ "Hi"
                    ┌────▼─────┐
                    │  MENU    │
                    └────┬─────┘
            ┌────────────┼────────────┬───────────┐
            ▼            ▼            ▼           ▼
     ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐
     │ HEALTH   │ │ FORM16   │ │ TAX_CALC │ │FILE_ITR │
     │ _CHECK   │ │ _UPLOAD  │ │          │ │         │
     └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬────┘
          │             │            │             │
     [6 steps]    [upload+parse]  [5 steps]   [redirect]
          │             │            │             │
          ▼             ▼            ▼             ▼
     ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐
     │ RESULT   │ │ RESULT   │ │ RESULT   │ │TAXBUDDY │
     │ + SCORE  │ │ + TIPS   │ │ + TIPS   │ │ HANDOFF │
     └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────────┘
          │             │            │
          └─────────────┴────────────┘
                        │
                   ┌────▼─────┐
                   │  MENU    │ (loop back)
                   └──────────┘
```

### Session Timeout
- Sessions expire after **30 minutes** of inactivity
- On expiry, user is greeted with main menu on next message
- Collected data is preserved and linked to phone number

---

## External Integrations

### WhatsApp Cloud API (Meta)
- **Endpoint**: `https://graph.facebook.com/v21.0/{phone-number-id}/messages`
- **Auth**: Bearer token from Meta Business App
- **Webhook**: Configured at `https://api.myfinancial.in/api/v1/whatsapp/webhook`
- **Rate Limits**: 80 messages/second (Business tier)

### AWS Bedrock (AI)
- **Model**: Claude 3.5 Sonnet (or Haiku for cost optimization)
- **Use Cases**:
  - Intent detection from user messages
  - Form 16 PDF text extraction → structured JSON
  - Personalized financial advice generation
  - Multi-language response translation

### TaxBuddy (ITR Filing Partner)
- **Integration Type**: Affiliate referral with pre-filled data
- **Revenue Model**: Commission per successful ITR filing
- **Data Transfer**: Encrypted payload via URL parameters or partner API

---

## Security & Privacy

| Concern | Mitigation |
|---------|------------|
| PII in messages | Encrypt stored messages at rest (AES-256) |
| Phone number as identifier | Hash phone numbers for internal references |
| Form 16 PDFs | Process in-memory, do not persist raw PDFs |
| TaxBuddy data transfer | Encrypted payload, HTTPS only |
| Webhook verification | Validate Meta signature on every webhook call |
| Session hijacking | Phone number verified by WhatsApp — inherent auth |

---

## Error Handling

| Scenario | Bot Response |
|----------|-------------|
| Invalid number input | "Please enter a valid number. For example: 85000" |
| Unsupported file type | "I can only process PDF files. Please upload your Form 16 as a PDF." |
| Form 16 parse failure | "I couldn't read that PDF clearly. Please ensure it's a clear, unencrypted Form 16." |
| Session timeout | "Welcome back! Let's start fresh. What would you like to do?" |
| Backend error | "Something went wrong on our end. Please try again in a moment. 🙏" |
| Rate limit hit | Queue message, process when available |
