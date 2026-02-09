# Backend Setup Guide

## Prerequisites

- Python 3.12+
- PostgreSQL
- Poetry (Python package manager)

## Installation Steps
1. Install Poetry (if not installed) (using Terminal)

### 2. Clone the repository

git clone <your-repo-url>
cd backend

### 3. Install dependencies
poetry install --no-root

### 4. Activate virtual environment

poetry env activate
```

### 5. Set up PostgreSQL database

Create a new PostgreSQL database:

```bash
psql -U postgres
CREATE DATABASE your_database_name;
\q
```

### 6. Configure environment variables

Create a `.env` file in the `backend` directory:

```env
DATABASE_URL=postgresql+asyncpg://username:password@localhost:5432/your_database_name
SECRET_KEY=your-secret-key-here
```

### 7. Run the server

```bash
uvicorn src.main:app --reload
```

The API will be available at `http://127.0.0.1:8000`

### 8. Access API documentation

- Swagger UI: `http://127.0.0.1:8000/docs`
- ReDoc: `http://127.0.0.1:8000/redoc`

## Test Users

The following test users are created automatically on startup:

- **User 1**: `user1@gmail.com` / password: `12345678`
- **User 2**: `user2@gmail.com` / password: `87654321`

## Available Endpoints

### Authentication

- `POST /api/v1/jwt/register_user/` - Register new user
- `POST /api/v1/jwt/login_user/` - Login (get access + refresh tokens)

## Troubleshooting

### Database connection error
- Check if PostgreSQL is running
- Verify DATABASE_URL in `.env` file

### Module not found error
- Make sure you're running from the `backend` directory
- Activate poetry shell: `poetry shell`

### Port already in use
- Kill the process using port 8000 or change port:
```bash
uvicorn src.main:app --reload --port 8001
```