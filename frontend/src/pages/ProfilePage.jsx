import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
    AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, 
    BarChart, Bar, Cell 
} from 'recharts';

const API_BASE = 'http://localhost:8083';

/* ─── Helpers de estado ─── */
const STATUS_LABELS = {
    visto: 'Visto', no_visto: 'No visto',
    en_progreso: 'En progreso', leido: 'Leído', leyendo: 'Leyendo',
    no_iniciado: 'No iniciado', pendiente: 'Pendiente', abandonado: 'Abandonado',
    seguimiento_episodios: 'Siguiendo', seguimiento_canciones: 'Escuchando',
    PLANNING: 'Pendiente', COMPLETED: 'Completado',
};
const STATUS_CHIP = {
    visto: 'chip-seen', leido: 'chip-seen', COMPLETED: 'chip-seen',
    no_visto: 'chip-unseen', no_iniciado: 'chip-unseen', pendiente: 'chip-unseen', PLANNING: 'chip-unseen',
    en_progreso: 'chip-progress', leyendo: 'chip-progress',
    abandonado: 'chip-abandoned',
    seguimiento_episodios: 'chip-progress', seguimiento_canciones: 'chip-progress',
};
const label = (s) => STATUS_LABELS[s] || s || '—';
const chip  = (s) => STATUS_CHIP[s]  || '';

/* ─── Heart Button con actualización optimista ─── */
const HeartBtn = ({ itemId, initialFavorite, onUpdate }) => {
    const [fav, setFav] = useState(!!initialFavorite);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        setFav(!!initialFavorite);
    }, [initialFavorite]);

    const toggle = async (e) => {
        e.stopPropagation();
        if (loading) return;
        const newVal = !fav;
        setFav(newVal); 
        setLoading(true);
        try {
            const res = await fetch(`${API_BASE}/api/content/${itemId}/favorite`, { method: 'POST', credentials: 'include' });
            if (res.ok) {
                const data = await res.json();
                // El backend devuelve true/false o un objeto con favorite
                const finalFav = typeof data === 'object' ? data.favorite : data;
                setFav(!!finalFav);
                if (onUpdate) onUpdate(itemId, !!finalFav);
            } else {
                setFav(!newVal);
            }
        } catch {
            setFav(!newVal);
        } finally {
            setLoading(false);
        }
    };

    return (
        <button
            onClick={toggle}
            className={`heart-btn-new ${fav ? 'fav-active' : ''} ${loading ? 'fav-loading' : ''}`}
            title={fav ? 'Quitar de favoritos' : 'Marcar como favorito'}
        >
            <span className="heart-icon-scale">{fav ? '♥' : '♡'}</span>
        </button>
    );
};

/* ─── Status Chip ─── */
const StatusChip = ({ status }) => (
    <span className={`status-chip ${chip(status)}`}>{label(status)}</span>
);

/* ─── Star Rating (5 estrellas con media estrella) ─── */
const StarRating = ({ itemId, initialRating }) => {
    const [rating, setRating] = useState(initialRating || 0);
    const [hover, setHover] = useState(0);

    useEffect(() => { setRating(initialRating || 0); }, [initialRating]);

    const handleRate = async (val) => {
        const newRating = rating === val ? 0 : val;
        setRating(newRating);
        try {
            await fetch(`${API_BASE}/library/${itemId}/rating`, {
                method: 'PUT',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rating: newRating }),
            });
        } catch { /* ignored */ }
    };

    const activeVal = hover || rating;

    return (
        <div className="star-rating" onMouseLeave={() => setHover(0)}>
            {[1, 2, 3, 4, 5].map((star) => {
                let starClass = "";
                if (activeVal >= star) {
                    starClass = "star-full";
                } else if (activeVal >= star - 0.5) {
                    starClass = "star-half";
                }

                return (
                    <div 
                        key={star} 
                        className={`star-wrap ${starClass}`}
                        style={{ position: 'relative', display: 'inline-block' }}
                    >
                        <span className="star-bg">★</span>
                        <span className="star-fg">★</span>
                        
                        {/* Left half overlay */}
                        <div 
                            className="absolute left-0 top-0 w-1/2 h-full z-10 cursor-pointer"
                            onMouseEnter={() => setHover(star - 0.5)}
                            onClick={(e) => { e.stopPropagation(); handleRate(star - 0.5); }}
                            title={`${star - 0.5} estrellas`}
                        />
                        {/* Right half overlay */}
                        <div 
                            className="absolute right-0 top-0 w-1/2 h-full z-10 cursor-pointer"
                            onMouseEnter={() => setHover(star)}
                            onClick={(e) => { e.stopPropagation(); handleRate(star); }}
                            title={`${star} estrellas`}
                        />
                    </div>
                );
            })}
            {activeVal > 0 && <span className="star-val">{activeVal}</span>}
        </div>
    );
};

/* ─── Top Rank Selector ─── */
const TopRankSelector = ({ itemId, currentRank, onUpdate }) => {
    return (
        <div className="flex items-center gap-1.5 mt-3 pt-3 border-t border-[#ece7da]/50">
            <span className="text-[8px] font-black text-[#8c8471] uppercase tracking-widest mr-1">TOP</span>
            {[1, 2, 3, 4].map(r => (
                <button
                    key={r}
                    onClick={(e) => { e.stopPropagation(); onUpdate(itemId, r); }}
                    className={`w-5 h-5 rounded-md text-[9px] font-black transition-all border ${
                        currentRank === r 
                            ? 'bg-tcd-orange text-white border-tcd-orange shadow-sm' 
                            : 'bg-[#fcfaf5] text-[#8c8471] border-[#ece7da] hover:border-tcd-orange'
                    }`}
                >
                    {r}
                </button>
            ))}
            {currentRank && (
                <button 
                    onClick={(e) => { e.stopPropagation(); onUpdate(itemId, null); }} 
                    className="text-[12px] text-red-400 hover:text-red-600 ml-1"
                >
                    ×
                </button>
            )}
        </div>
    );
};

/* ─── Date Picker for Completion ─── */
const CompletionDateInput = ({ item, onUpdate }) => {
    const isCompleted = status => ['visto', 'leido', 'completado'].includes(status);
    if (!isCompleted(item.status)) return null;

    return (
        <div className="mt-2 group/date">
            <input 
                type="date" 
                defaultValue={item.completionDate}
                onChange={(e) => onUpdate(item.id, e.target.value)}
                className="text-[9px] font-bold bg-transparent border-none text-[#8c8471] cursor-pointer hover:text-tcd-orange transition-colors uppercase tracking-widest focus:ring-0 p-0"
                title="Fecha de finalización"
            />
        </div>
    );
};

/* ─── SectionHeader ─── */
const SectionHeader = ({ title, count, color }) => (
    <div className="section-hd" style={{ '--accent-col': color }}>
        <div className="flex items-center gap-3">
            <h2 className="section-title">{title}</h2>
        </div>
        <span className="count-pill">{count}</span>
    </div>
);

