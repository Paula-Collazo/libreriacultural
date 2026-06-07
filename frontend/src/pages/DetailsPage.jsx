import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import AddToLibraryModal from '../components/AddToLibraryModal';

const API_BASE = 'http://localhost:8083';

const HeartBtn = ({ itemId, initialFavorite, onUpdate }) => {
    const [fav, setFav] = useState(!!initialFavorite);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        setFav(!!initialFavorite);
    }, [initialFavorite]);

    const toggle = async (e) => {
        e.stopPropagation();
        if (!itemId || loading) return;
        const newVal = !fav;
        setFav(newVal); 
        setLoading(true);
        try {
            const res = await fetch(`${API_BASE}/api/content/${itemId}/favorite`, { method: 'POST', credentials: 'include' });
            if (res.ok) {
                const data = await res.json();
                const finalFav = typeof data === 'object' ? data.favorite : data;
                setFav(!!finalFav);
                if (onUpdate) onUpdate(!!finalFav);
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
            className={`w-14 h-14 rounded-full flex items-center justify-center transition-all ${fav ? 'bg-tcd-orange text-white shadow-lg' : 'bg-white text-[#8c8471] border border-[#ece7da] hover:border-tcd-orange hover:text-tcd-orange'}`}
        >
            <span className="text-2xl leading-none">{fav ? '♥' : '♡'}</span>
        </button>
    );
};

const DetailsPage = () => {
    const { source, type, '*': id } = useParams();
    const navigate = useNavigate();
    const [itemDetails, setItemDetails] = useState(null);
    const [loading, setLoading] = useState(true);
    const [relatedItems, setRelatedItems] = useState([]);
    const [actorDetails, setActorDetails] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [lists, setLists] = useState([]);
    const [showListDropdown, setShowListDropdown] = useState(false);
    const [addingToList, setAddingToList] = useState(null);

    const fetchLists = async () => {
        try {
            const res = await fetch(`${API_BASE}/api/lists`, { credentials: 'include' });
            if (res.ok) {
                const data = await res.json();
                setLists(data);
            }
        } catch (err) {
            console.error("Error fetching lists:", err);
        }
    };

    useEffect(() => {
        fetchLists();
    }, []);

    const handleToggleList = async (listId, isAlreadyInList) => {
        if (addingToList) return;
        setAddingToList(listId);
        try {
            let userContentId = itemDetails.libraryId;

            // If not in library, add it first with default planning status
            if (!itemDetails.inLibrary) {
                const addRes = await fetch(`${API_BASE}/api/content/add`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        externalId: itemDetails.externalId || id,
                        type: itemDetails.type || type.toUpperCase(),
                        title: itemDetails.title,
                        imageUrl: itemDetails.coverUrl,
                        status: 'PLANNING',
                        favorite: false
                    })
                });

                if (addRes.ok || addRes.status === 400) {
                    // Refetch details to get the libraryId
                    const detailsRes = await fetch(`${API_BASE}/api/external/details?source=${source}&id=${id}&type=${type.toUpperCase()}`, { credentials: 'include' });
                    if (detailsRes.ok) {
                        const updatedDetails = await detailsRes.json();
                        setItemDetails(updatedDetails);
                        userContentId = updatedDetails.libraryId;
                    }
                } else {
                    const msg = await addRes.text();
                    alert(msg || "Error al añadir a la biblioteca");
                    setAddingToList(null);
                    return;
                }
            }

            if (!userContentId) {
                alert("Error al obtener el ID de biblioteca");
                setAddingToList(null);
                return;
            }

            const method = isAlreadyInList ? 'DELETE' : 'POST';
            const res = await fetch(`${API_BASE}/api/lists/${listId}/items/${userContentId}`, {
                method,
                credentials: 'include'
            });

            if (res.ok) {
                await fetchLists();
            } else {
                alert("Error al actualizar la lista");
            }
        } catch (err) {
            console.error("Error toggling item in list:", err);
        } finally {
            setAddingToList(null);
        }
    };

    useEffect(() => {
        const fetchDetails = async () => {
            setLoading(true);
            window.scrollTo({ top: 0, behavior: 'instant' });
            try {
                const res = await fetch(`${API_BASE}/api/external/details?source=${source}&id=${id}&type=${type.toUpperCase()}`, { credentials: 'include' });
                if (res.ok) {
                    const data = await res.json();
                    setItemDetails(data);
                }
            } catch (err) {
                console.error("Error fetching details:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchDetails();
    }, [source, type, id]);

    // Fetch related content
    useEffect(() => {
        if (itemDetails && (itemDetails.author || itemDetails.artist)) {
            const name = itemDetails.author || itemDetails.artist;
            const searchType = type.toUpperCase() === 'LIBRO' ? 'LIBRO' : 'DISCO';
            
            fetch(`${API_BASE}/api/external/search?query=${encodeURIComponent(name)}&type=${searchType}`, { credentials: 'include' })
                .then(res => res.json())
                .then(data => {
                    if (Array.isArray(data)) {
                        setRelatedItems(data.filter(it => it.externalId !== id).slice(0, 8));
                    }
                })
                .catch(err => console.error("Error fetching related:", err));
        }
    }, [itemDetails, id, type]);

    const fetchActorDetails = async (name) => {
        try {
            const res = await fetch(`${API_BASE}/api/external/actor?name=${encodeURIComponent(name)}`, { credentials: 'include' });
            if (res.ok) {
                const data = await res.json();
                setActorDetails(data);
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        } catch (err) {
            console.error("Error fetching actor details:", err);
        }
    };

    const updateMetadata = async (field, value) => {
        if (!itemDetails.libraryId) return;
        try {
            const endpoint = field === 'topRank' ? 'top-rank' : 'completion-date';
            const bodyObj = field === 'topRank' ? { topRank: value } : { completionDate: value };
            
            const res = await fetch(`${API_BASE}/library/${itemDetails.libraryId}/${endpoint}`, {
                method: 'PUT',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bodyObj)
            });
            if (res.ok) {
                const updated = await res.json();
                setItemDetails(prev => ({ 
                    ...prev, 
                    topRank: updated.topRank, 
                    completionDate: updated.completionDate 
                }));
            }
        } catch (err) {
            console.error("Error updating metadata:", err);
        }
    };

    const handleConfirmAdd = async ({ status, completionDate, rating, favorite }) => {
        setIsModalOpen(false);
        try {
            const res = await fetch(`${API_BASE}/api/content/add`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    externalId: itemDetails.externalId || id,
                    type: itemDetails.type || type.toUpperCase(),
                    title: itemDetails.title,
                    imageUrl: itemDetails.coverUrl,
                    status,
                    completionDate,
                    rating,
                    favorite
                })
            });
            if (res.ok) {
                // Refetch details to get updated state from backend
                const detailsRes = await fetch(`${API_BASE}/api/external/details?source=${source}&id=${id}&type=${type.toUpperCase()}`, { credentials: 'include' });
                if (detailsRes.ok) {
                    const data = await detailsRes.json();
                    setItemDetails(data);
                }
                alert(`¡${itemDetails.title} añadido a tu biblioteca!`);
            } else {
                const msg = await res.text();
                alert(msg);
            }
        } catch (err) {
            console.error("Error adding to library:", err);
        }
    };

    if (loading) {
        return (
            <div className="flex flex-col items-center justify-center py-40">
                <div className="w-16 h-16 border-4 border-tcd-orange border-t-transparent rounded-full animate-spin mb-6"></div>
                <p className="text-xs font-black text-[#8c8471] uppercase tracking-[0.4em] animate-pulse">Recuperando archivo cultural...</p>
            </div>
        );
    }

    if (!itemDetails) {
        return <div className="py-40 text-center font-black uppercase tracking-[0.3em] opacity-20">No se encontró información.</div>;
    }

    // View Actor Profile
    if (actorDetails) {
        return (
            <div className="animate-fade-in pb-20">
                <button onClick={() => setActorDetails(null)} className="mb-12 flex items-center gap-4 text-[#8c8471] hover:text-black transition-all group">
                    <div className="w-10 h-10 rounded-full border border-[#ece7da] flex items-center justify-center group-hover:bg-[#f4efdf]">←</div>
                    <span className="text-[10px] font-black uppercase tracking-widest">VOLVER</span>
                </button>

                <div className="flex flex-col md:flex-row gap-16 items-start">
                    <div className="w-full md:w-[400px] shrink-0">
                        <div className="aspect-[2/3] rounded-[40px] overflow-hidden shadow-2xl border border-[#ece7da]">
                            <img src={actorDetails.photoUrl} alt={actorDetails.name} className="w-full h-full object-cover" />
                        </div>
                    </div>
                    <div className="flex-1">
                        <h2 className="text-6xl md:text-8xl font-black italic tracking-tighter uppercase mb-8">{actorDetails.name}</h2>
                        <div className="mb-12">
                            <p className="text-[10px] font-black text-tcd-orange uppercase tracking-[0.4em] mb-6">BIOGRAFÍA</p>
                            <p className="text-xl leading-relaxed text-[#2d2a26] font-medium italic">{actorDetails.bio}</p>
                        </div>
                    </div>
                </div>

                {/* Filmografía / Otros trabajos */}
                {actorDetails.movies && actorDetails.movies.length > 0 && (
                    <div className="mt-24">
                        <p className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-12 opacity-60">FILMOGRAFÍA DESTACADA / OTROS TRABAJOS</p>
                        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-x-8 gap-y-12">
                            {actorDetails.movies.map((movie, idx) => (
                                <div 
                                    key={idx} 
                                    className="group cursor-pointer"
                                    onClick={() => {
                                        setActorDetails(null);
                                        navigate(`/details/TMDb/${movie.type}/${movie.externalId}`);
                                    }}
                                >
                                    <div className="aspect-[2/3] rounded-[32px] overflow-hidden border border-[#ece7da] mb-4 bg-[#f4efdf] shadow-md group-hover:shadow-2xl transition-all duration-500 group-hover:-translate-y-2">
                                        <img src={movie.coverUrl} alt={movie.title} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
                                    </div>
                                    <p className="text-[11px] font-black uppercase text-[#2d2a26] leading-tight group-hover:text-tcd-orange transition-colors">{movie.title}</p>
                                    <p className="text-[9px] font-bold text-[#8c8471] uppercase tracking-widest mt-1">{movie.type}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        );
    }

    const t = type.toUpperCase();

    return (
        <div className="animate-fade-in pb-20 pt-10 px-4 md:px-0">
            <button 
                onClick={() => navigate(-1)}
                className="mb-12 flex items-center gap-4 text-[#8c8471] hover:text-black transition-colors group"
            >
                <div className="w-12 h-12 rounded-full border border-[#ece7da] flex items-center justify-center group-hover:bg-[#f4efdf] transition-all">
                    <span className="text-xl">←</span>
                </div>
                <span className="text-[11px] font-black uppercase tracking-[0.3em]">VOLVER</span>
            </button>

            <div className="flex flex-col lg:flex-row gap-16 lg:items-start">
                {/* Lateral Izquierdo: Portada */}
                <div className="w-full lg:w-[400px] flex-shrink-0">
                    <div className="lg:sticky lg:top-24">
                        <div className={`${t === 'DISCO' ? 'aspect-square' : 'aspect-[2/3]'} rounded-[48px] overflow-hidden shadow-2xl border border-[#ece7da] bg-[#f4efdf] mb-8 group/poster relative`}>
                            <img 
                                src={itemDetails.coverUrl} 
                                alt={itemDetails.title} 
                                className="w-full h-full object-cover group-hover/poster:scale-105 transition-transform duration-700"
                            />
                            <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent pointer-events-none"></div>
                        </div>
                        
                        <div className="flex gap-4 mb-8">
                            <button 
                                onClick={() => setIsModalOpen(true)}
                                disabled={itemDetails.inLibrary}
                                className={`flex-1 py-6 rounded-[24px] font-black text-[11px] uppercase tracking-[0.3em] transition-all shadow-xl flex items-center justify-center gap-3 ${itemDetails.inLibrary ? 'bg-[#ece7da] text-[#8c8471] cursor-default' : 'bg-[#2d2a26] text-white hover:bg-tcd-orange'}`}
                            >
                                <span>{itemDetails.inLibrary ? '✓ EN TU BIBLIOTECA' : '+ AÑADIR A LA BIBLIOTECA'}</span>
                            </button>
                            
                            {itemDetails.inLibrary && (
                                <HeartBtn 
                                    itemId={itemDetails.libraryId} 
                                    initialFavorite={itemDetails.favorite} 
                                    onUpdate={(val) => setItemDetails(prev => ({ ...prev, favorite: val }))}
                                />
                            )}
                        </div>

                        {/* AÑADIR A LISTAS */}
                        <div className="mb-8 relative">
                            <button
                                onClick={() => setShowListDropdown(v => !v)}
                                className="w-full py-5 rounded-[24px] font-black text-[11px] uppercase tracking-[0.3em] transition-all shadow-md bg-white border border-[#ece7da] text-[#2d2a26] hover:bg-[#f4efdf] flex items-center justify-center gap-3"
                            >
                                <span>📋 Añadir a una lista</span>
                                <span className="text-xs">▾</span>
                            </button>
                            {showListDropdown && (
                                <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-[#ece7da] rounded-[24px] shadow-2xl z-50 overflow-hidden max-h-60 overflow-y-auto p-2 space-y-1">
                                    {lists.length === 0 ? (
                                        <div className="text-[10px] font-bold text-[#8c8471] p-4 text-center">
                                            No tienes listas creadas. Créalas en tu perfil.
                                        </div>
                                    ) : (
                                        lists.map(list => {
                                            const isAlreadyInList = list.items && list.items.some(item => item.externalId === id);
                                            return (
                                                <button
                                                    key={list.id}
                                                    onClick={() => handleToggleList(list.id, isAlreadyInList)}
                                                    disabled={addingToList === list.id}
                                                    className="w-full text-left px-4 py-3 text-[11px] font-bold hover:bg-[#f4efdf] text-[#2d2a26] transition-colors rounded-xl flex items-center justify-between"
                                                >
                                                    <div className="flex items-center gap-3">
                                                        {list.coverUrl ? (
                                                            <img src={list.coverUrl} alt={list.name} className="w-6 h-6 rounded-lg object-cover shrink-0" />
                                                        ) : (
                                                            <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-[#7c3aed] to-[#a855f7] flex items-center justify-center text-white text-[9px] font-black shrink-0">
                                                                {list.name.charAt(0).toUpperCase()}
                                                            </div>
                                                        )}
                                                        <span>{list.name}</span>
                                                    </div>
                                                    <span className="text-[10px] font-bold text-[#8c8471]">
                                                        {addingToList === list.id ? '...' : isAlreadyInList ? '✓ Quitar' : '+ Añadir'}
                                                    </span>
                                                </button>
                                            );
                                        })
                                    )}
                                </div>
                            )}
                        </div>

                        <div className="bg-[#fcfaf5] p-8 rounded-[32px] border border-[#ece7da]">
                            <p className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-4 opacity-60">ID EXTERNO</p>
                            <div className="flex items-center gap-3">
                                <div className="w-3 h-3 rounded-full bg-tcd-orange animate-pulse"></div>
                                <span className="text-[11px] font-black uppercase tracking-widest text-[#2d2a26]">{source}: {id}</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Contenido Derecho: Información */}
                <div className="flex-1 min-w-0 pt-4">
                    <div className="flex items-center gap-4 mb-6">
                        <span className="bg-tcd-orange text-white text-[10px] font-black uppercase tracking-[0.2em] px-4 py-1.5 rounded-lg border border-tcd-orange shadow-lg">
                            {t}
                        </span>
                    </div>

                    <h2 className="text-5xl md:text-8xl font-black text-[#2d2a26] uppercase italic tracking-tighter mb-12 leading-[0.8] break-words">
                        {itemDetails.title}
                    </h2>

                    {itemDetails.tagline && (
                        <p className="text-2xl font-bold italic text-[#8c8471] mb-12 border-l-4 border-tcd-orange pl-8">
                            "{itemDetails.tagline}"
                        </p>
                    )}


                    <div className="grid grid-cols-2 lg:grid-cols-4 gap-12 mb-16 py-12 border-y border-[#ece7da]">
                        {itemDetails.releaseDate && (
                            <div>
                                <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-3 opacity-50">LANZAMIENTO</p>
                                <p className="text-xl font-black text-[#2d2a26] tracking-widest">
                                    {new Date(itemDetails.releaseDate).getFullYear()}
                                </p>
                            </div>
                        )}
                        {(itemDetails.directorName || itemDetails.author || itemDetails.artist) && (
                            <div>
                                <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-3 opacity-50">AUTOR / DIR</p>
                                <p className="text-xl font-black text-[#2d2a26] tracking-tight uppercase italic hover:text-tcd-orange cursor-pointer transition-colors">
                                    {itemDetails.directorName || itemDetails.author || itemDetails.artist}
                                </p>
                            </div>
                        )}
                        {itemDetails.totalPages && (
                            <div>
                                <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-3 opacity-50">EXTENSIÓN</p>
                                <p className="text-xl font-black text-[#2d2a26] tracking-widest">{itemDetails.totalPages} PÁG.</p>
                            </div>
                        )}
                        {itemDetails.totalTracks && (
                            <div>
                                <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-3 opacity-50">COMPOSICIÓN</p>
                                <p className="text-xl font-black text-[#2d2a26] tracking-widest">{itemDetails.totalTracks} TRACKS</p>
                            </div>
                        )}
                        {itemDetails.genre && (
                            <div>
                                <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-3 opacity-50">GÉNERO</p>
                                <p className="text-xl font-black text-[#2d2a26] tracking-tight truncate uppercase italic">{itemDetails.genre}</p>
                            </div>
                        )}
                    </div>

                    <div className="mb-20">
                        <p className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-8 opacity-60 italic border-l-2 border-tcd-orange pl-4">SINOPSIS Y ARCHIVO</p>
                        <p className="text-xl md:text-2xl leading-relaxed text-[#2d2a26]/90 font-medium italic">
                            {itemDetails.description || "No hay información detallada disponible para esta obra en tu archivo local."}
                        </p>
                    </div>

                    {/* REPARTO (CAST) */}
                    {itemDetails.actors && itemDetails.actors.length > 0 && (
                        <div className="mb-20">
                            <p className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-10 opacity-60">REPARTO PRINCIPAL</p>
                            <div className="flex overflow-x-auto gap-8 pb-8 no-scrollbar">
                                {itemDetails.actors.map((actor, idx) => (
                                    <div 
                                        key={idx} 
                                        className="w-[140px] shrink-0 group cursor-pointer"
                                        onClick={() => fetchActorDetails(actor.name)}
                                    >
                                        <div className="aspect-[1/1] rounded-[24px] overflow-hidden border border-[#ece7da] mb-4 bg-[#f4efdf]">
                                            <img src={actor.photoUrl || 'https://via.placeholder.com/200x200?text=Actor'} alt={actor.name} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" />
                                        </div>
                                        <p className="text-[11px] font-black uppercase text-[#2d2a26] leading-tight group-hover:text-tcd-orange transition-colors">{actor.name}</p>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* LISTA DE CANCIONES */}
                    {itemDetails.tracks && itemDetails.tracks.length > 0 && (
                        <div className="mb-20 bg-[#fcfaf5] p-12 rounded-[40px] border border-[#ece7da] shadow-sm">
                            <p className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-10 opacity-60">LISTA DE CANCIONES</p>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-16 gap-y-4">
                                {itemDetails.tracks.map((track, idx) => (
                                    <div key={idx} className="flex justify-between items-center py-5 border-b border-[#ece7da]/50 group hover:bg-white px-6 transition-all rounded-2xl">
                                        <span className="text-base font-bold text-[#2d2a26]">{track.number}. {track.title}</span>
                                        <span className="text-[11px] font-bold text-[#8c8471] font-mono bg-[#f4efdf] px-2 py-0.5 rounded-md">{track.duration}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* RECOMENDACIONES */}
                    {itemDetails.recommendations && itemDetails.recommendations.length > 0 && (
                        <div className="mb-20">
                            <p className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.4em] mb-10 opacity-60">RECOMENDACIONES SIMILARES</p>
                            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
                                {itemDetails.recommendations.map((rec, idx) => (
                                    <div 
                                        key={idx} 
                                        className="group cursor-pointer"
                                        onClick={() => navigate(`/details/TMDb/${type.toLowerCase()}/${rec.externalId}`)}
                                    >
                                        <div className="aspect-[2/3] rounded-[24px] overflow-hidden border border-[#ece7da] mb-3 shadow-md group-hover:shadow-xl transition-all">
                                            <img src={rec.coverUrl} alt={rec.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                                        </div>
                                        <p className="text-[10px] font-black uppercase tracking-tight truncate text-[#2d2a26]">{rec.title}</p>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>
            <AddToLibraryModal 
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onConfirm={handleConfirmAdd}
                itemTitle={itemDetails.title}
                itemType={itemDetails.type || type.toUpperCase()}
            />
        </div>
    );
};

export default DetailsPage;
