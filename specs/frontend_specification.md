# Frontend Specification for MyFinance PWA

This document outlines the technical specifications for the frontend of the MyFinance Progressive Web App (PWA).

## Technology Stack

*   **Framework:** React 18+
*   **Build Tool:** Vite
*   **Styling:** Tailwind CSS v4 (PostCSS)
*   **State Management:** Zustand
*   **Routing:** React Router DOM (v6/v7)
*   **Data Fetching:** TanStack Query (React Query)
*   **PWA Support:** vite-plugin-pwa
*   **Charts:** Recharts (Planned/Suggested)

## Key Features

1.  **Mobile-First Design:** Optimized for touch interactions and small screens.
2.  **Offline Capability:** Service worker caching for core assets and potential offline data syncing.
3.  **Installable:** Meets PWA criteria for installation on mobile and desktop.
4.  **Responsive Layout:** Adapts to various screen sizes using Tailwind's utility classes.

## Project Structure

```
src/
├── components/       # Reusable UI components (Button, Input, Card, etc.)
├── pages/            # Page-level components matching routes
├── hooks/            # Custom React hooks
├── store/            # Zustand state stores
├── services/         # API integration services (Axios/Fetch wrappers)
├── styles/           # Global styles and Tailwind configuration
├── utils/            # Helper functions and constants
└── App.jsx           # Main application entry point with routing
```

## State Management (Zustand)

Global application state is managed using Zustand for its simplicity and minimal boilerplate.

*   **User Store:** Authentication status, user profile data.
*   **Financial Store:** Income, expenses, assets, liabilities (cached via React Query where appropriate).
*   **UI Store:** Modal visibility, toast notifications, theme preferences.

## API Integration

The frontend communicates with the Spring Boot backend via verifyable REST endpoints.

*   **Base URL:** `/api/v1` (Proxied in development via Vite config)
*   **Authentication:** JWT-based or Session-based (To be finalized).
*   **Error Handling:** Global error boundary and toast notifications for API failures.

## Styling Guidelines

*   **Color Palette:** Use the defined Tailwind theme colors for consistency.
*   **Typography:** Sans-serif font stack (Inter/Roboto default).
*   **Components:** Build atomic components first, then assemble into complex views.
