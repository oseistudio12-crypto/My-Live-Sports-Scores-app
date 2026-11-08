const express = require('express');
const axios = require('axios');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const SPORTMONKS_TOKEN = process.env.SPORTMONKS_TOKEN;
const BASE_URL = 'https://api.sportmonks.com/v3/football';

// Verify token on startup
if (!SPORTMONKS_TOKEN) {
    console.error('CRITICAL: SPORTMONKS_TOKEN is not set in environment variables.');
}

const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Authorization': SPORTMONKS_TOKEN
    }
});

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        service: 'Osei Live Sports Scores Backend',
        timestamp: new Date().toISOString()
    });
});

// Diagnostics
app.get('/api/sports/diagnostics', async (req, res) => {
    try {
        const response = await api.get('/leagues');
        const accessibleLeagues = response.data.data.map(l => ({
            id: l.id,
            name: l.name,
            type: l.type
        }));
        res.json({
            status: 'success',
            token_configured: !!SPORTMONKS_TOKEN,
            accessible_leagues: accessibleLeagues
        });
    } catch (error) {
        res.status(500).json({
            status: 'error',
            message: 'Failed to fetch diagnostics',
            error: error.response?.data || error.message
        });
    }
});

// Livescores
app.get('/api/sports/livescores', async (req, res) => {
    try {
        // Includes: participants, scores, events
        const response = await api.get('/livescores', {
            params: {
                include: 'participants;scores;events;league.country'
            }
        });
        res.json(response.data);
    } catch (error) {
        res.status(error.response?.status || 500).json(error.response?.data || { message: error.message });
    }
});

// Today's fixtures or by date
app.get('/api/sports/fixtures', async (req, res) => {
    try {
        const { date } = req.query; // YYYY-MM-DD
        let endpoint = '/fixtures';
        if (date) {
            endpoint = `/fixtures/date/${date}`;
        }

        const response = await api.get(endpoint, {
            params: {
                include: 'participants;scores;league.country'
            }
        });
        res.json(response.data);
    } catch (error) {
        res.status(error.response?.status || 500).json(error.response?.data || { message: error.message });
    }
});

// Fixture by ID
app.get('/api/sports/fixtures/:id', async (req, res) => {
    try {
        const response = await api.get(`/fixtures/${req.params.id}`, {
            params: {
                include: 'participants;scores;events;league.country;statistics;lineups;formations'
            }
        });
        res.json(response.data);
    } catch (error) {
        res.status(error.response?.status || 500).json(error.response?.data || { message: error.message });
    }
});

// Leagues
app.get('/api/sports/leagues', async (req, res) => {
    try {
        const response = await api.get('/leagues', {
            params: {
                include: 'country'
            }
        });
        res.json(response.data);
    } catch (error) {
        res.status(error.response?.status || 500).json(error.response?.data || { message: error.message });
    }
});

// Standings
app.get('/api/sports/standings', async (req, res) => {
    try {
        const response = await api.get('/standings');
        res.json(response.data);
    } catch (error) {
        res.status(error.response?.status || 500).json(error.response?.data || { message: error.message });
    }
});

// Standings by League
app.get('/api/sports/standings/leagues/:id', async (req, res) => {
    try {
        const response = await api.get(`/standings/live/leagues/${req.params.id}`, {
            params: {
                include: 'participant;details'
            }
        });
        res.json(response.data);
    } catch (error) {
        // Fallback to season standings if live standings not available
        try {
            const response = await api.get(`/standings/seasons/${req.params.id}`, {
                params: {
                    include: 'participant;details'
                }
            });
            res.json(response.data);
        } catch (innerError) {
            res.status(error.response?.status || 500).json(error.response?.data || { message: error.message });
        }
    }
});

// Teams
app.get('/api/sports/teams', async (req, res) => {
    try {
        const response = await api.get('/teams');
        res.json(response.data);
    } catch (error) {
        res.status(error.response?.status || 500).json(error.response?.data || { message: error.message });
    }
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({ message: `Cannot ${req.method} ${req.url}` });
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on port ${PORT}`);
});
