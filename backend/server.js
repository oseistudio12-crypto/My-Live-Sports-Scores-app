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

// Helper for error handling
const handleApiError = (res, error) => {
  if (error.response) {
    const status = error.response.status;
    const message = error.response.data.message || 'Error from sports data provider';
    console.error(`Sportmonks API Error: ${status} - ${message}`);
    return res.status(status).json({ error: message });
  }
  console.error('Network/Server Error:', error.message);
  res.status(500).json({ error: 'Internal Server Error' });
};

// Endpoints
app.get('/api/sports/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

app.get('/api/sports/livescores', async (req, res) => {
  try {
    const response = await sportmonks.get('/livescores/inplay', {
      params: { include: 'participants;scores;events;league' }
    });
    res.json(response.data);
  } catch (error) {
    handleApiError(res, error);
  }
});

app.get('/api/sports/fixtures', async (req, res) => {
  try {
    const response = await sportmonks.get('/fixtures', {
      params: { include: 'participants;scores;league' }
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

app.listen(PORT, () => {
  console.log(`Backend proxy server running on port ${PORT}`);
});