/* ══════════════════════════════════════════════
   PELÍCULAS — diseño tarjeta compacta
══════════════════════════════════════════════ */
const MovieCard = ({ item, onRefresh, onFavUpdate, onOpenDetails, onUpdateRank, onUpdateDate }) => {
    const [sel, setSel] = useState(item.status || 'no_visto');
    const [saving, setSaving] = useState(false);

    useEffect(() => { setSel(item.status || 'no_visto'); }, [item.status]);

    const handleUpdate = async () => {
        setSaving(true);
        await fetch(`${API_BASE}/library/${item.id}/movie-status`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: sel }),
        }).then(r => r.ok ? onRefresh() : alert('Error al actualizar estado'));
        setSaving(false);
    };

    const handleDelete = () => {
        if (window.confirm('¿Eliminar esta película?'))
            fetch(`${API_BASE}/library/${item.id}`, { method: 'DELETE', credentials: 'include' })
                .then(r => r.ok ? onRefresh() : alert('Error al eliminar'));
    };

    return (
        <div className="media-card group/card">
            <div className="media-card-cover cursor-pointer" onClick={() => onOpenDetails && onOpenDetails(item.content)}>
                {item.content.coverUrl
                    ? <img src={item.content.coverUrl} alt={item.content.title} className="hover:scale-105 transition-transform duration-500" />
                    : <div className="cover-placeholder">IMG</div>}
                <HeartBtn itemId={item.id} initialFavorite={item.favorite} onUpdate={onFavUpdate} />
                {item.topRank && <div className="top-rank-badge">#{item.topRank}</div>}
            </div>
            <div className="media-card-body">
                <p className="media-card-title cursor-pointer hover:text-tcd-orange transition-colors" title={item.content.title} onClick={() => onOpenDetails && onOpenDetails(item.content)}>{item.content.title}</p>
                <StarRating itemId={item.id} initialRating={item.rating} />
                <div className="flex justify-between items-center">
                    <StatusChip status={item.status} />
                    <CompletionDateInput item={item} onUpdate={onUpdateDate} />
                </div>
                <div className="media-card-controls">
                    <select value={sel} onChange={e => setSel(e.target.value)} className="mini-select">
                        <option value="visto">Visto</option>
                        <option value="no_visto">No visto</option>
                    </select>
                    <button onClick={handleUpdate} disabled={saving} className="mini-btn">
                        {saving ? '...' : 'OK'}
                    </button>
                    <button onClick={handleDelete} className="mini-btn danger-btn" title="Eliminar">DEL</button>
                </div>
                <TopRankSelector itemId={item.id} currentRank={item.topRank} onUpdate={onUpdateRank} />
            </div>
        </div>
    );
};

/* ══════════════════════════════════════════════
   LIBROS — diseño tarjeta con barra de progreso
══════════════════════════════════════════════ */
const BookCard = ({ item, onRefresh, onFavUpdate, onOpenDetails, onUpdateRank, onUpdateDate }) => {
    const [currPage, setCurrPage] = useState(item.bookCurrentPage ?? 0);
    const [totalPages, setTotalPages] = useState(item.bookTotalPages ?? 0);
    const [saving, setSaving] = useState(false);
    const [editing, setEditing] = useState(false);

    useEffect(() => {
        setCurrPage(item.bookCurrentPage ?? 0);
        setTotalPages(item.bookTotalPages ?? 0);
    }, [item.bookCurrentPage, item.bookTotalPages]);

    const pct = totalPages > 0 ? Math.min(100, Math.round((currPage / totalPages) * 100)) : 0;

    const handleSave = async () => {
        setSaving(true);
        await fetch(`${API_BASE}/library/${item.id}/book-progress`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ currentPage: Number(currPage), totalPages: Number(totalPages) }),
        }).then(r => r.ok ? onRefresh() : alert('Error al actualizar progreso'));
        setSaving(false);
        setEditing(false);
    };

    const handleDelete = () => {
        if (window.confirm('¿Eliminar este libro?'))
            fetch(`${API_BASE}/library/${item.id}`, { method: 'DELETE', credentials: 'include' })
                .then(r => r.ok ? onRefresh() : alert('Error al eliminar'));
    };

    return (
        <div className="media-card group/card">
            <div className="media-card-cover cursor-pointer" onClick={() => onOpenDetails && onOpenDetails(item.content)}>
                {item.content.coverUrl
                    ? <img src={item.content.coverUrl} alt={item.content.title} className="hover:scale-105 transition-transform duration-500" />
                    : <div className="cover-placeholder">IMG</div>}
                <HeartBtn itemId={item.id} initialFavorite={item.favorite} onUpdate={onFavUpdate} />
                <div className="cover-pct-badge">{pct}%</div>
                {item.topRank && <div className="top-rank-badge">#{item.topRank}</div>}
            </div>
            <div className="media-card-body">
                <p className="media-card-title cursor-pointer hover:text-tcd-orange transition-colors" title={item.content.title} onClick={() => onOpenDetails && onOpenDetails(item.content)}>{item.content.title}</p>
                <StarRating itemId={item.id} initialRating={item.rating} />
                <div className="flex justify-between items-center mb-1">
                    <StatusChip status={item.status} />
                    <CompletionDateInput item={item} onUpdate={onUpdateDate} />
                </div>
                <div className="progress-row">
                    <div className="progress-bar-wrap">
                        <div className="progress-bar-fill" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="progress-label">{currPage}/{totalPages}</span>
                </div>
                {editing ? (
                    <div className="media-card-controls">
                        <input type="number" min="0" value={currPage} onChange={e => setCurrPage(e.target.value)}
                            className="mini-input" placeholder="Pág actual" />
                        <input type="number" min="0" value={totalPages} onChange={e => setTotalPages(e.target.value)}
                            className="mini-input" placeholder="Total" />
                        <button onClick={handleSave} disabled={saving} className="mini-btn">{saving ? '...' : 'OK'}</button>
                        <button onClick={() => setEditing(false)} className="mini-btn">X</button>
                    </div>
                ) : (
                    <div className="media-card-controls">
                        <button onClick={() => setEditing(true)} className="mini-btn edit-btn">Editar progreso</button>
                        <button onClick={handleDelete} className="mini-btn danger-btn" title="Eliminar">DEL</button>
                    </div>
                )}
                <TopRankSelector itemId={item.id} currentRank={item.topRank} onUpdate={onUpdateRank} />
            </div>
        </div>
    );
};

