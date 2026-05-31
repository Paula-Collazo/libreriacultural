import React, { useState, useEffect } from 'react';

const ContentGrid = ({ title, type }) => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`http://localhost:8083/api/content/${type}`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        setItems(data);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
      });
  }, [type]);

  const toggleFavorite = (id) => {
    fetch(`http://localhost:8083/api/content/${id}/favorite`, { method: 'POST', credentials: 'include' })
        .then(res => res.json())
        .then(isFav => {
            setItems(items.map(i => i.id === id ? { ...i, favorite: isFav } : i));
        });
  };

  const updateProgress = (id, currentPage, totalPages) => {
    fetch(`http://localhost:8083/api/content/${id}/progress`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ currentPage, totalPages })
    }).then(() => {
        setItems(items.map(i => i.id === id ? { ...i, bookCurrentPage: currentPage, bookTotalPages: totalPages } : i));
    });
  };

  return (
    <div className="animate-fade-in px-4 md:px-10 pb-24">
      <div className="flex flex-col md:flex-row justify-between items-center md:items-end mb-10 pb-8 border-b border-[#ece7da] gap-6">
        <div className="text-center md:text-left">
          <span className="text-tcd-orange text-[9px] font-black uppercase tracking-[0.4em] mb-2 block">{type}</span>
          <h2 className="text-[40px] md:text-[50px] font-black tracking-tighter text-[#2d2a26] uppercase leading-[0.8]">
            {title}<span className="text-tcd-orange">.</span>
          </h2>
        </div>
        <div className="flex gap-3 mb-2">
            <button className="px-6 py-2 bg-white border border-[#ece7da] rounded-full text-[9px] font-black uppercase tracking-widest hover:border-tcd-orange transition-all">Recientes</button>
            <button className="px-6 py-2 bg-white border border-[#ece7da] rounded-full text-[9px] font-black uppercase tracking-widest hover:border-tcd-orange transition-all">A-Z</button>
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-6">
          {[1,2,3,4,5].map(i => <div key={i} className="aspect-[2/3] bg-white border border-[#ece7da] rounded-[24px] animate-pulse"></div>)}
        </div>
      ) : items.length > 0 ? (
        <div className="library-table-container">
            <table className="w-full">
                <thead>
                    <tr className="bg-[#fcf8f0] border-b border-[#ece7da]">
                        <th className="p-6 text-left text-[10px] font-extrabold uppercase tracking-widest text-[#8c8471]">CONTENIDO</th>
                        <th className="p-6 text-left text-[10px] font-extrabold uppercase tracking-widest text-[#8c8471]">PROGRESO / INFO</th>
                        <th className="p-6 text-left text-[10px] font-extrabold uppercase tracking-widest text-[#8c8471]">ESTADO</th>
                        <th className="p-6 text-right text-[10px] font-extrabold uppercase tracking-widest text-[#8c8471]">ACCIONES</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-[#ece7da]">
                    {items.map((uc) => (
                        <tr key={uc.id} className="hover:bg-[#fdfcf9] transition-colors group">
                            <td className="p-6">
                                <div className="flex items-center gap-6">
                                    <div className="relative w-16 h-24 bg-[#f4efdf] rounded-xl overflow-hidden shadow-md flex-shrink-0 group-hover:scale-105 transition-transform duration-700">
                                        {uc.content.coverUrl ? (
                                            <img src={uc.content.coverUrl} className="w-full h-full object-cover" alt="" />
                                        ) : (
                                            <div className="w-full h-full flex items-center justify-center bg-gray-100 italic opacity-20 text-[8px]">No Cover</div>
                                        )}
                                        <button 
                                            onClick={() => toggleFavorite(uc.id)}
                                            className={`absolute top-1 right-1 w-8 h-8 rounded-full flex items-center justify-center text-[8px] font-black transition-all ${
                                                uc.favorite ? 'bg-red-500 text-white shadow-lg' : 'bg-white/90 text-gray-400 hover:text-red-500'
                                            }`}
                                        >
                                            {uc.favorite ? 'FAV' : 'ADD'}
                                        </button>
                                    </div>
                                    <div>
                                        <div className="text-xl md:text-2xl font-black text-[#2d2a26] uppercase tracking-tighter italic mb-2 leading-none">
                                            {uc.content.title}
                                        </div>
                                        <div className="flex items-center gap-3">
                                            <span className="text-tcd-orange font-extrabold text-[8px] uppercase tracking-widest">ARCHIVADO</span>
                                            <span className="w-1 h-1 bg-[#8c8471]/40 rounded-full"></span>
                                            <span className="text-[8px] font-bold text-[#8c8471]/60 uppercase tracking-widest">UID: {uc.content.externalId}</span>
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td className="p-6">
                                {uc.content.type === 'LIBRO' && (
                                    <div className="flex flex-col gap-2">
                                        <div className="flex items-center gap-2">
                                            <input 
                                                type="number" 
                                                className="w-16 p-1 text-[10px] border border-[#ece7da] rounded"
                                                value={uc.bookCurrentPage || 0}
                                                onChange={(e) => updateProgress(uc.id, parseInt(e.target.value), uc.bookTotalPages)}
                                            />
                                            <span className="text-[10px] text-[#8c8471]">de {uc.bookTotalPages || '?'} págs</span>
                                        </div>
                                        <div className="w-full bg-[#f4efdf] h-1.5 rounded-full overflow-hidden">
                                            <div 
                                                className="bg-tcd-orange h-full transition-all duration-1000"
                                                style={{ width: `${(uc.bookCurrentPage / (uc.bookTotalPages || 1)) * 100}%` }}
                                            ></div>
                                        </div>
                                    </div>
                                )}
                                {uc.content.type === 'SERIE' && (
                                    <div className="text-[10px] font-bold text-[#8c8471] uppercase tracking-wider">
                                        {uc.seriesTotalEpisodes ? `${uc.seriesTotalEpisodes} Episodios` : 'Seguimiento activo'}
                                    </div>
                                )}
                                {uc.content.type === 'DISCO' && (
                                    <div className="text-[10px] font-bold text-[#8c8471] uppercase tracking-wider">
                                        {uc.albumTotalTracks ? `${uc.albumTotalTracks} Pistas` : 'Álbum musical'}
                                    </div>
                                )}
                                {uc.content.type === 'PELICULA' && (
                                    <div className="text-[10px] font-bold text-[#8c8471] uppercase tracking-wider">
                                        {uc.movieWatched ? 'VISTO' : 'PENDIENTE'}
                                    </div>
                                )}
                            </td>
                            <td className="p-6">
                                <button 
                                    onClick={() => updateStatus(uc.id, uc.status === 'COMPLETED' ? 'PLANNING' : 'COMPLETED')}
                                    className={`px-6 py-2 rounded-full text-[9px] font-black uppercase tracking-widest transition-all ${
                                        uc.status === 'COMPLETED' ? 'bg-[#e7f5e9] text-[#2d5a3a]' : 'bg-[#fff4e5] text-[#b8601a]'
                                    }`}
                                >
                                    {uc.status}
                                </button>
                            </td>
                            <td className="p-6 text-right">
                                <button 
                                    onClick={() => removeContent(uc.id)}
                                    className="text-[9px] font-black uppercase tracking-widest text-[#8c8471] hover:text-red-600 transition-colors opacity-30 hover:opacity-100 px-4 py-2 border border-[#ece7da] rounded-full"
                                >
                                    Eliminar
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
      ) : (
        <div className="py-40 text-center glass-card border-dashed flex flex-col items-center justify-center">
            <span className="text-4xl font-black text-[#2d2a26]/10 uppercase tracking-[0.4em] italic leading-none mb-8">Sin registros en esta sección</span>
            <button 
                onClick={() => window.location.href='/explorar'}
                className="bg-[#b8601a] text-white px-12 py-5 rounded-full font-black text-xs uppercase tracking-[0.2em] shadow-lg hover:scale-105 transition-transform"
            >
                Explorar Catálogo
            </button>
        </div>
      )}
    </div>
  );
};

export default ContentGrid;
