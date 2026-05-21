import React, { useState, useEffect } from 'react';

const Explorar = () => {
    const [movies, setMovies] = useState([]);
    const [series, setSeries] = useState([]);
    const [searchResults, setSearchResults] = useState(null);
    const [query, setQuery] = useState('');
    const [searchType, setSearchType] = useState('PELICULA');
    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);

    useEffect(() => {
        const fetchData = async () => {
            try {
                // FETCH TRENDING DATA FROM BACKEND
                const [moviesRes, seriesRes] = await Promise.all([
                    fetch('http://127.0.0.1:8083/api/external/trending/movies'),
                    fetch('http://127.0.0.1:8083/api/external/trending/series')
                ]);
                
                if (moviesRes.ok) {
                    const moviesData = await moviesRes.json();
                    setMovies(moviesData || []);
                }
                
                if (seriesRes.ok) {
                    const seriesData = await seriesRes.json();
                    setSeries(seriesData || []);
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
                    imageUrl: item.imageUrl
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
                        <div className="aspect-[2/3] bg-white rounded-[32px] overflow-hidden shadow-sm border border-[#ece7da] transition-all duration-700 hover:shadow-[0_20px_60px_-15px_rgba(45,42,38,0.2)] hover:-translate-y-4">
                            {item.imageUrl ? (
                                <img 
                                    src={item.imageUrl} 
                                    alt={item.title} 
                                    className="w-full h-full object-cover transition-transform duration-[1.5s] group-hover:scale-110"
                                    onError={(e) => e.target.src = 'https://via.placeholder.com/300x450?text=No+Cover'}
                                />
                            ) : (
                                <div className="w-full h-full flex items-center justify-center p-8 text-center bg-[#f4efdf]">
                                    <span className="text-[11px] font-black opacity-30 uppercase tracking-[0.2em] leading-tight text-[#2d2a26]">{item.title}</span>
                                </div>
                            )}
                            <div className="absolute inset-0 bg-[#2d2a26]/80 backdrop-blur-md flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-500">
                                <button 
                                    onClick={() => addToLibrary(item)}
                                    className="bg-white text-black px-8 py-4 rounded-full text-[10px] font-black uppercase tracking-widest hover:scale-110 active:scale-95 transition-all shadow-2xl"
                                >
                                    + ARCHIVAR
                                </button>
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
                    {renderGrid(movies, "CINE EN TENDENCIA")}
                    {renderGrid(series, "SERIES ACLAMADAS")}
                </>
            )}
        </div>
    );
};

export default Explorar;
