# 💈 Barbering

A modern, full-stack web application for managing barber shop services, appointments, and clients.  
Built using **Next.js (Frontend)** and **Spring Boot (Backend)** in a scalable monorepo architecture.

---

## 🧱 Project Structure

 - FrontEnd - # Next.js (React + Tailwind CSS + TypeScript)
 - BackEnd - # Spring Boot (Java, REST APIs)

 ## ⚙️ Getting Started

### Prerequisites

- Node.js & npm
- JDK 17+
- Maven

---

#### Setup

    1. Clone the repo
    2. Start the frontend - npm install, npm run dev
    3. Start the backend - ./mvnw spring-boot:run


📁 Folder Structure (Detailed)
    frontend/

        app/ – App routing (Next.js App Router)

        components/ – Shared React components

        lib/ – Reusable utilities

        services/ – API handling

        types/ – TypeScript types and interfaces

        styles/ – Custom Tailwind styles or CSS

    backend/

        src/main/java/com/barbering/ – Controllers, Services, Models

        src/main/resources/ – application.properties, templates, static files

📌 Todo

    Setup MongoDB / PostgreSQL in production

    Create authentication system

    Dockerize for deployment

    CI/CD via GitHub Actions/Self Made Kubernetes environment using tools like jenkins

🧑‍💻 Contributors

    Shaurya Verma – Full-stack dev, maintainer
    Ujjwal Walia - Full-stack dev, mediater

