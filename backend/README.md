# My Live Sports Scores - Backend Proxy

This is a secure Node.js backend that proxies requests to the Sportmonks Football API v3. It ensures that your API token is never exposed to the Android client.

## Deployment Instructions

### 1. Configure Sportmonks Token
You must have a valid Sportmonks API v3 token.

### 2. Deploy to Render / Cloud Run
When deploying to a platform like **Render** or **Google Cloud Run**:
- Set the environment variable `SPORTMONKS_TOKEN` to your actual API key.
- Set `NODE_ENV` to `production`.

### 3. Update Android App
Once deployed, update the `BACKEND_URL` in your Android application's `Retrofit` configuration to point to your deployed service URL (e.g., `https://your-app.onrender.com/`).

## Local Development
1. `cd backend`
2. `npm install`
3. Create a `.env` file based on `.env.example`.
4. `npm run dev`

## Testing
- Health Check: `GET /api/sports/health`
- Live Scores: `GET /api/sports/livescores`
- Fixtures: `GET /api/sports/fixtures`
