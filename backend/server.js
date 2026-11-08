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
const API_FOOTBALL_TOKEN = process.env.API_FOOTBALL_TOKEN || process.env.API_FOOTBALL_KEY;

const BASE_URL = 'https://api.sportmonks.com/v3/football';
const FOOTBALL_DATA_URL = 'https://api.football-data.org/v4';
const API_FOOTBALL_URL = 'https://v3.football.api-sports.io';

// Verify tokens on startup
if (!SPORTMONKS_TOKEN) console.error('SPORTMONKS_TOKEN is not set.');
if (!FOOTBALL_DATA_TOKEN) console.warn('FOOTBALL_DATA_TOKEN is not set.');
if (!API_FOOTBALL_TOKEN) console.warn('API_FOOTBALL_TOKEN is not set. Ghana data may be unavailable.');

const api = axios.create({
    baseURL: BASE_URL,
    headers: { 'Authorization': SPORTMONKS_TOKEN }
});

const footballDataApi = axios.create({
    baseURL: FOOTBALL_DATA_URL,
    headers: { 'X-Auth-Token': FOOTBALL_DATA_TOKEN }
});

const apiFootball = axios.create({
    baseURL: API_FOOTBALL_URL,
    headers: { 'x-apisports-key': API_FOOTBALL_TOKEN }
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

// Translation helper: API-Football -> Sportmonks v3
function translateApiFootballToSportmonks(item) {
    const { fixture, league, teams, goals, score } = item;
    return {
        id: `AF_${fixture.id}`,
        name: `${teams.home.name} vs ${teams.away.name}`,
        starting_at: fixture.date,
        result_info: fixture.status.long,
        league: {
            id: league.id,
            name: league.name,
            image_path: league.logo,
            country: { name: league.country, image_path: league.flag }
        },
        participants: [
            {
                id: teams.home.id,
                name: teams.home.name,
                image_path: teams.home.logo,
                meta: { location: 'home' }
            },
            {
                id: teams.away.id,
                name: teams.away.name,
                image_path: teams.away.logo,
                meta: { location: 'away' }
            }
        ],
        scores: [
            {
                participant_id: teams.home.id,
                score: { goals: goals.home },
                description: 'CURRENT'
            },
            {
                participant_id: teams.away.id,
                score: { goals: goals.away },
                description: 'CURRENT'
            }
        ],
        events: item.events ? item.events.map(e => ({
            minute: e.time.elapsed,
            player_name: e.player.name,
            type_id: e.type === 'Goal' ? 1 : (e.type === 'Card' ? 2 : 4) // Simplified mapping
        })) : []
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
    const report = {
        status: 'ok',
        providers: {
            sportmonks: { configured: !!SPORTMONKS_TOKEN, status: 'pending' },
            football_data: { configured: !!FOOTBALL_DATA_TOKEN, status: 'pending' },
            api_football: { configured: !!API_FOOTBALL_TOKEN, status: 'pending' }
        }
    };

    // Check Sportmonks
    try {
        await api.get('/leagues');
        report.providers.sportmonks.status = 'active';
    } catch (e) { report.providers.sportmonks.status = 'error'; }

    // Check Football-Data
    if (FOOTBALL_DATA_TOKEN) {
        try {
            await footballDataApi.get('/competitions');
            report.providers.football_data.status = 'active';
        } catch (e) { report.providers.football_data.status = 'error'; }
    }

    // Check API-Football
    if (API_FOOTBALL_TOKEN) {
        try {
            await apiFootball.get('/leagues', { params: { country: 'Ghana' } });
            report.providers.api_football.status = 'active';
        } catch (e) { report.providers.api_football.status = 'error'; }
    }

    res.json(report);
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

        // 3. Try API-Football
        if (API_FOOTBALL_TOKEN) {
            try {
                const afResponse = await apiFootball.get('/fixtures', { params: { live: 'all' } });
                if (afResponse.data.response) {
                    const translated = afResponse.data.response.map(translateApiFootballToSportmonks);
                    results.push(...translated);
                }
            } catch (e) { console.error('API-Football livescores failed', e.message); }
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

        // 3. Try API-Football
        if (API_FOOTBALL_TOKEN) {
            try {
                const params = {};
                if (date) params.date = date;
                else params.date = new Date().toISOString().split('T')[0];
                const afResponse = await apiFootball.get('/fixtures', { params });
                if (afResponse.data.response) {
                    const translated = afResponse.data.response.map(translateApiFootballToSportmonks);
                    results.push(...translated);
                }
            } catch (e) { console.error('API-Football fixtures failed', e.message); }
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

        // 2. Check if it's an API-Football match
        if (id.startsWith('AF_')) {
            const originalId = id.replace('AF_', '');
            const response = await apiFootball.get('/fixtures', { params: { id: originalId } });
            if (response.data.response && response.data.response[0]) {
                const translated = translateApiFootballToSportmonks(response.data.response[0]);
                return res.json({ data: translated });
            }
            throw new Error('Match not found');
        }

        // 3. Default to Sportmonks
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

// Translation helper: AF Standing -> Sportmonks format
function translateAFStandingToSportmonks(standingItem) {
    return {
        position: standingItem.rank,
        points: standingItem.points,
        participant_id: standingItem.team.id,
        participant: {
            id: standingItem.team.id,
            name: standingItem.team.name,
            image_path: standingItem.team.logo
        },
        details: [
            { type: { code: 'played' }, value: standingItem.all.played },
            { type: { code: 'won' }, value: standingItem.all.win },
            { type: { code: 'draw' }, value: standingItem.all.draw },
            { type: { code: 'lost' }, value: standingItem.all.lose },
            { type: { code: 'goals-for' }, value: standingItem.all.goals.for },
            { type: { code: 'goals-against' }, value: standingItem.all.goals.against }
        ]
    };
}

// Standings by League
app.get('/api/sports/standings/leagues/:id', async (req, res) => {
    try {
        const id = req.params.id;

        // 1. Check if it's an API-Football code (prefixed with AF_ or Ghana ID)
        if (id.startsWith('AF_') || id === '277') {
            const leagueId = id.replace('AF_', '');
            if (!API_FOOTBALL_TOKEN) throw new Error('API-Football token not configured');
            const response = await apiFootball.get('/standings', { params: { league: leagueId, season: new Date().getFullYear() } });
            if (response.data.response && response.data.response[0]) {
                const standings = response.data.response[0].league.standings[0];
                const translated = standings.map(translateAFStandingToSportmonks);
                return res.json({ data: translated });
            }
            throw new Error('Standing not found');
        }

        // 2. Check if it's a Football-Data.org code (non-numeric or specific code)
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

        // 3. Default to Sportmonks
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
