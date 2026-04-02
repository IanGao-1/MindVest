# MindVest Presentation Framework

This outline is designed to match the `Project Presentations` section in [final-project.md](/Users/yijian/Desktop/MindVest/final-project.md), especially:

- tell a story
- introduce the team
- introduce the project and learning context
- explain the team approach and technologies
- show the data model
- show high-level architecture
- give a live demo
- reflect on challenges, mistakes, and next steps

For a 15-20 minute presentation, aim for **10-12 slides + live demo**.

## Recommended Flow

### Slide 1. Title

**MindVest: Portfolio Management System**

- Team name and team members
- One-line tagline:
  `A portfolio manager for tracking assets, transactions, and performance`
- Optional subtitle:
  `Built with Spring Boot, H2, Swagger, and Vue`

Speaker note:
Briefly introduce who you are and make sure everyone on the team is named, because the spec explicitly says to start by introducing the team.

---

### Slide 2. Project Background

**What Were We Asked To Build?**

- Build a portfolio management application
- Core target: a REST API for storing and retrieving portfolio data
- Front-end goal:
  browse portfolio, view performance, add items, remove items
- Time constraint:
  explain how long you had to work on it

Suggested wording:
`We were asked to design and build a portfolio management application, starting from a REST API and extending it with a front end that lets users browse holdings, view performance, and manage portfolio items.`

Speaker note:
This is the “beginning” of the story: what the customer wanted and what success looked like.

---

### Slide 3. Our Approach

**How We Approached The Project**

- Started from a minimal viable backend
- Focused first on core portfolio and asset records
- Added market data and analytics after CRUD was stable
- Extended into a dashboard-style front end for visualization
- Used Git and incremental development rather than building everything at once

If you want, add team division:

- Person A: backend APIs and services
- Person B: analytics / market data / testing
- Person C: front end / UI / integration / presentation

Speaker note:
The spec asks whether you divided roles or coded together. If your actual process was mixed, say that honestly.

---

### Slide 4. Tech Stack

**Technologies And Tools**

- Backend: Spring Boot
- Persistence: H2 database
- API documentation: Swagger / OpenAPI
- Build tool: Maven
- Front end: Vue
- Version control: Git

Optional sentence:
`We chose a standard layered Java backend so we could move quickly from data model to REST endpoints, then connected it to a lightweight front end for visualization.`

---

### Slide 5. Data Model

**Core Data Model Decisions**

Show 3 core entities:

- `Portfolio`
  - id, name, description, createdAt
- `Asset`
  - id, ticker, name, type, quantity, avgCost, currentPrice, purchaseDate, lastUpdated, notes
- `AssetTransaction`
  - id, assetId, transactionType, quantity, price, totalAmount, remainingQuantity, transactionDate

Key talking points:

- We kept the initial model relatively simple
- `Asset` represents current holdings
- `AssetTransaction` supports buy/sell history and analytics
- `Portfolio` groups the investment records at a higher level

Speaker note:
This slide matters because the specification explicitly asks for a data model overview and your design decisions.

---

### Slide 6. Architecture

**High-Level Application Architecture**

Recommended diagram:

`Vue Front End -> REST Controllers -> Services -> Repositories -> H2 Database`

Also show market/history path:

`Market Data / History JSON / Yahoo Quote -> Analytics Service -> Charts`

Suggested components to mention:

- Controllers expose API endpoints
- Services contain business logic
- Repositories handle persistence
- Swagger documents the API
- Front end consumes the REST API and renders dashboard views

Speaker note:
Keep this high-level. The spec asks for a simple diagram, not a deep technical lecture.

---

### Slide 7. What We Built

**Implemented Features**

- Asset management
  - create and view assets
  - filter by type
  - sell asset quantities
- Portfolio management
  - create portfolios
  - add/remove assets
- Market data
  - get current price by ticker
  - fetch quote/history endpoints
- Analytics
  - holding value history
  - portfolio value history
- Front end dashboard
  - total equity overview
  - allocation chart
  - holdings table
  - history view

Tip:
Keep this slide short and let the demo do the real proof.

---

### Slide 8. Demo Plan

**Live Demo Walkthrough**

Recommended order:

1. Show dashboard landing page
2. Show holdings table and asset allocation
3. Open transaction flow or add transaction entry point
4. Show history / performance charts
5. Show Swagger endpoint for one backend API
6. Optionally show H2 or sample data reset if it is stable

Demo script:

