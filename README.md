# Expense Approval Workflow — Machine Coding Submission

**Language:** Java 17  
**Build:** Maven

## Overview
 
It demonstrates:

- Declarative workflow templates (JSON)
- Request creation and submission with metadata
- Ordered, role-based workflow steps and simple conditional steps
- Approver actions (approve / reject) with comments
- State transitions and tracking (PENDING, IN_REVIEW, APPROVED, REJECTED, SKIPPED)
- Tenant isolation and access control
- Preflight validation for missing approvers
- Minimal change-management: **versioned workflow templates** (in-flight instances stick to the version they were created with)

---

## Assumptions (explicit)

- Templates are declared in JSON (`src/main/resources/templates.json`) and loaded at startup.
- Conditions are simple `key=value` strings evaluated at submit time (e.g. `requiresLegal=yes`).
- Any user with the required role in the same tenant can act on a step.
- In-flight workflow instances record `templateId` + `version` at submission time.
- Repositories are in-memory for the exercise and are pluggable interfaces (swap for DB later).
- No REST API or persistence required for this round — focus on correctness and design.

---

## What to look for in the code

- `ApprovalService` — core business logic (submit, act, validation, state transitions).
- `TemplateService` — small helper to publish new template versions (demo change-management).
- Domain models: `Request`, `WorkflowTemplate`, `WorkflowInstance`, `StepInstance`, `User`.
- Repositories: `InMemoryUserRepository`, `InMemoryTemplateRepository` (versioned), `InMemoryRequestRepository`.

## Demo Output

```
--- Scenario 1: Normal approval flow (Legal skipped) ---
Created: 384bdd11 status=IN_REVIEW
Request 384bdd11 -> APPROVED
  Manager Review -> APPROVED (MANAGER)
  Finance Review -> APPROVED (FINANCE)
  Legal Review -> SKIPPED (LEGAL)

--- Scenario 2: Rejection at Finance ---
Created: bd677e8c status=IN_REVIEW
Request bd677e8c -> REJECTED
  Manager Review -> APPROVED (MANAGER)
  Finance Review -> REJECTED (FINANCE)
  Legal Review -> PENDING (LEGAL)

--- Scenario 3: Security checks ---
Expected: Requester cannot act on their own request: u-requester
Tenant isolation: tasks for u-aditya = 0 (expected 0)

--- Scenario 4: Preflight validation ---
Expected preflight failure: No approvers in tenant 'tenantA' for role: AUDITOR

--- Scenario 5: Template versioning (in-flight keeps old version) ---
Submitted req 1e3d9454 using template v1
Published template latest v2 (steps=4)
In-flight req uses template v1 (steps=3)
Final status (in-flight) = APPROVED

Demo complete.

```
