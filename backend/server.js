const express = require('express');
const axios = require('axios');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const SPORTMONKS_TOKEN = process.env.SPORTMONKS_TOKEN;
const FOOTBALL_DATA_TOKEN = process.env.FOOTBALL_DATA_TOKEN || process.env.FOOTBALL_DATA_API_KEY;

const BASE_URL = 'https://api.sportmonks.com/v3/football';
const FOOTBALL_DATA_URL = 'https://api.football-data.org/v4';

// Verify tokens on startup
if (!SPORTMONKS_TOKEN) console.error('SPORTMONKS_TOKEN is not set.');
if (!FOOTBALL_DATA_TOKEN) console.warn('FOOTBALL_DATA_TOKEN is not set. Football-Data.org features will be disabled.');

const api = axios.create({
    baseURL: BASE_URL,
    headers: { 'Authorization': SPORTMONKS_TOKEN }
});

const footballDataApi = axios.create({
    baseURL: FOOTBALL_DATA_URL,
    headers: { 'X-Auth-Token': FOOTBALL_DATA_TOKEN }
});

// Translation helper: Football-Data.org -> Sportmonks v3
function translateFootballDataToSportmonks(match) {
    return {
        id: `FD_${match.id}`, // Prefix to distinguish from Sportmonks
        name: `${match.homeTeam.name} vs ${match.awayTeam.name}`,
        starting_at: match.utcDate,
        result_info: match.status,
        league: {
            id: match.competition.id,
            name: match.competition.name,
            image_path: match.competition.emblem
        },
        participants: [
            {
                id: match.homeTeam.id,
                name: match.homeTeam.shortName || match.homeTeam.name,
                image_path: match.homeTeam.crest,
                meta: { location: 'home' }
            },
            {
                id: match.awayTeam.id,
                name: match.awayTeam.shortName || match.awayTeam.name,
                image_path: match.awayTeam.crest,
                meta: { location: 'away' }
            }
        ],
        scores: [
            {
                participant_id: match.homeTeam.id,
                score: { goals: match.score.fullTime.home },
                description: 'CURRENT'
            },
            {
                participant_id: match.awayTeam.id,
                score: { goals: match.score.fullTime.away },
                description: 'CURRENT'
            }
        ],
        events: []
    };
}

// Translate FD Match Detail (matches single match response)
function translateFDDetailToSportmonks(match) {
    const base = translateFootballDataToSportmonks(match);
    // Add lineups or stats if available in detail view
    return base;
}


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
        const results = [];
        
        // 1. Try Sportmonks
        try {
            const smResponse = await api.get('/livescores', {
                params: { include: 'participants;scores;events;league.country' }
            });
            if (smResponse.data.data) results.push(...smResponse.data.data);
        } catch (e) { console.error('Sportmonks livescores failed', e.message); }

        // 2. Try Football-Data.org
        if (FOOTBALL_DATA_TOKEN) {
            try {
                const fdResponse = await footballDataApi.get('/matches', { params: { status: 'IN_PLAY' } });
                if (fdResponse.data.matches) {
                    const translated = fdResponse.data.matches.map(translateFootballDataToSportmonks);
                    results.push(...translated);
                }
            } catch (e) { console.error('Football-Data livescores failed', e.message); }
        }

        res.json({ data: results });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
});

// Today's fixtures or by date
app.get('/api/sports/fixtures', async (req, res) => {
    try {
        const { date } = req.query; // YYYY-MM-DD
        const results = [];

        // 1. Try Sportmonks
        try {
            let smEndpoint = '/fixtures';
            if (date) smEndpoint = `/fixtures/date/${date}`;
            const smResponse = await api.get(smEndpoint, {
                params: { include: 'participants;scores;league.country' }
            });
            if (smResponse.data.data) results.push(...smResponse.data.data);
        } catch (e) { console.error('Sportmonks fixtures failed', e.message); }

        // 2. Try Football-Data.org
        if (FOOTBALL_DATA_TOKEN) {
            try {
                const params = {};
                if (date) {
                    params.dateFrom = date;
                    params.dateTo = date;
                }
                const fdResponse = await footballDataApi.get('/matches', { params });
                if (fdResponse.data.matches) {
                    const translated = fdResponse.data.matches.map(translateFootballDataToSportmonks);
                    results.push(...translated);
                }
            } catch (e) { console.error('Football-Data fixtures failed', e.message); }
        }

        res.json({ data: results });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
});

// Fixture by ID
app.get('/api/sports/fixtures/:id', async (req, res) => {
    try {
        const id = req.params.id;

        // 1. Check if it's a Football-Data.org match
        if (id.startsWith('FD_')) {
            const originalId = id.replace('FD_', '');
            const response = await footballDataApi.get(`/matches/${originalId}`);
            const translated = translateFDDetailToSportmonks(response.data);
            return res.json({ data: translated });
        }

        // 2. Default to Sportmonks
        const response = await api.get(`/fixtures/${id}`, {
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

// Translation helper: FD Standing -> Sportmonks format
function translateFDStandingToSportmonks(tableEntry) {
    return {
        position: tableEntry.position,
        points: tableEntry.points,
        participant_id: tableEntry.team.id,
        participant: {
            id: tableEntry.team.id,
            name: tableEntry.team.shortName || tableEntry.team.name,
            image_path: tableEntry.team.crest
        },
        details: [
            { type: { code: 'played' }, value: tableEntry.playedGames },
            { type: { code: 'won' }, value: tableEntry.won },
            { type: { code: 'draw' }, value: tableEntry.draw },
            { type: { code: 'lost' }, value: tableEntry.lost },
            { type: { code: 'goals-for' }, value: tableEntry.goalsFor },
            { type: { code: 'goals-against' }, value: tableEntry.goalsAgainst }
        ]
    };
}

// Standings by League
app.get('/api/sports/standings/leagues/:id', async (req, res) => {
    try {
        const id = req.params.id;

        // 1. Check if it's a Football-Data.org code (non-numeric or specific code)
        const fdCodes = ['PL', 'PD', 'BL1', 'SA', 'FL1', 'DED', 'BSA', 'PPL', 'ELC', 'CL', 'EC', 'WC'];
        if (isNaN(id) || fdCodes.includes(id)) {
            if (!FOOTBALL_DATA_TOKEN) throw new Error('Football-Data token not configured');
            
            const response = await footballDataApi.get(`/competitions/${id}/standings`);
            const totalTable = response.data.standings.find(s => s.type === 'TOTAL');
            if (totalTable && totalTable.table) {
                const translated = totalTable.table.map(translateFDStandingToSportmonks);
                return res.json({ data: translated });
            }
            throw new Error('Table not found');
        }

        // 2. Default to Sportmonks
        try {
            const response = await api.get(`/standings/live/leagues/${id}`, {
                params: { include: 'participant;details' }
            });
            res.json(response.data);
        } catch (error) {
            const response = await api.get(`/standings/seasons/${id}`, {
                params: { include: 'participant;details' }
            });
            res.json(response.data);
        }
    } catch (error) {
        res.status(500).json({ message: error.message });
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
