import React, { useState, useEffect } from 'react';

const ContentGrid = ({ title, type }) => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`http://127.0.0.1:8083/api/content/${type}`)
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

  const removeContent = (id) => {
    if (window.confirm('¿Eliminar de la biblioteca?')) {
        fetch(`http://127.0.0.1:8083/api/content/${id}/delete`, { method: 'POST' })
            .then(() => setItems(items.filter(i => i.id !== id)));
    }
  };

  const updateStatus = (id, newStatus) => {
    fetch(`http://127.0.0.1:8083/api/content/${id}/status`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus })
    }).then(() => {
        setItems(items.map(i => i.id === id ? { ...i, status: newStatus } : i));
    });
  };

  return (
    <div className="animate-fade-in px-10 pb-24">
      <div className="flex justify-between items-end mb-16 pb-12 border-b border-[#ece7da]">
        <div>
          <span className="text-tcd-orange text-xs font-black uppercase tracking-[0.4em] mb-4 block">{type}</span>
          <h2 className="text-[100px] font-black tracking-tighter text-[#2d2a26] uppercase leading-[0.8]">
            {title}<span className="text-tcd-orange">.</span>
          </h2>
        </div>
        <div className="flex gap-4 mb-4">
            <button className="px-8 py-3 bg-white border border-[#ece7da] rounded-full text-[10px] font-black uppercase tracking-widest hover:border-tcd-orange transition-all">Recientes</button>
            <button className="px-8 py-3 bg-white border border-[#ece7da] rounded-full text-[10px] font-black uppercase tracking-widest hover:border-tcd-orange transition-all">A-Z</button>
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-5 gap-8">
          {[1,2,3,4,5].map(i => <div key={i} className="aspect-[2/3] bg-white border border-[#ece7da] rounded-[30px] animate-pulse"></div>)}
        </div>
      ) : items.length > 0 ? (
        <div className="library-table-container">
            <table className="w-full">
                <thead>
                    <tr className="bg-[#fcf8f0] border-b border-[#ece7da]">
                        <th className="p-10 text-left text-[11px] font-extrabold uppercase tracking-widest text-[#8c8471]">CONTENIDO</th>
                        <th className="p-10 text-left text-[11px] font-extrabold uppercase tracking-widest text-[#8c8471]">ESTADO</th>
                        <th className="p-10 text-left text-[11px] font-extrabold uppercase tracking-widest text-[#8c8471]">VALORACIÓN</th>
                        <th className="p-10 text-right text-[11px] font-extrabold uppercase tracking-widest text-[#8c8471]">ACCIONES</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-[#ece7da]">
                    {items.map((uc) => (
                        <tr key={uc.id} className="hover:bg-[#fdfcf9] transition-colors group">
                            <td className="p-10">
                                <div className="flex items-center gap-10">
                                    <div className="w-24 h-36 bg-[#f4efdf] rounded-2xl overflow-hidden shadow-xl flex-shrink-0 group-hover:scale-105 transition-transform duration-700">
                                        {uc.content.coverUrl ? (
                                            <img src={uc.content.coverUrl} className="w-full h-full object-cover" alt="" />
                                        ) : (
                                            <div className="w-full h-full flex items-center justify-center bg-gray-100 italic opacity-20 text-xs">No Cover</div>
                                        )}
                                    </div>
                                    <div>
                                        <div className="text-4xl font-black text-[#2d2a26] uppercase tracking-tighter italic mb-3">
                                            {uc.content.title}
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-tcd-orange font-extrabold text-[10px] uppercase tracking-widest">ARCHIVADO</span>
                                            <span className="w-1.5 h-1.5 bg-[#8c8471]/40 rounded-full"></span>
                                            <span className="text-[10px] font-bold text-[#8c8471]/60 uppercase tracking-widest">UID: {uc.content.externalId}</span>
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td className="p-10">
                                <button 
                                    onClick={() => updateStatus(uc.id, uc.status === 'COMPLETED' ? 'PLANNING' : 'COMPLETED')}
                                    className={`px-8 py-3 rounded-full text-[11px] font-black uppercase tracking-widest transition-all ${
                                        uc.status === 'COMPLETED' ? 'bg-[#e7f5e9] text-[#2d5a3a]' : 'bg-[#fff4e5] text-[#b8601a]'
                                    }`}
                                >
                                    {uc.status}
                                </button>
                            </td>
                            <td className="p-10">
                                <div className="flex gap-2">
                                    {[1, 2, 3, 4, 5].map(star => (
                                        <span key={star} className={`text-2xl ${star <= uc.rating ? "text-tcd-yellow" : "text-[#8c8471]/20"}`}>★</span>
                                    ))}
                                </div>
                            </td>
                            <td className="p-10 text-right">
                                <button 
                                    onClick={() => removeContent(uc.id)}
                                    className="text-[10px] font-black uppercase tracking-widest text-[#8c8471] hover:text-red-600 transition-colors"
                                >
                                    Eliminar del Archivo
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
