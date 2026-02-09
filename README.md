# Installation & Setup

## Prerequisites
- Python 3.11+
- PostgreSQL
- Poetry (dependency management)

## Installation Steps

### 1. Install Poetry
**macOS:**
```bash
brew install poetry
```

**Windows/Linux:** Follow instructions at https://python-poetry.org/docs/#installation

### 2. Clone the Repository
```bash
git clone <your-repo-url>
cd backend
```

### 3. Install Dependencies
```bash
poetry install --no-root
```

### 4. Activate Virtual Environment
```bash
poetry env activate
```

> **Note:** If `poetry env activate` doesn't work, manually activate:
> ```bash
> poetry env info  # Shows path to virtualenv
> source /path/to/virtualenv/bin/activate  # Copy the activate path
> ```

### 5. Set Up PostgreSQL Database
1. Open **pgAdmin4**
2. Create a new database named `ovi`

### 6. Configure Environment Variables
Create a `.env` file in the `backend` directory:
```bash
cp .env.example .env
```
Edit `.env` with your database credentials (see `.env.example` for reference)

### 7. Run the Server
```bash
uvicorn src.main:app --reload
```

✅ API available at: `http://127.0.0.1:8000`

### 8. Access API Documentation
- **Swagger UI:** http://127.0.0.1:8000/docs#

---

## Test Users

| Username | Password   |
|----------|------------|
| user1    | 12345678   |
| user2    | 87654321   |

---

## Available Endpoints

### Authentication
- `POST /api/v1/jwt/register_user/` - Register new user
- `POST /api/v1/jwt/login_user/` - Login (returns access + refresh tokens)

---

## Troubleshooting

### ❌ Database Connection Error
- Verify PostgreSQL is running
- Check `DATABASE_URL` in `.env` file
- Ensure database `ovi` exists

### ❌ Module Not Found Error
- Confirm you're in the `backend` directory
- Activate virtual environment: `poetry shell`

### ❌ Port Already in Use
```bash
# Option 1: Kill process on port 8000
lsof -ti:8000 | xargs kill -9

# Option 2: Use different port
uvicorn src.main:app --reload --port 8001
```

---