- Start from the UI so non-technical audience sees value quickly
- Then briefly connect the UI to the API
- End with analytics/history because it feels like the most complete business outcome

Important:
Choose only the flows that are stable. A short, smooth demo is better than a long risky one.

---

### Slide 9. Challenges And Lessons

**Challenges We Faced**

- Defining a data model that was not too complex too early
- Connecting transaction history with portfolio analytics
- Keeping frontend and backend contracts aligned
- Managing sample data and history data consistently
- Balancing feature ambition with limited project time

Good reflective wording:
`One of our biggest lessons was that it was better to get a simple version working first and then add analytics, rather than starting with an overly ambitious data model.`

---

### Slide 10. What We Would Do Differently

**Mistakes And Improvements**

- Design API contracts earlier before building UI details
- Add more automated integration tests
- Separate demo data and production-style data more clearly
- Improve error handling and validation
- Plan the demo scenario earlier to reduce last-minute risk

Speaker note:
The spec explicitly asks for mistakes and what you would do differently, so being reflective helps.

---

### Slide 11. Next Steps

**What We Would Build Next**

- Real market data integration
- Persistent database instead of in-memory H2 for deployment
- User authentication and multi-user support
- More advanced analytics and risk metrics
- Better transaction history and portfolio drill-down
- Stronger test coverage and deployment pipeline

Suggested one-liner:
`If we had more time, we would focus on making the product more production-ready rather than just adding surface-level features.`

---

### Slide 12. Closing

**Thank You / Questions**

- Thank the audience
- Invite questions
- Prepare one backup slide in case you need it

Backup slide ideas:

- API endpoint summary
- team workflow / Git process
- extra UI screenshots

## Suggested Timing

For a 15-20 minute group presentation:

- Slides 1-4: 3-4 min
- Slides 5-7: 4-5 min
- Slide 8 demo: 4-6 min
- Slides 9-11: 3-4 min
- Slide 12 questions: 1 min+

## Speaker Split Suggestion

To satisfy `Everyone is expected to speak`, a good split is:

- Speaker 1:
  Slides 1-3
- Speaker 2:
  Slides 4-6
- Speaker 3:
  Slides 7-11 + transition to Q&A

Alternative:
let the strongest demo presenter own Slide 8 even if they also speak elsewhere.

## Practical Advice For This Project

### 1. Emphasize the story, not just features

Good presentation story:

- We started with a basic portfolio API
- We made the data model workable
- We added analytics and a dashboard
- We learned where finance apps become more complex than they first appear

That matches the specification better than just listing endpoints.

### 2. Use your actual implemented features

Based on the current codebase, the strongest items to present are:

- REST API with controllers for assets, portfolios, and market data
- Swagger/OpenAPI support
- dashboard-style Vue front end
- analytics endpoints for holdings and portfolio history

These are stronger presentation points than promising unfinished features.

### 3. Keep the architecture diagram simple

Do not overcrowd the slide.

Recommended boxes:

- Front End (Vue)
- REST API (Spring Boot Controllers)
- Business Logic (Services)
- Persistence (Repositories + H2)
- External / Historical Market Data

### 4. Demo only one happy path

Best likely happy path:

- open dashboard
- show holdings and allocation
- trigger or explain sample data
- show one chart/history page
- show one Swagger endpoint

Avoid switching between too many tools or tabs.

### 5. Be honest about limitations

This specification rewards reflection. It is completely fine to say:

- data source is partially mocked / local history based
- database is H2 in-memory for development
- authentication is intentionally out of scope
- production hardening is future work

That sounds realistic and mature.

## Optional Visual Assets To Use In PPT

You already have useful visual material in the repo:

- UI inspiration screens under [stitch](/Users/yijian/Desktop/MindVest/stitch)
- front-end app under [financial-management/front/formal](/Users/yijian/Desktop/MindVest/financial-management/front/formal)
- backend project under [financial-management](/Users/yijian/Desktop/MindVest/financial-management)

If you want the PPT to look clean, use:

- 1 architecture diagram
- 1 simplified entity relationship/data model slide
- 2 UI screenshots max before demo

## A Short Version You Can Paste Into PowerPoint

1. Title / Team Introduction
2. Project Goal And Context
3. Our Approach And Team Workflow
4. Tech Stack
5. Data Model
6. System Architecture
7. Features Implemented
8. Live Demo
9. Challenges And Lessons Learned
10. What We Would Do Differently
11. Next Steps
12. Thank You / Questions