/* ══════════════════════════════════════════════
   SERIES — tarjeta con episodios
══════════════════════════════════════════════ */
const SeriesCard = ({ item, episodes, onRefresh, onEpisodesRefresh, onFavUpdate, onOpenDetails, onUpdateRank, onUpdateDate }) => {
    const [genStatus, setGenStatus] = useState(item.status || 'pendiente');
    const [season, setSeason]   = useState('');
    const [episode, setEpisode] = useState('');
    const [watched, setWatched] = useState(false);
    const [saving, setSaving]   = useState(false);

    useEffect(() => { setGenStatus(item.status || 'pendiente'); }, [item.status]);

    const handleStatusUpdate = async () => {
        setSaving(true);
        await fetch(`${API_BASE}/library/${item.id}/status`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: genStatus }),
        }).then(r => r.ok ? onRefresh() : alert('Error al actualizar estado'));
        setSaving(false);
    };

    const handleSaveEpisode = () => {
        if (!season || !episode) return alert('Indica temporada y episodio');
        fetch(`${API_BASE}/library/${item.id}/episodes`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ seasonNumber: Number(season), episodeNumber: Number(episode), watched }),
        }).then(r => r.ok ? onEpisodesRefresh(item.id) : alert('Error al guardar episodio'));
    };

    const handleDelete = () => {
        if (window.confirm('¿Eliminar esta serie?'))
            fetch(`${API_BASE}/library/${item.id}`, { method: 'DELETE', credentials: 'include' })
                .then(r => r.ok ? onRefresh() : alert('Error al eliminar'));
    };

    const eps = episodes || [];
    const episodeCounts = item.seriesSeasonData ? item.seriesSeasonData.split('|').map(Number) : [];
    const selSeasonIdx = Number(season) - 1;
    const epsForSeason = (selSeasonIdx >= 0 && selSeasonIdx < episodeCounts.length) ? episodeCounts[selSeasonIdx] : 0;

    return (
        <div className="media-card series-card group/card">
            <div className="media-card-cover cursor-pointer" onClick={() => onOpenDetails && onOpenDetails(item.content)}>
                {item.content.coverUrl
                    ? <img src={item.content.coverUrl} alt={item.content.title} className="hover:scale-105 transition-transform duration-500" />
                    : <div className="cover-placeholder">IMG</div>}
                <HeartBtn itemId={item.id} initialFavorite={item.favorite} onUpdate={onFavUpdate} />
                {eps.length > 0 && (
                    <div className="cover-pct-badge">{eps.length} ep</div>
                )}
                {item.topRank && <div className="top-rank-badge">#{item.topRank}</div>}
            </div>
            <div className="media-card-body">
                <p className="media-card-title cursor-pointer hover:text-tcd-orange transition-colors" title={item.content.title} onClick={() => onOpenDetails && onOpenDetails(item.content)}>{item.content.title}</p>
                <StarRating itemId={item.id} initialRating={item.rating} />
                <div className="flex justify-between items-center mb-1">
                    <StatusChip status={item.status} />
                    <CompletionDateInput item={item} onUpdate={onUpdateDate} />
                </div>
                {item.seriesTotalSeasons && (
                    <p className="media-meta">{item.seriesTotalSeasons} temp · {item.seriesTotalEpisodes} ep</p>
                )}
                {eps.length > 0 && (
                    <div className="episodes-mini">
                        {eps.slice(-3).map(ep => (
                            <span key={`${ep.seasonNumber}-${ep.episodeNumber}`} className={`ep-chip ${ep.watched ? 'ep-seen' : 'ep-unseen'}`}>
                                T{ep.seasonNumber}E{ep.episodeNumber}
                            </span>
                        ))}
                    </div>
                )}
                <div className="media-card-controls flex-wrap">
                    <select value={genStatus} onChange={e => setGenStatus(e.target.value)} className="mini-select">
                        <option value="pendiente">Pendiente</option>
                        <option value="en_progreso">En progreso</option>
                        <option value="abandonado">Abandonado</option>
                    </select>
                    <button onClick={handleStatusUpdate} disabled={saving} className="mini-btn">{saving ? '...' : 'OK'}</button>
                </div>
                <div className="media-card-controls flex-wrap mt-2">
                    <div className="flex gap-1 w-full mb-1">
                        {episodeCounts.length > 0 && item.seriesTotalSeasons > 0 ? (
                            <>
                                <select value={season} onChange={e => { setSeason(e.target.value); setEpisode(''); }} className="mini-select">
                                    <option value="">Temp</option>
                                    {Array.from({ length: item.seriesTotalSeasons }, (_, i) => i + 1).map(n => (
                                        <option key={n} value={n}>{n}</option>
                                    ))}
                                </select>
                                <select value={episode} onChange={e => setEpisode(e.target.value)} className="mini-select" disabled={!season}>
                                    <option value="">Ep</option>
                                    {season && epsForSeason > 0 && Array.from({ length: epsForSeason }, (_, i) => i + 1).map(n => (
                                        <option key={n} value={n}>{n}</option>
                                    ))}
                                </select>
                            </>
                        ) : (
                            <>
                                <input type="number" min="1" placeholder="Temp" value={season} onChange={e => setSeason(e.target.value)} className="mini-input" style={{width:'50%'}} />
                                <input type="number" min="1" placeholder="Ep" value={episode} onChange={e => setEpisode(e.target.value)} className="mini-input" style={{width:'50%'}} />
                            </>
                        )}
                    </div>
                    <label className="mini-check">
                        <input type="checkbox" checked={watched} onChange={e => setWatched(e.target.checked)} /> Visto
                    </label>
                    <button onClick={handleSaveEpisode} className="mini-btn">+EP</button>
                    <button onClick={handleDelete} className="mini-btn danger-btn" title="Eliminar">DEL</button>
                </div>
                <TopRankSelector itemId={item.id} currentRank={item.topRank} onUpdate={onUpdateRank} />
            </div>
        </div>
    );
};

