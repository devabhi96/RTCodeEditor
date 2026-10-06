# Database Design

## Overview
Optional PostgreSQL database for document persistence and user accounts.

## Tables

### documents
- id (UUID, Primary Key)
- title (String)
- content (Text)
- created_at (Timestamp)
- updated_at (Timestamp)
- owner_id (Foreign Key to users)

### users
- id (UUID, Primary Key)
- username (String, Unique)
- email (String, Unique)
- password_hash (String)
- created_at (Timestamp)
- last_login_at (Timestamp)

### document_access (Optional, for sharing)
- id (UUID, Primary Key)
- document_id (Foreign Key to documents)
- user_id (Foreign Key to users)
- permission_level (Enum: owner, editor, viewer)
- granted_at (Timestamp)

## Relationships
- One user can own many documents
- One document can have many users with access permissions
- Documents store the latest state; historical versions could be stored separately if needed