import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AddToLibraryModal from '../components/AddToLibraryModal';

const API_BASE = 'http://localhost:8083';

const Explorar = ({ filterType }) => {
    const navigate = useNavigate();
    const [movies, setMovies] = useState([]);
    const [series, setSeries] = useState([]);
    const [albums, setAlbums] = useState([]);
    const [books, setBooks] = useState([]);
    const [searchResults, setSearchResults] = useState(null);
    const [query, setQuery] = useState('');
    const [searchType, setSearchType] = useState(filterType || 'PELICULA');
    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);

    const [selectedItem, setSelectedItem] = useState(null);
    const [itemDetails, setItemDetails] = useState(null);
    const [actorDetails, setActorDetails] = useState(null);
    const [loadingDetails, setLoadingDetails] = useState(false);
    const [relatedItems, setRelatedItems] = useState([]);

    const [genreData, setGenreData] = useState({});
    const [loadingGenres, setLoadingGenres] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeItemToAdd, setActiveItemToAdd] = useState(null);

    useEffect(() => {
        setSearchType(filterType || 'PELICULA');
        setSearchResults(null);
        setQuery('');
    }, [filterType]);

    useEffect(() => {
        const fetchGenres = async () => {
            if (!filterType) {
                setGenreData({});
                return;
            }
            setLoadingGenres(true);
            try {
                if (filterType === 'PELICULA') {
                    const [accion, comedia, drama, terror] = await Promise.all([
                        fetch(`${API_BASE}/api/external/movies/genre?genre=accion`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/movies/genre?genre=comedia`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/movies/genre?genre=drama`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/movies/genre?genre=terror`, { credentials: 'include' }).then(r => r.json())
                    ]);
                    setGenreData({
                        "Acción": accion,
                        "Comedia": comedia,
                        "Drama": drama,
                        "Terror / Suspenso": terror
                    });
                } else if (filterType === 'SERIE') {
                    const [drama, comedia, misterio, scifi] = await Promise.all([
                        fetch(`${API_BASE}/api/external/series/genre?genre=drama`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/series/genre?genre=comedia`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/series/genre?genre=misterio`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/series/genre?genre=scifi`, { credentials: 'include' }).then(r => r.json())
                    ]);
                    setGenreData({
                        "Drama": drama,
                        "Comedia": comedia,
                        "Misterio / Suspenso": misterio,
                        "Ciencia Ficción & Fantasía": scifi
                    });
                } else if (filterType === 'DISCO') {
                    const [pop, rock, electronic, indie] = await Promise.all([
                        fetch(`${API_BASE}/api/external/discs/genre?genre=pop`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/discs/genre?genre=rock`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/discs/genre?genre=electronic`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/discs/genre?genre=indie`, { credentials: 'include' }).then(r => r.json())
                    ]);
                    setGenreData({
                        "Pop": pop,
                        "Rock": rock,
                        "Electrónica": electronic,
                        "Indie / Alternativo": indie
                    });
                } else if (filterType === 'LIBRO') {
                    const [fiction, fantasy, history, thriller] = await Promise.all([
                        fetch(`${API_BASE}/api/external/books/genre?genre=fiction`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/books/genre?genre=fantasy`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/books/genre?genre=history`, { credentials: 'include' }).then(r => r.json()),
                        fetch(`${API_BASE}/api/external/books/genre?genre=thriller`, { credentials: 'include' }).then(r => r.json())
                    ]);
                    setGenreData({
                        "Ficción / Novela": fiction,
                        "Fantasía / Ciencia Ficción": fantasy,
                        "Historia / Biografías": history,
                        "Thriller / Misterio": thriller
                    });
                }
            } catch (err) {
                console.error("Error fetching genre data:", err);
            } finally {
                setLoadingGenres(false);
            }
        };
        fetchGenres();
    }, [filterType]);


    // Efecto para buscar contenido relacionado (Más de...)
    useEffect(() => {
        if (selectedItem && itemDetails && (itemDetails.author || itemDetails.artist)) {
            const name = itemDetails.author || itemDetails.artist;
            const type = selectedItem.type === 'LIBRO' ? 'LIBRO' : 'DISCO';
            
            fetch(`${API_BASE}/api/external/search?query=${encodeURIComponent(name)}&type=${type}`, { credentials: 'include' })
                .then(res => res.json())
                .then(data => {
                    if (Array.isArray(data)) {
                        setRelatedItems(data.filter(it => it.externalId !== selectedItem.externalId).slice(0, 8));
                    }
                })
                .catch(err => console.error("Error fetching related:", err));
        } else {
            setRelatedItems([]);
        }
    }, [selectedItem, itemDetails]);

    // Asegurar que el scroll siempre esté activo en esta página
    useEffect(() => {
        document.body.style.overflow = 'unset';
        return () => { document.body.style.overflow = 'unset'; };
    }, [selectedItem, actorDetails]);

    useEffect(() => {
        if (itemDetails || actorDetails) {
            window.scrollTo({ top: 0, behavior: 'instant' });
        }
    }, [itemDetails, actorDetails]);

    useEffect(() => {
        const fetchDetails = async () => {
            if (!selectedItem) {
                setItemDetails(null);
                return;
            }
            // Salto al inicio inmediato antes de cargar
            window.scrollTo({ top: 0, behavior: 'instant' });
            
            setLoadingDetails(true);
            try {
                const url = `${API_BASE}/api/external/details?source=${selectedItem.source || 'TMDb'}&id=${selectedItem.externalId}&type=${selectedItem.type}`;
                console.log("[DEBUG] Fetching details from:", url);
                const res = await fetch(url, { credentials: 'include' });
                if (res.ok) {
                    const data = await res.json();
                    console.log("[DEBUG] Details received:", data);
                    setItemDetails(data);
                } else {
                    console.error("[ERROR] Failed to fetch details:", res.status, res.statusText);
                    // Usar datos básicos si falla la API de detalles
                    setItemDetails({
                        title: selectedItem.title,
                        description: selectedItem.description,
                        coverUrl: selectedItem.coverUrl,
                        type: selectedItem.type
                    });
                }
            } catch (err) {
                console.error("[ERROR] Network error fetching details:", err);
                setItemDetails({
                    title: selectedItem.title,
                    description: selectedItem.description,
                    coverUrl: selectedItem.coverUrl,
                    type: selectedItem.type
                });
            } finally {
                setLoadingDetails(false);
            }
        };
        fetchDetails();
    }, [selectedItem]);

    const fetchActorDetails = async (name) => {
        console.log("Iniciando búsqueda de actor:", name);
        // Llevar al usuario arriba antes de cambiar la vista
        window.scrollTo({ top: 0, behavior: 'instant' });
        try {
            const res = await fetch(`${API_BASE}/api/external/actor?name=${encodeURIComponent(name)}`, { credentials: 'include' });
            if (res.ok) {
                const data = await res.json();
                console.log("Datos actor recibidos:", data);
                setActorDetails(data);
            } else {
                console.error("Error en respuesta del servidor al buscar actor:", res.status);
            }
        } catch (err) {
            console.error("Error de red al buscar actor:", err);
        }
    };

    useEffect(() => {
        const fetchData = async () => {
            try {
                // FETCH TRENDING DATA FROM BACKEND
                const endpoints = [
                    `${API_BASE}/api/external/trending/movies`,
                    `${API_BASE}/api/external/trending/series`,
                    `${API_BASE}/api/external/trending/discs`,
                    `${API_BASE}/api/external/trending/books`
                ];

                const [moviesRes, seriesRes, albumsRes, booksRes] = await Promise.all(
                    endpoints.map(url => fetch(url, { credentials: 'include' }))
                );

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

                if (booksRes.ok) {
                    const booksData = await booksRes.json();
                    setBooks(booksData || []);
                }
            } catch (err) {
                console.error("Error fetching trending:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const handleSearch = async (e, forcedQuery = null, forcedType = null) => {
        if (e) e.preventDefault();
        const q = forcedQuery || query;
        const t = forcedType || searchType;
        if (!q.trim()) return;
        setSearching(true);
        setSelectedItem(null); // Cerrar detalles si estaban abiertos
        try {
            const res = await fetch(`${API_BASE}/api/external/search?query=${encodeURIComponent(q)}&type=${t.toUpperCase()}`, { credentials: 'include' });
            if (res.ok) {
                const data = await res.json();
                setSearchResults(data);
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        } catch (err) {
            console.error("Error searching:", err);
        } finally {
            setSearching(false);
        }
    };

    const handleConfirmAdd = async ({ status, completionDate, rating, favorite }) => {
        if (!activeItemToAdd) return;
        setIsModalOpen(false);
        try {
            const res = await fetch(`${API_BASE}/api/content/add`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    externalId: String(activeItemToAdd.externalId || '').trim(),
                    type: activeItemToAdd.type,
                    title: activeItemToAdd.title,
                    imageUrl: activeItemToAdd.coverUrl,
                    status,
                    completionDate,
                    rating,
                    favorite
                })
            });
            if (res.ok) {
                alert(`¡${activeItemToAdd.title} añadido a tu biblioteca!`);
            } else {
                const msg = await res.text();
                alert(msg);
            }
        } catch (err) {
            console.error("Error adding to library:", err);
        } finally {
            setActiveItemToAdd(null);
        }
    };

    const renderGrid = (items, sectionTitle, isTrending = false) => {
        // Asegurar simetria en el grid (2, 3, 4, 6 columnas)
        // 24 es ideal (divisible por 2, 3, 4, 6)
        let displayItems = items || [];
        if (displayItems.length >= 24) displayItems = displayItems.slice(0, 24);
        else if (displayItems.length >= 18) displayItems = displayItems.slice(0, 18);
        else if (displayItems.length >= 12) displayItems = displayItems.slice(0, 12);
        else if (displayItems.length >= 6) displayItems = displayItems.slice(0, 6);

        return (
            <div className="mb-24 px-4 md:px-8">
                <h3 className="text-sm font-black text-[#2d2a26] uppercase tracking-[0.4em] mb-12 flex items-center gap-4">
                    <span className="w-8 h-[2px] bg-tcd-orange"></span>
                    {sectionTitle}
                </h3>
                <div className="explore-grid">
                    {displayItems.length > 0 ? (
                        displayItems.map((item, i) => (
                            <div key={item.externalId || i} className="group relative flex flex-col explore-item">
                                <div 
                                    onClick={() => {
                                    const source = item.type === 'DISCO' ? 'Spotify' : (item.type === 'LIBRO' ? 'GoogleBooks' : 'TMDb');
                                    navigate(`/details/${source}/${item.type.toLowerCase()}/${item.externalId}`);
                                }}
                                className={`${item.type === 'DISCO' ? 'aspect-square' : 'aspect-[2/3]'} w-full bg-[#f4efdf] rounded-[24px] overflow-hidden shadow-sm border border-[#ece7da] transition-all duration-500 hover:shadow-[0_20px_50px_-12px_rgba(45,42,38,0.25)] hover:-translate-y-2 cursor-pointer relative`}
                            >
                                {item.coverUrl ? (
                                    <img 
                                        src={item.coverUrl} 
                                        alt={item.title} 
                                        className="w-full h-full object-cover transition-transform duration-[1.2s] group-hover:scale-105"
                                        onError={(e) => e.target.src = 'https://via.placeholder.com/300x450?text=No+Cover'}
                                    />
                                ) : (
                                    <div className="w-full h-full flex items-center justify-center p-6 text-center">
                                        <span className="text-[10px] font-black opacity-30 uppercase tracking-widest leading-tight text-[#2d2a26]">{item.title}</span>
                                    </div>
                                )}
                                
                                {isTrending && (
                                    <div className="absolute top-4 left-4 bg-tcd-orange text-white text-[8px] font-black px-3 py-1 rounded-full tracking-widest shadow-lg">
                                        TENDENCIA
                                    </div>
                                )}

                                <div className="absolute inset-0 bg-[#2d2a26]/40 backdrop-blur-[2px] flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-500">
                                    <div className="flex flex-col gap-2 scale-90 group-hover:scale-100 transition-transform duration-500">
                                        <button 
                                            onClick={(e) => { 
                                                e.stopPropagation(); 
                                                setActiveItemToAdd(item);
                                                setIsModalOpen(true);
                                            }}
                                            className="bg-white text-black px-6 py-2.5 rounded-full text-[9px] font-black uppercase tracking-widest hover:bg-tcd-orange hover:text-white transition-all shadow-xl"
                                        >
                                            + ARCHIVAR
                                        </button>
                                        <button 
                                            onClick={(e) => { 
                                                e.stopPropagation(); 
                                                const source = item.type === 'DISCO' ? 'Spotify' : (item.type === 'LIBRO' ? 'GoogleBooks' : 'TMDb');
                                                navigate(`/details/${source}/${item.type.toLowerCase()}/${item.externalId}`);
                                            }}
                                            className="bg-transparent border border-white text-white px-6 py-2.5 rounded-full text-[9px] font-black uppercase tracking-widest hover:bg-white hover:text-black transition-all"
                                        >
                                            DETALLES
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <div className="mt-4 px-1 flex flex-col h-14">
                                <p className="text-[14px] font-bold text-[#2d2a26] uppercase line-clamp-2 leading-tight tracking-tight group-hover:text-tcd-orange transition-colors duration-300">
                                    {item.title}
                                </p>
                                <div className="mt-auto flex items-center gap-2 opacity-60">
                                    <span className="text-[9px] text-[#b8601a] font-black uppercase tracking-widest">{item.type}</span>
                                    {item.year && (
                                        <>
                                            <span className="w-1 h-1 bg-[#8c8471] rounded-full"></span>
                                            <span className="text-[9px] text-[#8c8471] font-bold">{item.year}</span>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="col-span-full py-20 text-center border-2 border-dashed border-[#ece7da] rounded-[40px]">
                        <p className="text-[11px] font-black text-[#8c8471] uppercase tracking-widest">No se encontraron resultados en esta categoría</p>
                    </div>
                )}
            </div>
        </div>
    );
};

    return (
        <div className="animate-fade-in max-w-[1400px] mx-auto pb-40 px-4 md:px-8">
            {/* Search Header */}
            <div className="pt-10 pb-20 border-b border-[#ece7da] mb-16 text-center md:text-left">
                <span className="text-tcd-orange text-xs font-black uppercase tracking-[0.6em] mb-4 block">
                    {filterType ? `EXPLORACIÓN DE ${filterType}` : "EXPLORACIÓN GLOBAL"}
                </span>
                <div className="flex flex-col md:flex-row justify-between items-center gap-12">
                        <h2 className="text-[70px] md:text-[85px] font-black tracking-tighter text-[#2d2a26] leading-[0.8] uppercase italic">
                        {filterType ? (
                            filterType === 'PELICULA' ? 'Cine' :
                            filterType === 'SERIE' ? 'TV' :
                            filterType === 'LIBRO' ? 'Libros' : 'Música'
                        ) : 'Descubre'}<span className="text-tcd-orange">.</span>
                        </h2>

                        <form onSubmit={handleSearch} className="flex-1 max-w-2xl w-full">
                        <div className="relative group">
                            <input 
                                type="text"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                placeholder={
                                    searchType === 'PELICULA' ? 'BUSCAR CINE / PELÍCULAS...' :
                                    searchType === 'SERIE' ? 'BUSCAR SERIES DE TELEVISIÓN...' :
                                    searchType === 'LIBRO' ? 'BUSCAR LIBROS Y LITERATURA...' :
                                    searchType === 'DISCO' ? 'BUSCAR MÚSICA Y DISCOS...' :
                                    'BUSCAR EN EL ARCHIVO CULTURAL...'
                                }
                                className="w-full bg-white border-2 border-[#ece7da] px-10 py-8 rounded-full text-sm font-bold tracking-tight focus:outline-none focus:border-tcd-orange shadow-xl transition-all group-hover:shadow-2xl"
                            />
                            <button 
                                type="submit"
                                className="absolute right-4 top-4 bg-[#b8601a] text-white px-10 py-4 rounded-full text-[11px] font-black uppercase tracking-widest hover:bg-[#a05015] transition-all shadow-lg active:scale-95"
                            >
                                {searching ? '...' : 'BUSCAR'}
                            </button>
                        </div>
                        {!filterType && (
                            <div className="flex gap-4 mt-6 justify-center md:justify-start pl-6">
                                {['PELICULA', 'SERIE', 'LIBRO', 'DISCO'].map((t) => (
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
                        )}
                        </form>
                </div>
            </div>

            {loading || loadingGenres ? (
                <div className="grid grid-cols-2 md:grid-cols-6 gap-8">
                    {[1,2,3,4,5,6].map(i => (
                        <div key={i} className={`${searchType === 'DISCO' ? 'aspect-square' : 'aspect-[2/3]'} bg-[#f4efdf] rounded-[32px] animate-pulse`}></div>
                    ))}
                </div>
            ) : searchResults ? (
                renderGrid(searchResults, `RESULTADOS DE "${query.toUpperCase()}"`)
            ) : (
                <>
                    {/* Solo mostrar tendencias en la vista global */}
                    {!filterType && (
                        <>
                            {movies.length > 0 && renderGrid(movies, "CINE EN TENDENCIA", true)}
                            {series.length > 0 && renderGrid(series, "SERIES ACLAMADAS", true)}
                            {albums.length > 0 && renderGrid(albums, "DISCOS DEL MOMENTO", true)}
                            {books.length > 0 && renderGrid(books, "LITERATURA DESTACADA", true)}
                        </>
                    )}

                    {/* Mostrar géneros cuando hay un filtro activo */}
                    {filterType && Object.entries(genreData).map(([genreName, items]) => 
                        items && items.length > 0 && (
                            <React.Fragment key={genreName}>
                                {renderGrid(items, genreName.toUpperCase())}
                            </React.Fragment>
                        )
                    )}
                </>
            )}

            {activeItemToAdd && (
                <AddToLibraryModal 
                    isOpen={isModalOpen}
                    onClose={() => { setIsModalOpen(false); setActiveItemToAdd(null); }}
                    onConfirm={handleConfirmAdd}
                    itemTitle={activeItemToAdd.title}
                    itemType={activeItemToAdd.type}
                />
            )}
        </div>
    );
};

export default Explorar;