/* ══════════════════════════════════════════════
   DISCOS — tarjeta musical premium con vinilo
══════════════════════════════════════════════ */
const DiscCard = ({ item, songs, onRefresh, onSongsRefresh, onFavUpdate, onOpenDetails, onUpdateRank, onUpdateDate }) => {
    const [genStatus, setGenStatus] = useState(item.status || 'pendiente');
    const [trackNum, setTrackNum]   = useState('');
    const [trackTitle, setTrackTitle] = useState('');
    const [listened, setListened]   = useState(false);
    const [saving, setSaving]       = useState(false);
    const [expanded, setExpanded]   = useState(false);

    useEffect(() => { setGenStatus(item.status || 'pendiente'); }, [item.status]);

    const handleStatusUpdate = async () => {
        setSaving(true);
        await fetch(`${API_BASE}/library/${item.id}/status`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: genStatus }),
        }).then(r => r.ok ? onRefresh() : alert('Error al actualizar estado'));
        setSaving(false);
    };

    const handleSaveSong = () => {
        if (!trackNum || !trackTitle.trim()) return alert('Indica número y título de canción');
        fetch(`${API_BASE}/library/${item.id}/songs`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ trackNumber: Number(trackNum), trackTitle: trackTitle.trim(), listened }),
        }).then(r => {
            if (r.ok) {
                onSongsRefresh(item.id);
                setTrackNum('');
                setTrackTitle('');
                setListened(false);
            } else {
                alert('Error al guardar canción');
            }
        });
    };

    const handleDelete = () => {
        if (window.confirm('¿Eliminar este disco?'))
            fetch(`${API_BASE}/library/${item.id}`, { method: 'DELETE', credentials: 'include' })
                .then(r => r.ok ? onRefresh() : alert('Error al eliminar'));
    };

    const songList = songs || [];
    const listenedCount = songList.filter(s => s.listened).length;
    const totalTracks = item.albumTotalTracks || songList.length || 0;
    const pct = totalTracks > 0 && songList.length > 0 ? Math.round((listenedCount / totalTracks) * 100) : 0;

    const tracks = item.albumTrackList ? item.albumTrackList.split('|').map(t => {
        const idx = t.indexOf(':');
        const num = t.substring(0, idx);
        const title = idx !== -1 ? t.substring(idx + 1) : '';
        return { num, title };
    }) : null;

    return (
        <div className={`disc-card ${expanded ? 'disc-card-expanded' : ''} group/card`}>
            {/* Portada estilo vinilo */}
            <div className="disc-cover-wrap cursor-pointer" onClick={() => onOpenDetails && onOpenDetails(item.content)}>
                <div className="disc-vinyl-ring" />
                <div className="disc-cover-img">
                    {item.content.coverUrl
                        ? <img src={item.content.coverUrl} alt={item.content.title} className="hover:scale-110 transition-transform duration-[1s]" />
                        : <div className="cover-placeholder disc-ph">MUS</div>}
                </div>
                <HeartBtn itemId={item.id} initialFavorite={item.favorite} onUpdate={onFavUpdate} />
                {item.topRank && <div className="top-rank-badge" style={{left:'45%', top:'45%', zIndex:10}}>#{item.topRank}</div>}
            </div>

            {/* Info principal */}
            <div className="disc-body">
                <div className="disc-top">
                    <div className="w-full">
                        <p className="disc-title cursor-pointer hover:text-tcd-orange transition-colors" title={item.content.title} onClick={() => onOpenDetails && onOpenDetails(item.content)}>{item.content.title}</p>
                        <StarRating itemId={item.id} initialRating={item.rating} />
                        <div className="flex justify-between items-center mb-1">
                            <StatusChip status={item.status} />
                            <CompletionDateInput item={item} onUpdate={onUpdateDate} />
                        </div>
                        {totalTracks > 0 && (
                            <p className="disc-meta">{totalTracks} pistas · {listenedCount} escuchadas</p>
                        )}
                    </div>
                </div>

                {/* BARRA DE PROGRESO */}
                <div className="flex items-center gap-3 mt-3">
                    <button
                        onClick={() => setExpanded(!expanded)}
                        className={`disc-expand-btn ${expanded ? 'disc-expand-active' : ''}`}
                    >
                        {expanded ? 'LESS' : 'MORE'}
                    </button>
                    {totalTracks > 0 && (
                        <div className="flex-1 disc-progress h-2">
                             <div className="disc-progress-track">
                                <div className="disc-progress-fill" style={{ width: `${pct}%` }} />
                            </div>
                        </div>
                    )}
                </div>

                <div className="disc-controls mt-4">
                    <select value={genStatus} onChange={e => setGenStatus(e.target.value)} className="mini-select disc-select">
                        <option value="pendiente">Pendiente</option>
                        <option value="en_progreso">Escuchando</option>
                        <option value="abandonado">Abandonado</option>
                    </select>
                    <button onClick={handleStatusUpdate} disabled={saving} className="mini-btn">{saving ? '...' : 'OK'}</button>
                    <button onClick={handleDelete} className="mini-btn danger-btn" title="Eliminar">DEL</button>
                </div>

                <TopRankSelector itemId={item.id} currentRank={item.topRank} onUpdate={onUpdateRank} />

                {/* Panel expandido */}
                {expanded && (
                    <div className="disc-expanded-panel">
                        {/* Lista de canciones marcadas */}
                        {songList.length > 0 && (
                            <div className="disc-songs-list">
                                <p className="disc-panel-label">Canciones marcadas</p>
                                <div className="disc-songs-grid">
                                    {songList.map(s => (
                                        <div key={s.trackNumber} className={`disc-song-item ${s.listened ? 'song-listened' : 'song-pending'}`}>
                                            <span className="song-num">#{s.trackNumber}</span>
                                            <span className="song-title">{s.trackTitle}</span>
                                            <span className="song-status">{s.listened ? 'OK' : '..'}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Marcar canción */}
                        <div className="disc-add-song">
                            <p className="disc-panel-label">+ Marcar canción</p>
                            <div className="disc-add-controls">
                                {tracks && tracks.length > 0 ? (
                                    <select
                                        className="mini-select disc-select"
                                        onChange={e => {
                                            const val = e.target.value;
                                            if (val) {
                                                const parts = val.split(':');
                                                setTrackNum(parts[0]);
                                                setTrackTitle(parts.slice(1).join(':'));
                                            } else {
                                                setTrackNum(''); setTrackTitle('');
                                            }
                                        }}
                                        style={{ flex: 1 }}
                                    >
                                        <option value="">-- Seleccionar canción --</option>
                                        {tracks.map(t => (
                                            <option key={t.num} value={`${t.num}:${t.title}`}>#{t.num} — {t.title}</option>
                                        ))}
                                    </select>
                                ) : (
                                    <>
                                        <input type="number" min="1" placeholder="#" value={trackNum}
                                            onChange={e => setTrackNum(e.target.value)} className="mini-input" style={{ width: 50 }} />
                                        <input type="text" placeholder="Título de la canción" value={trackTitle}
                                            onChange={e => setTrackTitle(e.target.value)} className="mini-input" style={{ flex: 1 }} />
                                    </>
                                )}
                                <label className="mini-check">
                                    <input type="checkbox" checked={listened} onChange={e => setListened(e.target.checked)} />
                                    Escuchada
                                </label>
                                <button onClick={handleSaveSong} className="mini-btn">Guardar</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

/* ══════════════════════════════════════════════
   COMPONENTE PRINCIPAL
══════════════════════════════════════════════ */
const ProfilePage = () => {
    const navigate = useNavigate();
    const [data, setData]             = useState(null);
    const [loading, setLoading]       = useState(true);
    const [episodesByEntry, setEpisodesByEntry] = useState({});
    const [songsByEntry, setSongsByEntry]       = useState({});
    const [activeTab, setActiveTab]   = useState('all');

    // Profile customization states
    const [editingProfile, setEditingProfile] = useState(false);
    const [editUsername, setEditUsername] = useState('');
    const [editEmail, setEditEmail] = useState('');
    const [editBio, setEditBio] = useState('');
    const [editFavGenre, setEditFavGenre] = useState('');
    const [editProfilePic, setEditProfilePic] = useState('');

    useEffect(() => {
        if (data?.user) {
            setEditUsername(data.user.username || '');
            setEditEmail(data.user.email || '');
            setEditBio(data.user.bio || '');
            setEditFavGenre(data.user.favoriteGenre || '');
            setEditProfilePic(data.user.profilePicture || '');
        }
    }, [data]);

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 10 * 1024 * 1024) {
                alert("La imagen es demasiado grande. El límite es de 10MB.");
                return;
            }
            const reader = new FileReader();
            reader.onloadend = () => {
                setEditProfilePic(reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSaveProfile = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch(`${API_BASE}/api/profile/update`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: editUsername,
                    email: editEmail,
                    bio: editBio,
                    favoriteGenre: editFavGenre,
                    profilePicture: editProfilePic
                })
            });
            if (res.ok) {
                const updatedUser = await res.json();
                setData(prev => ({
                    ...prev,
                    user: updatedUser
                }));
                setEditingProfile(false);
                alert("¡Perfil actualizado con éxito!");
            } else {
                const msg = await res.text();
                alert(msg || "Error al actualizar el perfil");
            }
        } catch (err) {
            console.error("Error updating profile:", err);
            alert("Error de conexión al actualizar el perfil");
        }
    };

    const refreshEpisodes = useCallback((entryId) => {
        fetch(`${API_BASE}/library/${entryId}/episodes`, { credentials: 'include' })
            .then(r => r.json())
            .then(eps => setEpisodesByEntry(prev => ({ ...prev, [entryId]: eps })))
            .catch(() => {});
    }, []);

    const refreshSongs = useCallback((entryId) => {
        fetch(`${API_BASE}/library/${entryId}/songs`, { credentials: 'include' })
            .then(r => r.json())
            .then(songs => setSongsByEntry(prev => ({ ...prev, [entryId]: songs })))
            .catch(() => {});
    }, []);

    const refreshProfile = useCallback(() => {
        setLoading(true);
        fetch(`${API_BASE}/api/profile`, { headers: { Accept: 'application/json' }, credentials: 'include' })
            .then(r => r.json())
            .then(json => {
                setData(json);
                setLoading(false);
                (json.content || []).forEach(item => {
                    const t = (item.content.type || '').toLowerCase();
                    if (t === 'serie') refreshEpisodes(item.id);
                    if (t === 'disco') refreshSongs(item.id);
                });
            })
            .catch(err => { console.error(err); setLoading(false); });
    }, [refreshEpisodes, refreshSongs]);

    useEffect(() => { refreshProfile(); }, [refreshProfile]);

    // Actualización de favorito sin recargar todo
    const handleFavUpdate = useCallback((itemId, newVal) => {
        setData(prev => {
            if (!prev) return prev;
            return {
                ...prev,
                content: prev.content.map(item =>
                    item.id === itemId ? { ...item, favorite: newVal } : item
                )
            };
        });
    }, []);

    const [timeStats, setTimeStats] = useState(null);
    const refreshTimeStats = useCallback(() => {
        if (!data?.user?.id) return;
        fetch(`${API_BASE}/library/user/${data.user.id}/time-stats`, { credentials: 'include' })
            .then(res => {
                if (!res.ok) throw new Error('Stats not found');
                return res.json();
            })
            .then(json => setTimeStats(json))
            .catch(err => console.error("Error fetching time-stats:", err));
    }, [data?.user?.id]);

    useEffect(() => { 
        if (data?.user?.id) refreshTimeStats(); 
    }, [data?.user?.id, refreshTimeStats]);

    const handleUpdateRank = async (itemId, rank) => {
        try {
            const res = await fetch(`${API_BASE}/library/${itemId}/top-rank`, {
                method: 'PUT',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ topRank: rank })
            });
            if (res.ok) {
                refreshProfile();
            } else {
                alert('Error al actualizar ranking');
            }
        } catch (err) { 
            console.error(err); 
            alert('Error de conexión al actualizar ranking');
        }
    };

    const handleUpdateDate = async (itemId, date) => {
        try {
            const res = await fetch(`${API_BASE}/library/${itemId}/completion-date`, {
                method: 'PUT',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ completionDate: date })
            });
            if (res.ok) {
                refreshProfile();
                refreshTimeStats();
            } else {
                alert('Error al actualizar fecha de finalización');
            }
        } catch (err) { 
            console.error(err); 
            alert('Error de conexión al actualizar fecha');
        }
    };

    const handleOpenDetails = (content) => {
        let source = 'TMDb';
        const type = (content.type || '').toLowerCase();
        if (type === 'disco') source = 'Spotify';
        if (type === 'libro') {
            source = (content.externalId && (content.externalId.toLowerCase().includes('ol') || content.externalId.toLowerCase().includes('works'))) 
                ? 'OpenLibrary' : 'GoogleBooks';
        }
        navigate(`/details/${source}/${type}/${content.externalId}`);
    };

    const chartData = useMemo(() => {
        if (!timeStats || !timeStats.history) return [];
        return Object.entries(timeStats.history).slice(-14).map(([date, count]) => ({
            name: new Date(date).toLocaleDateString('es-ES', { day: '2-digit', month: 'short' }),
            consumido: count
        }));
    }, [timeStats]);

    /* ── Pantalla de carga ── */
    if (loading && !data) return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] gap-8">
            <div className="avatar-large animate-pulse bg-[#f4efdf] opacity-50"></div>
            <div className="text-xl font-black opacity-20 italic uppercase tracking-[0.5em]">Sincronizando Archivo...</div>
        </div>
    );

    const user       = data?.user       || { username: 'USUARIO' };
    const stats      = data?.stats      || {};
    const libraryItems = data?.content    || [];
    const totalCount = libraryItems.length;
    const favEntries = libraryItems.filter(i => i.favorite);

    const movieEntries = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'pelicula');
    const seriesEntries = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'serie');
    const bookEntries = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'libro');
    const discEntries = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'disco');

    const top4Movies = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'pelicula' && i.topRank).sort((a,b) => a.topRank - b.topRank);
    const top4Series = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'serie' && i.topRank).sort((a,b) => a.topRank - b.topRank);
    const top4Books  = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'libro' && i.topRank).sort((a,b) => a.topRank - b.topRank);
    const top4Music  = libraryItems.filter(i => (i.content.type || '').toLowerCase() === 'disco' && i.topRank).sort((a,b) => a.topRank - b.topRank);

    /* ── Filtrado por pestañas ── */
    const TABS = [
        { key: 'all',      label: 'Todo',    count: libraryItems.length },
        { key: 'pelicula', label: 'Cine',    count: stats.peliculas || 0 },
        { key: 'serie',    label: 'Series',  count: stats.series || 0 },
        { key: 'libro',    label: 'Libros',  count: stats.libros || 0 },
        { key: 'disco',    label: 'Música',  count: stats.discos || 0 },
        { key: 'favs',     label: 'Favs',    count: favEntries.length },
    ];

    const currentTab = TABS.find(t => t.key === activeTab) || TABS[0];

    const getTabItems = () => {
        if (activeTab === 'all') return libraryItems;
        if (activeTab === 'favs') return favEntries;
        return libraryItems.filter(i => {
            const t = (i.content.type || '').toLowerCase();
            return t === activeTab;
        });
    };

    const tabItems = getTabItems();

    const showAll    = activeTab === 'all';
    const showMovies = activeTab === 'all' || activeTab === 'pelicula';
    const showSeries = activeTab === 'all' || activeTab === 'serie';
    const showBooks  = activeTab === 'all' || activeTab === 'libro';
    const showDiscs  = activeTab === 'all' || activeTab === 'disco';
    const showFavs   = activeTab === 'favs';

    return (
        <main className="profile-container animate-fade-in pb-20 pt-10 px-4 md:px-0">
            {/* ── SECCIÓN CABECERA PERFIL ── */}
            <section className="profile-hero mb-16 p-8 lg:p-16 rounded-[60px] relative overflow-hidden bg-white border border-[#ece7da] shadow-2xl">
                {/* Decoración de fondo */}
                <div className="absolute top-0 right-0 w-1/2 h-full bg-gradient-to-l from-tcd-orange/5 to-transparent pointer-events-none"></div>
                
                <div className="flex flex-col lg:flex-row items-center lg:items-end gap-12 relative z-10">
                    <div className="avatar-large-container group">
                        <div className="avatar-large shadow-2xl border-4 border-white transition-transform duration-700 group-hover:scale-105">
                            {user.profilePicture 
                                ? <img src={user.profilePicture} alt={user.username} className="w-full h-full object-cover" />
                                : <div className="w-full h-full bg-[#f4efdf] flex items-center justify-center text-4xl font-black text-[#8c8471]">{user.username?.charAt(0).toUpperCase()}</div>
                            }
                        </div>
                    </div>
                    
                    <div className="flex-1 text-center lg:text-left">
                        <span className="text-[10px] font-black tracking-[0.5em] text-tcd-orange uppercase mb-4 block">PERFIL ARCHIVISTA</span>
                        <h1 className="text-6xl lg:text-8xl font-black text-[#2d2a26] uppercase italic tracking-tighter leading-[0.8] mb-8">
                            {user.username}<span className="text-tcd-orange">.</span>
                        </h1>
                        <p className="text-xl text-[#8c8471] font-medium leading-relaxed max-w-2xl italic mb-10">
                            {user.bio || "Explorador del archivo cultural universal. Documentando cada historia, cada nota y cada palabra."}
                        </p>
                        
                        <div className="flex flex-wrap items-center justify-center lg:justify-start gap-6">
                            <div className="summary-pill flex flex-col items-center lg:items-end py-4 px-8 rounded-3xl">
                                <span className="text-[8px] uppercase font-black tracking-widest text-[#8c8471] mb-0.5">INDEXADO</span>
                                <strong className="text-3xl font-black tracking-tighter text-[#2d2a26] leading-none">{totalCount}</strong>
                            </div>
                            <div className="flex gap-3">
                                <button onClick={refreshProfile} title="Sincronizar" className="w-[42px] h-[42px] rounded-full border border-[#ece7da] flex items-center justify-center bg-white hover:shadow-xl hover:scale-105 transition-all hover:border-tcd-orange/30">
                                    <span className={`text-xl text-[#8c8471] leading-none select-none ${loading ? 'animate-spin' : ''}`}>↻</span>
                                </button>
                                <button onClick={() => setEditingProfile(true)} className="bg-white border border-[#ece7da] text-[9px] font-black uppercase tracking-widest text-[#8c8471] hover:text-[#b8601a] px-6 py-4 rounded-full shadow-sm hover:shadow-md hover:scale-105 transition-all">
                                    Editar Perfil
                                </button>
                                <button onClick={() => window.location.href = '/explorar'} className="btn-community group px-6 py-2.5">
                                    <span className="flex flex-col items-center">
                                        <span className="text-[8px] font-black tracking-widest opacity-60 mb-0.5">AÑADIR</span>
                                        <span className="text-[10px]">+ CONTENIDO</span>
                                    </span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* ── DASHBOARD STATS & CHARTS ── */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-16">
                <div className="lg:col-span-2 bg-[#fcfaf5] p-10 rounded-[50px] border border-[#ece7da] shadow-sm">
                    <div className="flex justify-between items-center mb-10">
                        <div>
                            <p className="text-[10px] font-black text-tcd-orange uppercase tracking-[0.4em] mb-2">ACTIVIDAD DEL ARCHIVO</p>
                            <h3 className="text-2xl font-black text-[#2d2a26] uppercase italic tracking-tighter">Histórico de consumo</h3>
                        </div>
                        <div className="flex gap-8">
                            <div className="text-center">
                                <p className="text-[9px] font-black text-[#8c8471] uppercase mb-1 opacity-60 italic">ESTA SEMANA</p>
                                <p className="text-2xl font-black text-tcd-orange">{timeStats?.thisWeek || 0}</p>
                            </div>
                            <div className="text-center">
                                <p className="text-[9px] font-black text-[#8c8471] uppercase mb-1 opacity-60 italic">ESTE MES</p>
                                <p className="text-2xl font-black text-[#2d2a26]">{timeStats?.thisMonth || 0}</p>
                            </div>
                        </div>
                    </div>
                    
                    <div className="h-[260px] w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="gradConsumo" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#c4621a" stopOpacity={0.3}/>
                                        <stop offset="95%" stopColor="#c4621a" stopOpacity={0}/>
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#ece7da" />
                                <XAxis 
                                    dataKey="name" 
                                    axisLine={false} 
                                    tickLine={false} 
                                    tick={{fontSize: 9, fontWeight: 900, fill: '#8c8471'}} 
                                />
                                <YAxis 
                                    axisLine={false} 
                                    tickLine={false} 
                                    tick={{fontSize: 9, fontWeight: 900, fill: '#8c8471'}}
                                    allowDecimals={false}
                                />
                                <Tooltip 
                                    contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 25px rgba(0,0,0,0.12)', fontWeight: 900, fontSize: '10px', background: '#fff' }}
                                    cursor={{ stroke: '#c4621a', strokeWidth: 1, strokeDasharray: '4 4' }}
                                    formatter={(v) => [v, 'Consumido']}
                                />
                                <Area 
                                    type="monotone" 
                                    dataKey="consumido" 
                                    stroke="#b8601a" 
                                    strokeWidth={3}
                                    fill="url(#gradConsumo)"
                                    dot={{ r: 5, fill: '#b8601a', strokeWidth: 2, stroke: '#fff' }}
                                    activeDot={{ r: 7, strokeWidth: 0, fill: '#c4621a' }}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                    {[
                        { label: 'Cine',    val: stats.peliculas, unit: 'FILMS',  icon: 'MOV' },
                        { label: 'TV',      val: stats.series,    unit: 'SERIES', icon: 'TV' },
                        { label: 'Libros',  val: stats.libros,    unit: 'VOLS',   icon: 'BK' },
                        { label: 'Música',  val: stats.discos,    unit: 'ALBUMS', icon: 'MS' },
                    ].map((s, i) => (
                        <div key={i} className="stat-card p-6 border border-[#ece7da] rounded-[30px] bg-white group hover:border-[#b8601a]/30 transition-all flex flex-col justify-between">
                            <div>
                                <span className="text-[10px] font-black mb-1 block opacity-20">{s.icon}</span>
                                <span className="text-[9px] font-black uppercase tracking-widest text-[#8c8471] opacity-60">{s.label}</span>
                            </div>
                            <div className="flex items-baseline gap-2">
                                <strong className="text-3xl font-black tracking-tighter text-[#2d2a26] group-hover:text-tcd-orange transition-colors">{s.val ?? 0}</strong>
                                <span className="text-[9px] font-black text-tcd-orange/50 uppercase tracking-widest">{s.unit}</span>
                            </div>
                        </div>
                    ))}
                    <div className="col-span-2 stat-card p-6 border border-[#ece7da] rounded-[30px] bg-[#2d2a26] text-white flex items-center justify-between">
                        <div>
                            <span className="text-[9px] font-black uppercase tracking-widest opacity-60 mb-1 block">MIS FAVORITOS</span>
                            <strong className="text-4xl font-black tracking-tighter">{favEntries.length}</strong>
                        </div>
                        <span className="text-2xl">♥</span>
                    </div>
                </div>
            </div>

            {/* ── TOP 4 SECTION ── */}
            <section className="mb-20">
                <div className="flex items-center gap-6 mb-12">
                     <h2 className="text-4xl font-black text-[#2d2a26] uppercase italic tracking-tighter">MI TOP 4 <span className="text-tcd-orange">PERSONAL</span></h2>
                     <div className="h-px bg-[#ece7da] flex-1"></div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12">
                    {[
                        { title: 'FILMOGRAFÍA', items: top4Movies, accent: '#c4621a', imgClass: 'rounded-lg' },
                        { title: 'DISCOGRAFÍA', items: top4Music,  accent: '#1e1c18', imgClass: 'rounded-full animate-spin-slow' },
                        { title: 'BIBLIOTECA',   items: top4Books,  accent: '#8b8471', imgClass: 'rounded-lg' },
                        { title: 'TELEVISIÓN',  items: top4Series, accent: '#134e4a', imgClass: 'rounded-lg' }
                    ].map((sec, idx) => (
                        <div key={idx} className="bg-[#fcfaf5] p-8 rounded-[40px] border border-[#ece7da]">
                            <div className="flex items-center justify-between mb-8 border-b border-[#ece7da] pb-4">
                                <p className="text-[10px] font-black text-tcd-orange uppercase tracking-[0.4em]">{sec.title}</p>
                            </div>
                            <div className="flex flex-col gap-6">
                                {[1,2,3,4].map(rank => {
                                    const item = sec.items.find(m => m.topRank === rank);
                                    return (
                                        <div key={rank} className="flex items-center gap-4 group cursor-pointer" onClick={() => item && handleOpenDetails(item.content)}>
                                            <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 font-black text-sm italic ${item ? 'text-white' : 'bg-[#ece7da] text-[#8c8471]'}`} style={item ? { background: sec.accent } : {}}>
                                                {rank}
                                            </div>
                                            {item ? (
                                                <div className="flex items-center gap-3 overflow-hidden">
                                                    <img src={item.content.coverUrl} className={`w-12 h-16 object-cover shadow-md ${sec.imgClass}`} alt="" />
                                                    <p className="font-black text-[11px] uppercase tracking-tighter truncate leading-tight">{item.content.title}</p>
                                                </div>
                                            ) : <p className="text-[10px] font-bold text-[#8c8471]/40 uppercase italic">Añadir top {rank}...</p>}
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
            </section>

            {/* ── TABS ── */}
            <div className="flex gap-2 flex-wrap mb-8 border-b border-[#ece7da] pb-4">
                {TABS.map(tab => (
                    <button
                        key={tab.key}
                        onClick={() => setActiveTab(tab.key)}
                        className={`px-5 py-2 rounded-full text-[10px] font-black uppercase tracking-widest transition-all ${
                            activeTab === tab.key
                                ? 'bg-[#2d2a26] text-white shadow-md'
                                : 'bg-[#f0ece3] text-[#8c8471] hover:text-[#2d2a26]'
                        }`}
                    >
                        {tab.label} {tab.count > 0 && <span className="ml-1 opacity-60">({tab.count})</span>}
                    </button>
                ))}
            </div>

            {libraryItems.length === 0 ? (
                <div className="py-40 text-center glass-card border-dashed">
                    <span className="text-4xl font-black text-[#2d2a26]/10 uppercase tracking-[0.4em] italic mb-12 block">Archivo vacío</span>
                    <button onClick={() => window.location.href = '/explorar'} className="bg-[#2d2a26] text-white px-12 py-5 rounded-full font-black text-xs uppercase tracking-[0.2em] hover:bg-tcd-orange transition-all shadow-2xl">
                        Indexar Contenido
                    </button>
                </div>
            ) : (
                <>
                    {/* ═══ FAVORITOS ═══ */}
                    {showFavs && (
                        <section className="mb-16">
                            <SectionHeader title="Favoritos" count={favEntries.length} color="#e91e8c" />
                            {favEntries.length === 0 ? (
                                <div className="empty-section">Aún no tienes favoritos. Usa el corazón en cualquier ítem.</div>
                            ) : (
                                <div className="media-grid">
                                    {favEntries.filter(i => (i.content.type || '').toLowerCase() === 'pelicula').map(item => (
                                        <MovieCard key={item.id} item={item} onRefresh={refreshProfile} onFavUpdate={handleFavUpdate} onOpenDetails={handleOpenDetails} onUpdateRank={handleUpdateRank} onUpdateDate={handleUpdateDate} />
                                    ))}
                                    {favEntries.filter(i => (i.content.type || '').toLowerCase() === 'libro').map(item => (
                                        <BookCard key={item.id} item={item} onRefresh={refreshProfile} onFavUpdate={handleFavUpdate} onOpenDetails={handleOpenDetails} onUpdateRank={handleUpdateRank} onUpdateDate={handleUpdateDate} />
                                    ))}
                                    {favEntries.filter(i => (i.content.type || '').toLowerCase() === 'serie').map(item => (
                                        <SeriesCard key={item.id} item={item} episodes={episodesByEntry[item.id]} onRefresh={refreshProfile} onEpisodesRefresh={refreshEpisodes} onFavUpdate={handleFavUpdate} onOpenDetails={handleOpenDetails} onUpdateRank={handleUpdateRank} onUpdateDate={handleUpdateDate} />
                                    ))}
                                    {favEntries.filter(i => (i.content.type || '').toLowerCase() === 'disco').map(item => (
                                        <DiscCard key={item.id} item={item} songs={songsByEntry[item.id]} onRefresh={refreshProfile} onSongsRefresh={refreshSongs} onFavUpdate={handleFavUpdate} onOpenDetails={handleOpenDetails} onUpdateRank={handleUpdateRank} onUpdateDate={handleUpdateDate} />
                                    ))}
                                </div>
                            )}
                        </section>
                    )}


                    {/* ═══ PELÍCULAS ═══ */}
                    {showMovies && movieEntries.length > 0 && (
                        <section className="mb-16">
                            <SectionHeader title="Películas" count={movieEntries.length} color="#c2410c" />
                            <div className="media-grid">
                                {movieEntries.map(item => (
                                    <MovieCard key={item.id} item={item} onRefresh={refreshProfile} onFavUpdate={handleFavUpdate} onOpenDetails={handleOpenDetails} onUpdateRank={handleUpdateRank} onUpdateDate={handleUpdateDate} />
                                ))}
                            </div>
                        </section>
                    )}

                    {/* ═══ LIBROS ═══ */}
                    {showBooks && bookEntries.length > 0 && (
                        <section className="mb-16">
                            <SectionHeader title="Libros" count={bookEntries.length} color="#b45309" />
                            <div className="media-grid">
                                {bookEntries.map(item => (
                                    <BookCard key={item.id} item={item} onRefresh={refreshProfile} onFavUpdate={handleFavUpdate} onOpenDetails={handleOpenDetails} onUpdateRank={handleUpdateRank} onUpdateDate={handleUpdateDate} />
                                ))}
                            </div>
                        </section>
                    )}

                    {/* ═══ SERIES ═══ */}
                    {showSeries && seriesEntries.length > 0 && (
                        <section className="mb-16">
                            <SectionHeader title="Series" count={seriesEntries.length} color="#b45309" />
                            <div className="media-grid">
                                {seriesEntries.map(item => (
                                    <SeriesCard
                                        key={item.id}
                                        item={item}
                                        episodes={episodesByEntry[item.id]}
                                        onRefresh={refreshProfile}
                                        onEpisodesRefresh={refreshEpisodes}
                                        onFavUpdate={handleFavUpdate}
                                        onOpenDetails={handleOpenDetails}
                                        onUpdateRank={handleUpdateRank}
                                        onUpdateDate={handleUpdateDate}
                                    />
                                ))}
                            </div>
                        </section>
                    )}

                    {/* ═══ DISCOS ═══ */}
                    {showDiscs && discEntries.length > 0 && (
                        <section className="mb-16">
                            <SectionHeader title="Música" count={discEntries.length} color="#db2777" />
                            <div className="disc-grid">
                                {discEntries.map(item => (
                                    <DiscCard
                                        key={item.id}
                                        item={item}
                                        songs={songsByEntry[item.id]}
                                        onRefresh={refreshProfile}
                                        onSongsRefresh={refreshSongs}
                                        onFavUpdate={handleFavUpdate}
                                        onOpenDetails={handleOpenDetails}
                                        onUpdateRank={handleUpdateRank}
                                        onUpdateDate={handleUpdateDate}
                                    />
                                ))}
                            </div>
                        </section>
                    )}
                </>
            )}

            {/* Modal de Edición de Perfil */}
            {editingProfile && (
                <div className="fixed inset-0 bg-[#2d2a26]/90 backdrop-blur-md flex items-start justify-center z-[2000] p-0 overflow-y-auto">
                    <div className="bg-white rounded-t-[40px] md:rounded-[40px] border border-[#ece7da] p-8 md:p-12 w-full max-w-2xl mt-0 md:mt-10 mb-20 shadow-[0_25px_80px_rgba(0,0,0,0.5)] animate-fade-in relative">
                        <div className="flex justify-between items-center mb-10 sticky top-4 bg-white z-10 py-2">
                            <h2 className="text-3xl font-black italic tracking-tighter text-[#2d2a26] uppercase">
                                Editar Perfil<span className="text-tcd-orange">.</span>
                            </h2>
                            <button onClick={() => setEditingProfile(false)} className="w-10 h-10 rounded-full border border-[#ece7da] flex items-center justify-center hover:bg-[#f4efdf] transition-all font-bold">X</button>
                        </div>

                        <form onSubmit={handleSaveProfile} className="space-y-8">
                            <div className="flex flex-col md:flex-row gap-8 items-center mb-8">
                                <div className="w-24 h-24 text-3xl overflow-hidden bg-[#f4efdf] flex items-center justify-center border-4 border-[#ece7da] flex-shrink-0 rounded-full shadow-lg" style={{background: 'linear-gradient(135deg,#c4621a,#e07a3a)', color:'#fff'}}>
                                    {editProfilePic ? (
                                        <img src={editProfilePic} alt="Previsualización" className="w-full h-full object-cover" />
                                    ) : (
                                        editUsername.charAt(0).toUpperCase()
                                    )}
                                </div>
                                <div className="flex-1 w-full">
                                    <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-2">Foto de Perfil</label>
                                    <input 
                                        type="file" 
                                        accept="image/*"
                                        onChange={handleFileChange}
                                        className="w-full bg-[#fdfaf5] border border-[#ece7da] p-4 rounded-2xl text-xs font-bold"
                                    />
                                    <p className="text-[9px] text-[#8c8471] mt-2 uppercase tracking-wider font-semibold">O pega una URL de imagen:</p>
                                    <input 
                                        type="text"
                                        value={editProfilePic}
                                        onChange={e => setEditProfilePic(e.target.value)}
                                        className="w-full bg-[#fdfaf5] border border-[#ece7da] p-4 rounded-2xl text-xs font-bold mt-2"
                                        placeholder="https://ejemplo.com/avatar.jpg"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-2">Nombre de Usuario</label>
                                    <input 
                                        type="text" 
                                        required
                                        value={editUsername}
                                        onChange={e => setEditUsername(e.target.value)}
                                        className="w-full bg-[#fdfaf5] border border-[#ece7da] p-5 rounded-2xl focus:outline-none focus:border-tcd-orange font-bold text-xs"
                                    />
                                </div>
                                <div>
                                    <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-2">Email</label>
                                    <input 
                                        type="email" 
                                        required
                                        value={editEmail}
                                        onChange={e => setEditEmail(e.target.value)}
                                        className="w-full bg-[#fdfaf5] border border-[#ece7da] p-5 rounded-2xl focus:outline-none focus:border-tcd-orange font-bold text-xs"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-2">Biografía</label>
                                <textarea 
                                    value={editBio}
                                    onChange={e => setEditBio(e.target.value)}
                                    className="w-full bg-[#fdfaf5] border border-[#ece7da] p-5 rounded-2xl focus:outline-none focus:border-tcd-orange font-bold text-xs h-24"
                                    placeholder="Cuéntanos sobre tus gustos culturales..."
                                />
                            </div>

                            <div>
                                <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-2">Género Favorito</label>
                                <input 
                                    type="text" 
                                    value={editFavGenre}
                                    onChange={e => setEditFavGenre(e.target.value)}
                                    className="w-full bg-[#fdfaf5] border border-[#ece7da] p-5 rounded-2xl focus:outline-none focus:border-tcd-orange font-bold text-xs"
                                    placeholder="Ej: Cine de Autor, Indie Rock, Sci-Fi..."
                                />
                            </div>

                            <div className="flex gap-4 pt-4 border-t border-dashed border-[#ece7da]">
                                <button type="submit" className="flex-1 bg-[#b8601a] text-white py-5 rounded-2xl font-black uppercase tracking-widest text-xs hover:bg-[#a05015] transition-all">
                                    GUARDAR CAMBIOS
                                </button>
                                <button type="button" onClick={() => setEditingProfile(false)} className="px-8 bg-white border border-[#ece7da] text-[#8c8471] py-5 rounded-2xl font-black uppercase tracking-widest text-xs hover:bg-[#f4efdf] transition-all">
                                    CANCELAR
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </main>
    );
};

export default ProfilePage;
