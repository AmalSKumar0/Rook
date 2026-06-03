# Rook Mobile - Architecture Documentation

## Overview

Rook Mobile is a production-grade Android API testing and management platform designed for developers to create, organize, test, monitor, and analyze API endpoints.

The application follows **Clean Architecture**, **MVVM**, and the **Repository Pattern** to ensure scalability, maintainability, testability, and long-term product growth.

The architecture separates responsibilities into independent layers and modules, allowing the application to evolve without large-scale rewrites.

---

# Architectural Principles

The application is built around the following principles:

* Separation of Concerns
* Single Responsibility Principle
* Dependency Inversion
* Feature-Based Organization
* Offline-First Data Strategy
* Scalable Modular Design
* Test-Driven Development Support

---

# Architecture Pattern

```text
┌─────────────────────────┐
│        UI Layer         │
│ Activities / Fragments  │
│ Compose Screens         │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│       ViewModels        │
│ UI State Management     │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│       Use Cases         │
│ Business Logic Layer    │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│      Repositories       │
└───────┬─────────┬───────┘
        │         │
        ▼         ▼
 ┌──────────┐ ┌──────────┐
 │ Remote   │ │ Local    │
 │ Data     │ │ Data     │
 └──────────┘ └──────────┘
```

---

# Layer Responsibilities

## UI Layer

The UI layer is responsible for:

* Rendering screens
* Handling user interactions
* Observing UI state
* Triggering actions through ViewModels

The UI layer must never directly communicate with:

* Databases
* Network clients
* Firebase services

Communication must occur through ViewModels only.

### Components

* Activities
* Fragments
* RecyclerView Adapters
* Dialogs
* Custom Views

---

## ViewModel Layer

ViewModels act as the bridge between UI and business logic.

Responsibilities:

* Managing screen state
* Handling user actions
* Calling Use Cases
* Exposing observable UI data

ViewModels should not contain networking code or database queries.

---

## Domain Layer

The Domain Layer contains application business logic.

This layer is independent of Android Framework components.

### Components

#### Models

Core business entities:

* User
* Project
* Collection
* Endpoint
* Report
* RequestHistory

#### Use Cases

Examples:

* LoginUserUseCase
* RegisterUserUseCase
* CreateProjectUseCase
* SaveEndpointUseCase
* RunApiRequestUseCase
* GenerateReportUseCase
* ImportCollectionUseCase
* ExportCollectionUseCase

The Domain Layer defines what the application can do.

---

## Repository Layer

Repositories abstract data sources.

The UI and Domain layers should never know whether data originates from:

* Room Database
* Firebase
* Remote API
* Local Cache

Repositories provide a single source of truth.

### Examples

* AuthRepository
* ProjectRepository
* CollectionRepository
* EndpointRepository
* ReportRepository

---

## Data Layer

The Data Layer manages data retrieval and persistence.

### Local Data Source

Technology:

* Room Database

Responsibilities:

* Offline storage
* Request history
* Cached API responses
* User settings
* Project storage

### Remote Data Source

Technology:

* Retrofit
* OkHttp

Responsibilities:

* Cloud synchronization
* Authentication requests
* Workspace collaboration
* Scheduled monitoring

---

# Dependency Injection

Dependency Injection is implemented using Hilt.

Benefits:

* Reduced coupling
* Improved testing
* Simplified object creation
* Better scalability

Example services managed by Hilt:

* Repositories
* Retrofit
* Database
* Firebase
* Use Cases

---

# Project Structure

```text
com.rook.mobile

├── app
│   ├── RookApplication
│   └── di
│
├── core
│   ├── network
│   ├── database
│   ├── security
│   ├── preferences
│   ├── common
│   ├── constants
│   ├── logging
│   └── utils
│
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entities
│   │   └── database
│   │
│   ├── remote
│   │   ├── api
│   │   ├── dto
│   │   ├── interceptors
│   │   └── services
│   │
│   ├── mapper
│   └── repository
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── features
│   ├── auth
│   ├── dashboard
│   ├── projects
│   ├── collections
│   ├── endpoints
│   ├── api_lab
│   ├── reports
│   ├── profile
│   └── settings
│
├── navigation
│
└── workers
```

---

# Feature Module Structure

Each feature follows the same structure.

Example:

```text
features/api_lab

├── ApiLabActivity
├── ApiLabViewModel
├── ApiLabUiState
├── adapters
├── components
└── models
```

Benefits:

* Predictable structure
* Easier onboarding
* Easier maintenance
* Better scalability

---

# Database Architecture

Technology:

* Room Database

Entities:

### User

Stores authenticated user information.

### Project

Top-level API organization.

### Collection

Groups related endpoints.

### Endpoint

Represents a single API request.

Stores:

* URL
* Method
* Headers
* Query Parameters
* Request Body
* Authentication

### RequestHistory

Stores executed request information.

### Report

Stores historical test results.

---

# Network Architecture

Technology:

* Retrofit
* OkHttp

Features:

* Authentication Interceptors
* Request Logging
* Error Handling
* Retry Mechanism
* Timeout Configuration
* SSL Validation

---

# API Testing Engine

The API Lab is the core feature of Rook.

Components:

### Request Builder

Constructs requests dynamically.

### Authentication Manager

Supports:

* Bearer Token
* Basic Auth
* API Key
* OAuth 2.0

### Request Executor

Responsible for:

* Sending requests
* Timing requests
* Capturing metadata

### Response Processor

Processes:

* Headers
* Status Codes
* Body Content
* Timing Metrics

### Report Generator

Creates persistent test reports.

---

# Offline-First Strategy

Rook follows an Offline-First architecture.

Priority order:

1. Local Database
2. Local Cache
3. Remote Source

Benefits:

* Faster loading
* Better reliability
* Reduced network dependency

---

# Background Processing

Technology:

* WorkManager

Used for:

* Cloud Sync
* Scheduled API Monitoring
* Report Uploads
* Data Cleanup
* Backup Operations

---

# Security Architecture

Sensitive information is never stored in plain text.

Technology:

* EncryptedSharedPreferences
* Android Keystore

Protected Data:

* Access Tokens
* Refresh Tokens
* API Keys
* User Credentials

---

# Logging & Monitoring

Logging Layer:

```text
core/logging
```

Responsibilities:

* Debug Logs
* Error Tracking
* Crash Reporting
* Performance Monitoring

Future integrations:

* Firebase Crashlytics
* Sentry

---

# Testing Strategy

Unit Tests:

* ViewModels
* Repositories
* Use Cases

Integration Tests:

* Database
* API Services

UI Tests:

* Authentication Flow
* Project Management
* API Testing Workflow

Testing Structure:

```text
test/
androidTest/
```

---

# Scalability Goals

This architecture is designed to support:

* Large API collections
* Team workspaces
* Cloud synchronization
* Scheduled API monitoring
* AI-powered endpoint analysis
* Multi-device support
* CI/CD integrations
* Future desktop and web interoperability

---

# Technology Stack

Architecture:

* Clean Architecture
* MVVM
* Repository Pattern

Database:

* Room

Networking:

* Retrofit
* OkHttp

Authentication:

* Firebase Authentication

Dependency Injection:

* Hilt

Background Tasks:

* WorkManager

Security:

* Android Keystore
* EncryptedSharedPreferences

Testing:

* JUnit
* Espresso
* Mockito

Language:

* Java / Kotlin

Platform:

* Android

```

---
