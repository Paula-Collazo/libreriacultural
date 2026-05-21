import React, { useState, useEffect } from 'react';

const Explorar = () => {
    const [movies, setMovies] = useState([]);
    const [series, setSeries] = useState([]);
    const [albums, setAlbums] = useState([]);
    const [searchResults, setSearchResults] = useState(null);
    const [query, setQuery] = useState('');
    const [searchType, setSearchType] = useState('PELICULA');
    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);

    const [selectedItem, setSelectedItem] = useState(null);
    const [itemDetails, setItemDetails] = useState(null);
    const [actorDetails, setActorDetails] = useState(null);
    const [loadingDetails, setLoadingDetails] = useState(false);

    useEffect(() => {
        const fetchDetails = async () => {
            if (!selectedItem) {
                setItemDetails(null);
                return;
            }
            setLoadingDetails(true);
            try {
                const res = await fetch(`http://127.0.0.1:8083/api/external/details?source=${selectedItem.source || 'TMDb'}&id=${selectedItem.externalId}&type=${selectedItem.type}`);
                if (res.ok) {
                    const data = await res.json();
                    setItemDetails(data);
                }
            } catch (err) {
                console.error("Error fetching details:", err);
            } finally {
                setLoadingDetails(false);
            }
        };
        fetchDetails();
    }, [selectedItem]);

    const fetchActorDetails = async (name) => {
        try {
            const res = await fetch(`http://127.0.0.1:8083/api/external/actor?name=${encodeURIComponent(name)}`);
            if (res.ok) {
                const data = await res.json();
                setActorDetails(data);
            }
        } catch (err) {
            console.error("Error fetching actor:", err);
        }
    };

    useEffect(() => {
        const fetchData = async () => {
            try {
                // FETCH TRENDING DATA FROM BACKEND
                const [moviesRes, seriesRes, albumsRes] = await Promise.all([
                    fetch('http://127.0.0.1:8083/api/external/trending/movies'),
                    fetch('http://127.0.0.1:8083/api/external/trending/series'),
                    fetch('http://127.0.0.1:8083/api/external/trending/discs')
                ]);
                
                if (moviesRes.ok) {
                    const moviesData = await moviesRes.json();
                    setMovies(moviesData || []);
                }
                
                if (seriesRes.ok) {
                    const seriesData = await seriesRes.json();
                    setSeries(seriesData || []);
                }

                if (albumsRes.ok) {
                    const albumsData = await albumsRes.json();
                    setAlbums(albumsData || []);
                }
            } catch (err) {
                console.error("Error fetching trending:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const handleSearch = async (e) => {
        e.preventDefault();
        if (!query.trim()) return;
        setSearching(true);
        try {
            const res = await fetch(`http://127.0.0.1:8083/api/external/search?query=${encodeURIComponent(query)}&type=${searchType}`);
            if (res.ok) {
                const data = await res.json();
                setSearchResults(data);
            }
        } catch (err) {
            console.error("Error searching:", err);
        } finally {
            setSearching(false);
        }
    };

    const addToLibrary = async (item) => {
        try {
            const res = await fetch('http://127.0.0.1:8083/api/content/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    externalId: item.externalId,
                    type: item.type,
                    title: item.title,
                    imageUrl: item.coverUrl
                })
            });
            if (res.ok) {
                alert(`¡${item.title} añadido a tu biblioteca!`);
            } else {
                const msg = await res.text();
                alert(msg);
            }
        } catch (err) {
            console.error("Error adding to library:", err);
        }
    };

    const renderGrid = (items, sectionTitle) => (
        <div className="mb-24 px-4 md:px-0">
            <h3 className="text-xs font-black text-[#8c8471] uppercase tracking-[0.4em] mb-10 pl-2">
                {sectionTitle} <span className="text-tcd-orange ml-4">•</span>
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-x-8 gap-y-16">
                {items && items.length > 0 ? items.map((item, i) => (
                    <div key={item.externalId || i} className="group relative">
                        <div 
                            onClick={() => setSelectedItem(item)}
                            className="aspect-[2/3] bg-white rounded-[32px] overflow-hidden shadow-sm border border-[#ece7da] transition-all duration-700 hover:shadow-[0_20px_60px_-15px_rgba(45,42,38,0.2)] hover:-translate-y-4 cursor-pointer"
                        >
                            {item.coverUrl ? (
                                <img 
                                    src={item.coverUrl} 
                                    alt={item.title} 
                                    className="w-full h-full object-cover transition-transform duration-[1.5s] group-hover:scale-110"
                                    onError={(e) => e.target.src = 'https://via.placeholder.com/300x450?text=No+Cover'}
                                />
                            ) : (
                                <div className="w-full h-full flex items-center justify-center p-8 text-center bg-[#f4efdf]">
                                    <span className="text-[11px] font-black opacity-30 uppercase tracking-[0.2em] leading-tight text-[#2d2a26]">{item.title}</span>
                                </div>
                            )}
                            <div className="absolute inset-0 bg-[#2d2a26]/60 backdrop-blur-sm flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-500">
                                <div className="flex flex-col gap-3 scale-90 group-hover:scale-100 transition-transform duration-500">
                                    <button 
                                        onClick={(e) => { e.stopPropagation(); addToLibrary(item); }}
                                        className="bg-white text-black px-8 py-3 rounded-full text-[10px] font-black uppercase tracking-widest hover:bg-tcd-orange hover:text-white transition-all shadow-2xl"
                                    >
                                        + ARCHIVAR
                                    </button>
                                    <button 
                                        onClick={(e) => { e.stopPropagation(); setSelectedItem(item); }}
                                        className="bg-transparent border border-white text-white px-8 py-3 rounded-full text-[10px] font-black uppercase tracking-widest hover:bg-white hover:text-black transition-all"
                                    >
                                        VER DETALLES
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div className="mt-8 px-2">
                            <p className="text-[16px] font-black text-[#2d2a26] uppercase truncate tracking-tighter italic mb-1 group-hover:text-tcd-orange transition-colors duration-300">
                                {item.title}
                            </p>
                            <div className="flex items-center gap-2">
                                <span className="text-[9px] text-[#b8601a] font-black uppercase tracking-widest">{item.type}</span>
                                <span className="w-1 h-1 bg-[#8c8471]/40 rounded-full"></span>
                                <span className="text-[9px] text-[#8c8471] uppercase font-bold tracking-widest">TENDENCIA</span>
                            </div>
                        </div>
                    </div>
                )) : (
                    <div className="col-span-full py-20 text-center border-2 border-dashed border-[#ece7da] rounded-[40px]">
                        <p className="text-[11px] font-black text-[#8c8471] uppercase tracking-widest">No se encontraron resultados en esta categoría</p>
                    </div>
                )}
            </div>
        </div>
    );

    return (
        <div className="animate-fade-in max-w-[1500px] mx-auto pb-40">
            {/* Modal de Actor */}
            {actorDetails && (
                <div className="fixed inset-0 z-[110] flex items-center justify-center px-4">
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setActorDetails(null)}></div>
                    <div className="bg-[#f9f7f2] w-full max-w-2xl rounded-[40px] overflow-hidden shadow-2xl relative animate-in fade-in slide-in-from-bottom-8 duration-500 overflow-y-auto max-h-[85vh]">
                        <button onClick={() => setActorDetails(null)} className="absolute top-6 right-6 w-10 h-10 flex items-center justify-center rounded-full bg-white text-black font-bold shadow-md hover:scale-110 transition-transform z-10">×</button>
                        <div className="flex flex-col md:flex-row">
                            <div className="w-full md:w-1/3 aspect-[3/4]">
                                <img src={actorDetails.photoUrl} alt={actorDetails.name} className="w-full h-full object-cover" />
                            </div>
                            <div className="p-8 md:p-10 flex-1">
                                <h3 className="text-3xl font-black text-[#2d2a26] uppercase italic mb-6 leading-tight">{actorDetails.name}</h3>
                                <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.3em] mb-4">BIOGRAFÍA</p>
                                <p className="text-[13px] leading-relaxed text-[#2d2a26]/80 italic mb-8 max-h-[300px] overflow-y-auto pr-4">
                                    {actorDetails.biography || "No hay biografía disponible."}
                                </p>
                                {actorDetails.knownFor && (
                                    <div>
                                        <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.3em] mb-4">TRABAJOS DESTACADOS</p>
                                        <div className="flex flex-wrap gap-2 text-[10px] font-bold text-[#b8601a]">
                                            {actorDetails.knownFor.map((w, i) => <span key={i} className="bg-[#ece7da] px-3 py-1 rounded-full">{w}</span>)}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal de Detalles */}
            {selectedItem && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center px-4 py-10">
                    <div className="absolute inset-0 bg-[#2d2a26]/90 backdrop-blur-xl" onClick={() => setSelectedItem(null)}></div>
                    <div className="bg-[#f9f7f2] w-full max-w-6xl rounded-[48px] overflow-hidden shadow-2xl relative animate-in fade-in zoom-in duration-300 flex flex-col md:flex-row h-full max-h-[85vh]">
                        <button 
                            onClick={() => setSelectedItem(null)}
                            className="absolute top-8 right-8 z-10 w-12 h-12 flex items-center justify-center rounded-full bg-white text-black text-xl font-bold shadow-lg hover:scale-110 transition-transform"
                        >
                            ×
                        </button>
                        
                        <div className="w-full md:w-[35%] aspect-[2/3] md:h-auto overflow-hidden relative">
                            <img 
                                src={selectedItem.coverUrl} 
                                alt={selectedItem.title} 
                                className="w-full h-full object-cover"
                            />
                            {itemDetails?.tagline && (
                                <div className="absolute bottom-10 left-0 right-0 px-10">
                                    <p className="text-white text-lg font-bold italic tracking-tight drop-shadow-lg text-center leading-tight">
                                        "{itemDetails.tagline}"
                                    </p>
                                </div>
                            )}
                        </div>
                        
                        <div className="w-full md:w-[65%] p-10 md:p-14 overflow-y-auto custom-scrollbar">
                            <div className="flex items-center gap-4 mb-4">
                                <span className="text-tcd-orange text-[10px] font-black uppercase tracking-[0.4em]">
                                    {selectedItem.type}
                                </span>
                                {itemDetails?.seriesMetadata && (
                                    <span className="text-[#8c8471] text-[10px] font-bold uppercase tracking-widest bg-[#ece7da] px-3 py-1 rounded-full">
                                        {itemDetails.seriesMetadata}
                                    </span>
                                )}
                            </div>
                            
                            <h2 className="text-4xl md:text-6xl font-black text-[#2d2a26] uppercase italic tracking-tighter mb-8 leading-[0.85]">
                                {selectedItem.title}
                            </h2>
                            
                            <div className="flex flex-wrap gap-10 mb-10 border-b border-[#ece7da] pb-10">
                                {selectedItem.releaseDate && (
                                    <div>
                                        <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-widest mb-1 opacity-60">LANZAMIENTO</p>
                                        <p className="text-sm font-black text-[#2d2a26] tracking-widest">{new Date(selectedItem.releaseDate).getFullYear()}</p>
                                    </div>
                                )}
                                {itemDetails?.directorName && (
                                    <div>
                                        <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-widest mb-1 opacity-60">DIRECCIÓN</p>
                                        <p className="text-sm font-black text-[#2d2a26] tracking-tight uppercase italic hover:text-tcd-orange cursor-pointer transition-colors"
                                           onClick={() => fetchActorDetails(itemDetails.directorName)}>
                                            {itemDetails.directorName}
                                        </p>
                                    </div>
                                )}
                            </div>

                            <div className="mb-10">
                                <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.3em] mb-4 opacity-70 italic">SINOPSIS</p>
                                <p className="text-sm leading-relaxed text-[#2d2a26]/90 font-medium italic pr-4">
                                    {loadingDetails ? "Cargando descripción..." : (itemDetails?.description || selectedItem.description || "No hay descripción disponible para esta obra.")}
                                </p>
                            </div>

                            {itemDetails?.actors && (
                                <div className="mb-12">
                                    <div className="flex items-center justify-between mb-6">
                                        <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.3em] opacity-70 italic">REPARTO PRINCIPAL</p>
                                    </div>
                                    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
                                        {itemDetails.actors.map((actor, idx) => (
                                            <div key={idx} 
                                                 onClick={() => fetchActorDetails(actor.name)}
                                                 className="group/actor cursor-pointer">
                                                <div className="aspect-square rounded-full overflow-hidden mb-3 border-2 border-transparent group-hover/actor:border-tcd-orange transition-all shadow-md">
                                                    <img src={actor.photoUrl || 'https://via.placeholder.com/200?text=Actor'} 
                                                         alt={actor.name} 
                                                         className="w-full h-full object-cover group-hover/actor:scale-110 transition-transform duration-500" />
                                                </div>
                                                <p className="text-[10px] font-black text-center text-[#2d2a26] leading-tight uppercase group-hover/actor:text-tcd-orange transition-colors">{actor.name}</p>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {itemDetails?.recommendations && (
                                <div className="mb-12 pt-10 border-t border-[#ece7da]">
                                    <p className="text-[9px] font-black text-[#8c8471] uppercase tracking-[0.3em] mb-6 opacity-70 italic">OBRAS SIMILARES</p>
                                    <div className="grid grid-cols-3 gap-6">
                                        {itemDetails.recommendations.map((rec, idx) => (
                                            <div key={idx} 
                                                 onClick={() => setSelectedItem(rec)}
                                                 className="group/rec cursor-pointer">
                                                <div className="aspect-[2/3] rounded-2xl overflow-hidden mb-3 shadow-md">
                                                    <img src={rec.coverUrl} alt={rec.title} className="w-full h-full object-cover group-hover/rec:scale-110 transition-transform duration-500" />
                                                </div>
                                                <p className="text-[9px] font-black text-[#2d2a26] truncate uppercase tracking-tighter italic">{rec.title}</p>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            <div className="sticky bottom-0 bg-[#f9f7f2]/90 backdrop-blur-md pt-6 pb-2">
                                <button 
                                    onClick={() => { addToLibrary(selectedItem); setSelectedItem(null); }}
                                    className="w-full bg-[#2d2a26] text-white py-6 rounded-2xl text-[11px] font-black uppercase tracking-[0.4em] hover:bg-tcd-orange transition-all shadow-xl active:scale-[0.98]"
                                >
                                    + ARCHIVAR EN MI BIBLIOTECA
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Search Header */}
            <div className="pt-10 pb-24 border-b border-[#ece7da] mb-20 text-center md:text-left">
                <span className="text-tcd-orange text-xs font-black uppercase tracking-[0.6em] mb-6 block">EXPLORACIÓN GLOBAL</span>
                <div className="flex flex-col md:flex-row justify-between items-center gap-12">
                     <h2 className="text-[90px] md:text-[110px] font-black tracking-tighter text-[#2d2a26] leading-[0.8] uppercase italic">
                        Descubre<span className="text-tcd-orange">.</span>
                     </h2>

                     <form onSubmit={handleSearch} className="flex-1 max-w-2xl w-full">
                        <div className="relative group">
                            <input 
                                type="text"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                placeholder="BUSCAR CINE, SERIES O LITERATURA..."
                                className="w-full bg-white border-2 border-[#ece7da] px-10 py-8 rounded-full text-sm font-bold tracking-tight focus:outline-none focus:border-tcd-orange shadow-xl transition-all group-hover:shadow-2xl"
                            />
                            <button 
                                type="submit"
                                className="absolute right-4 top-4 bg-[#b8601a] text-white px-10 py-4 rounded-full text-[11px] font-black uppercase tracking-widest hover:bg-[#a05015] transition-all shadow-lg active:scale-95"
                            >
                                {searching ? '...' : 'BUSCAR'}
                            </button>
                        </div>
                        <div className="flex gap-4 mt-6 justify-center md:justify-start pl-6">
                            {['PELICULA', 'SERIE', 'LIBRO'].map((t) => (
                                <button 
                                    key={t}
                                    type="button"
                                    onClick={() => setSearchType(t)}
                                    className={`text-[9px] font-black uppercase tracking-widest px-6 py-2 rounded-full transition-all ${searchType === t ? 'bg-[#2d2a26] text-white' : 'bg-[#e6e2d8] text-[#8c8471] hover:text-[#2d2a26]'}`}
                                >
                                    {t}
                                </button>
                            ))}
                        </div>
                     </form>
                </div>
            </div>

            {loading ? (
                <div className="grid grid-cols-2 md:grid-cols-6 gap-8">
                    {[1,2,3,4,5,6].map(i => (
                        <div key={i} className="aspect-[2/3] bg-[#f4efdf] rounded-[32px] animate-pulse"></div>
                    ))}
                </div>
            ) : searchResults ? (
                renderGrid(searchResults, `RESULTADOS DE "${query.toUpperCase()}"`)
            ) : (
                <>
                    {movies.length > 0 && renderGrid(movies, "CINE EN TENDENCIA")}
                    {series.length > 0 && renderGrid(series, "SERIES ACLAMADAS")}
                    {albums.length > 0 && renderGrid(albums, "DISCOS DEL MOMENTO")}
                </>
            )}
        </div>
    );
};

export default Explorar;
