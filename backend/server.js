const express = require('express');
const axios = require('axios');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;
const SPORTMONKS_TOKEN = process.env.SPORTMONKS_TOKEN;
const BASE_URL = 'https://api.sportmonks.com/v3/football';

if (!SPORTMONKS_TOKEN) {
  console.error('CRITICAL: SPORTMONKS_TOKEN is not defined in environment variables.');
  process.exit(1);
}

// Middleware
app.use(helmet());
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());

// Sportmonks Client
const sportmonks = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Authorization': SPORTMONKS_TOKEN
  }
});

// Request interceptor for logging
sportmonks.interceptors.request.use(config => {
  console.log(`[Sportmonks Request] ${config.method.toUpperCase()} ${config.baseURL}${config.url}`);
  console.log(`[Params]`, config.params);
  console.log(`[Auth Header Present] ${!!SPORTMONKS_TOKEN}`);
  return config;
});

// Helper for error handling
const handleApiError = (res, error) => {
  if (error.response) {
    const status = error.response.status;
    const message = error.response.data.message || 'Error from sports data provider';
    const code = error.response.data.code;
    console.error(`Sportmonks API Error: ${status} - ${message} (Code: ${code})`);
    return res.status(status).json({ 
      error: message,
      status: status,
      apiCode: code
    });
  }
  console.error('Network/Server Error:', error.message);
  res.status(500).json({ error: 'Internal Server Error', details: error.message });
};

// Endpoints
app.get('/health', (req, res) => {
  res.json({
    status: "ok",
    service: "My Live Sports Scores Backend"
  });
});

const DIAGNOSTICS_SECRET = process.env.DIAGNOSTICS_SECRET;

app.get('/api/sports/diagnostics', async (req, res) => {
  if (DIAGNOSTICS_SECRET && req.query.secret !== DIAGNOSTICS_SECRET) {
    return res.status(403).json({ error: "Unauthorized access to diagnostics" });
  }

  try {
    const subscription = await sportmonks.get('/my/subscription');
    const leaguesResponse = await sportmonks.get('/leagues');
    
    const freeLeagues = leaguesResponse.data.data.filter(l => [271, 501].includes(l.id));
    
    res.json({
      auth_header_sent: !!SPORTMONKS_TOKEN,
      subscription: subscription.data,
      free_leagues: freeLeagues.map(l => ({
        id: l.id,
        name: l.name,
        current_season_id: l.current_season_id,
        is_active: l.is_active
      })),
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

app.get('/api/sports/livescores', async (req, res) => {
  try {
    const response = await sportmonks.get('/livescores/inplay', {
      params: { include: 'participants;scores;events;league;state' }
    });
    if (response.data.data.length === 0) {
      return res.json({
        data: [],
        message: "No matches are currently in-play. Try GET /api/sports/fixtures/today for today's schedule.",
        timestamp: new Date().toISOString()
      });
    }
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/livescores/inplay', async (req, res) => {
  try {
    const response = await sportmonks.get('/livescores/inplay', {
      params: { include: 'participants;scores;events;league;state' }
    });
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/livescores/latest', async (req, res) => {
  try {
    const response = await sportmonks.get('/livescores', {
      params: { include: 'participants;scores;events;league;state' }
    });
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/fixtures/today', async (req, res) => {
  try {
    const today = new Date().toISOString().split('T')[0];
    const response = await sportmonks.get(`/fixtures/date/${today}`, {
      params: { 
        include: 'participants;scores;league;state',
        filters: 'fixtureLeagues:501,271'
      }
    });
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/fixtures', async (req, res) => {
  try {
    const response = await sportmonks.get('/fixtures', {
      params: { include: 'participants;scores;league;state' }
    });
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/standings', async (req, res) => {
  try {
    const response = await sportmonks.get('/standings');
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/leagues', async (req, res) => {
  try {
    const response = await sportmonks.get('/leagues');
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/teams', async (req, res) => {
  try {
    const response = await sportmonks.get('/teams');
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/players', async (req, res) => {
  try {
    const response = await sportmonks.get('/players');
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/top-scorers', async (req, res) => {
  try {
    const response = await sportmonks.get('/topscorers');
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Backend proxy server running on port ${PORT} and listening on 0.0.0.0`);
});
