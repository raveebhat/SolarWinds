Base Problem Statement: 
Design and implement an approval service for an expense review system. Users submit documents, which go through a multi-stage approval workflow (e.g., manager review → finance check → final sign-off). The system should handle creation/approvals/rejections and track status.

Core requirements
Create/submit a Request with metadata (requester, category, tenantId, and request meta data like amount and so on)
Define workflow templates with ordered or conditional steps (role-based: Manager, Finance, Legal).
Approvers receive tasks; they can approve/reject with comments.
Transition a request through steps: Ex: PENDING → IN_REVIEW → APPROVED/REJECTED.
Given a workflow system should give a proper status
Declarative workflow representation (e.g., JSON/YAML spec) and validation.
Clear domain models

Instructions for the round
Get core working first; avoid premature optimization
Prioritise correctness over features; better to have five solid requirements than six buggy ones
Show your thinking with brief comments on design choices
Test as you go; verify each piece works before proceeding
Avoid API layer and DB implementation initially. Prefer a general class-level implementation. Use a test class (preferred) or main class to run scenarios, and an in-memory data structure with a pluggable interface to store data, which you can later replace with any DB
The problem expects candidates to prioritise ruthlessly and make assumptions. Decision-making is key to success. Make and document assumptions or ask the panel for clarity.

Evaluation Criteria
Domain modeling & design Separate entities clearly. Define clear roles and relations. Avoid large classes or complex abstractions. Each entity should handle its own tasks.
State management Ensure correct workflow states. Validate transitions. no skips or duplicate approvals. Track approval history. Handle cases like rejection at step 2.
Access control & security
Users from Tenant A cannot access Tenant B data
Only assigned approvers can act on current step
Requesters cannot approve their own requests
All queries and actions must filter by tenant ID
Code quality Keep clean structure, clear names, single responsibility, minimal duplication, and proper error handling. Deliver production-ready code.
Problem solving & extensibility Handle edge cases (missing approvers, invalid inputs). Design for easy extension. Make trade-offs and explain design choices clearly

Bonus challenges ( Any one ):
Change management: versioned workflow templates; in-flight instances continue on old version.
Compensations: if a later step rejects, can earlier steps be revisited? Model as saga with compensating actions.
Search & reporting: list requests by status/approver/date with pagination, index strategy.
Event publishing: emit RequestApproved to a queue with exactly-once using outbox table.