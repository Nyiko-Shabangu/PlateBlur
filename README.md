# Rihlazana Auctions Mobile App

This repository contains the development documentation for the **Rihlazana Auctions Mobile App**. The app streamlines the auction cataloguing process, automating photo management, catalog creation, PDF generation, social media integration, and includes a **number plate blurring feature** using Google ML to ensure privacy in auction images.

## Table of Contents
1. [Introduction](#introduction)
2. [Project Roadmap](#project-roadmap)
3. [Requirements](#requirements)
   - [User Roles](#user-roles)
   - [User Stories](#user-stories)
   - [Non-functional Requirements](#non-functional-requirements)
4. [Features](#features)
   - [Number Plate Blurring](#number-plate-blurring)
5. [Architecture](#architecture)
   - [Domain Model](#domain-model)
   - [Design Patterns](#design-patterns)
   - [Cloud Services](#cloud-services)
6. [Security](#security)
7. [DevOps Pipeline](#devops-pipeline)
8. [Cost Estimation](#cost-estimation)
9. [Appendices](#appendices)

## Introduction
The Rihlazana Auctions app aims to improve the cataloguing and auction preparation processes by providing:
- An automated photo upload system
- A lot catalogue builder with numbering and PDF generation
- Secure login for staff and admin
- Social media API integration
- Number plate blurring for images to enhance bidder privacy

The app minimizes manual tasks, enhancing accuracy and reducing operational time for Rihlazana Auctions.

## Project Roadmap
The project follows a structured 8-week, multi-sprint roadmap:
1. **Sprint 1**: Setup, requirements gathering, and architecture.
2. **Sprint 2**: Core functionality (authentication, photo upload).
3. **Sprint 3**: Catalogue organization and PDF generation.
4. **Sprint 4**: UI/UX design.
5. **Sprint 5**: API integration and testing.
6. **Sprint 6**: Security and performance optimization.
7. **Sprint 7**: Final testing and deployment preparation.

## Requirements

### User Roles
- **Operations Team Member**: Manages lot uploads, cataloguing, and auction info.
- **Administrator**: Approves catalogues, manages users, generates analytics, and handles social media integration.
- **Potential Bidder**: Browses upcoming auctions and views catalogue details.
- **Guest User**: Accesses limited information without login.

### User Stories
Sample stories include:
- As an Operations Team Member, I can upload multiple photos for a lot in a single action to streamline cataloguing.
- As an Administrator, I can generate a PDF catalogue quickly to prepare for an auction.
- As a Potential Bidder, I can save lots of interest to plan bids.

### Non-functional Requirements
- **Performance**: Quick load times, photo uploads within 5 seconds, and PDF generation under 10 seconds.
- **Scalability**: Supports up to 1,000 concurrent users.
- **Reliability**: 99.9% uptime and rapid system recovery.
- **Security**: Encrypted data, multi-factor authentication for admins, and adherence to POPIA.

## Features

### Number Plate Blurring
The app includes a number plate blurring feature, leveraging Google ML Kit to automatically detect and blur vehicle license plates in uploaded images. This feature enhances privacy and complies with data protection requirements, preventing unauthorized sharing of identifiable information. Key details:
- **Technology**: Google ML Kit's Text Recognition API
- **Regex Pattern Matching**: Customized for various South African number plate formats
- **Image Processing**: Detects and blurs recognized number plates before the images are added to the catalogue

## Architecture

### Domain Model
- **User Management Context**: Handles authentication, authorization, and profile management.
- **Auction Lot Context**: Manages lot creation, storage, and cataloguing.
- **Catalogue Context**: Organizes lots and generates PDFs.
- **Notification Context**: Manages system notifications.

### Design Patterns
- **MVVM Architecture**: Enhances testability and separation of concerns.
- **Repository Pattern**: Abstracts data access.
- **Factory, Observer, Singleton**: Patterns for efficient processing and state management.

### Cloud Services
Using **Azure**:
- **App Service**: API hosting
- **Blob Storage**: Image and document storage
- **SQL Database**: Relational storage
- **Functions**: Serverless photo processing and PDF generation
- **API Management**: Manages API security and access

## Security
Security mechanisms address potential threats such as phishing, unauthorized data access, and external attacks. Key measures:
- **Multi-factor Authentication** for admins.
- **Role-based Access Control** for sensitive data.
- **Data Encryption** during transmission and at rest.
- **OAuth 2.0** for secure external API integration.

## DevOps Pipeline
Using **GitHub Actions** for CI/CD, with steps for:
1. Code Checkout
2. Environment Setup
3. Linting & Testing
4. Build & Security Scan
5. Deployment to Staging
6. Manual Deployment to Production



[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/e6SRsF6I)
