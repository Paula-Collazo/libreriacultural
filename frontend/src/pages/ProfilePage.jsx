import React, { useState, useEffect } from 'react';

const ProfilePage = () => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);

    const refreshProfile = () => {
        setLoading(true);
        fetch('http://127.0.0.1:8083/api/profile', {
            headers: { 'Accept': 'application/json' }
        })
        .then(res => res.json())
        .then(json => {
            setData(json);
            setLoading(false);
        })
        .catch(err => {
            console.error("Error loading profile:", err);
            setLoading(false);
        });
    };

    useEffect(() => {
        refreshProfile();
    }, []);

    const handleDelete = (id) => {
        if (window.confirm('¿Eliminar este registro permanentemente?')) {
            fetch(`http://127.0.0.1:8083/api/content/${id}/delete`, { method: 'POST' })
                .then(res => {
                    if(res.ok) refreshProfile();
                    else alert("Error al eliminar");
                });
        }
    };

    const handleUpdateStatus = (id, currentStatus) => {
        const newStatus = currentStatus === 'COMPLETED' ? 'PLANNING' : 'COMPLETED';
        fetch(`http://127.0.0.1:8083/api/content/${id}/status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        }).then(res => {
            if(res.ok) refreshProfile();
            else alert("Error al actualizar estado");
        });
    };

    if (loading && !data) return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] gap-8">
            <div className="avatar-large animate-pulse bg-[#f4efdf] opacity-50"></div>
            <div className="text-xl font-black opacity-20 italic uppercase tracking-[0.5em]">Sincronizando Archivo...</div>
        </div>
    );

    const user = data?.user || { username: 'USUARIO' };
    const stats = data?.stats || { peliculas: 0, series: 0, libros: 0, discos: 0, paginasLeidas: 0, episodiosVistos: 0 };
    const totalCount = data?.totalCount || 0;
    const items = data?.content || [];

    return (
        <div className="animate-fade-in pb-40">
            {/* PROFILE HERO */}
            <section className="glass-card mb-16 p-16 mt-8">
                <div className="flex flex-col lg:flex-row items-center gap-16">
                    <div className="relative group">
                        <div className="avatar-large group-hover:scale-105 group-hover:rotate-3 transition-transform duration-700">
                            {user.username.charAt(0).toUpperCase()}
                        </div>
                        <div className="absolute -bottom-4 left-1/2 -translate-x-1/2 whitespace-nowrap">
                            <span className="tcd-badge shadow-xl">MIEMBRO CULTURAL</span>
                        </div>
                    </div>
                
                    <div className="flex-1 text-center lg:text-left">
                        <span className="text-tcd-orange text-[11px] font-black uppercase tracking-[0.4em] mb-4 block">Director del Departamento</span>
                        <h1 className="text-[120px] font-black leading-[0.8] tracking-tighter text-[#2d2a26] uppercase mb-12">
                            {user.username}<span className="text-tcd-orange">.</span>
                        </h1>
                        
                        <div className="flex flex-wrap items-center justify-center lg:justify-start gap-8">
                            <div className="summary-pill flex flex-col items-end">
                                <span className="text-[10px] uppercase font-black tracking-widest text-[#8c8471] mb-2">INDEXED ITEMS</span>
                                <strong className="text-7xl font-black tracking-tighter text-[#2d2a26] leading-none">{totalCount}</strong>
                            </div>
                            
                            <div className="flex gap-4">
                                <button className="btn-community group">
                                    <span className="flex flex-col items-center">
                                        <span className="text-[9px] font-black tracking-widest opacity-60 mb-1">NETWORK</span>
                                        <span>COMUNIDAD</span>
                                    </span>
                                </button>
                                <button onClick={refreshProfile} className="w-[60px] h-[60px] rounded-full border border-[#ece7da] flex items-center justify-center hover:bg-white hover:shadow-xl transition-all">
                                    <span className={loading ? "animate-spin" : ""}>↻</span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* STATS TILES */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 mb-32">
                {[
                    { label: 'Cinematografía', val: stats.peliculas, unit: 'FILMS', color: '#b8601a' },
                    { label: 'Series TV', val: stats.series, unit: 'EPS', color: '#b8601a' },
                    { label: 'Literatura', val: stats.libros, unit: 'VOLS', color: '#b8601a' },
                    { label: 'Impacto', val: stats.episodiosVistos + stats.paginasLeidas, unit: 'PTS', color: '#fbbf24' }
                ].map((s, i) => (
                    <div key={i} className="stat-card group">
                        <span className="stat-label">{s.label}</span>
                        <div className="flex items-baseline gap-3">
                             <strong className="stat-value group-hover:text-tcd-orange transition-colors">{s.val}</strong>
                             <span className="text-[11px] font-black text-tcd-orange/50 uppercase tracking-widest">{s.unit}</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* LIBRARY SECTION */}
            <div className="flex items-baseline justify-between mb-16 border-b-2 border-[#ece7da] pb-10">
               <div>
                  <span className="text-tcd-orange text-[10px] font-black uppercase tracking-[0.5em] mb-2 block">Your Private Archive</span>
                  <h2 className="text-[80px] font-black tracking-tighter text-[#2d2a26] uppercase leading-none italic">Mi Biblioteca<span className="text-tcd-orange">.</span></h2>
               </div>
               <div className="flex gap-4">
                  <button className="px-8 py-3 bg-white border border-[#ece7da] rounded-full text-[10px] font-black uppercase tracking-widest hover:border-tcd-orange transition-all">Exportar PDF</button>
                  <button onClick={() => window.location.href='/explorar'} className="px-8 py-3 bg-[#b8601a] text-white rounded-full text-[10px] font-black uppercase tracking-widest hover:bg-[#a05015] shadow-lg transition-all">Añadir Nuevo</button>
               </div>
            </div>

            {items.length === 0 ? (
                <div className="py-40 text-center glass-card border-dashed">
                    <span className="text-4xl font-black text-[#2d2a26]/10 uppercase tracking-[0.4em] italic mb-12 block">Archivo vacío</span>
                    <button 
                        onClick={() => window.location.href='/explorar'}
                        className="bg-[#2d2a26] text-white px-12 py-5 rounded-full font-black text-xs uppercase tracking-[0.2em] hover:bg-tcd-orange transition-all shadow-2xl"
                    >
                        Indexar Contenido
                    </button>
                </div>
            ) : (
                <div className="library-table-container">
                    <table className="w-full">
                        <thead>
                            <tr className="bg-[#fcf8f0] border-b-2 border-[#ece7da]">
                                <th className="p-10 text-left text-[11px] font-black uppercase tracking-[0.2em] text-[#8c8471]">ARTÍCULO</th>
                                <th className="p-10 text-left text-[11px] font-black uppercase tracking-[0.2em] text-[#8c8471]">COLECCIÓN</th>
                                <th className="p-10 text-left text-[11px] font-black uppercase tracking-[0.2em] text-[#8c8471]">ESTADO</th>
                                <th className="p-10 text-right text-[11px] font-black uppercase tracking-[0.2em] text-[#8c8471]">GESTIÓN</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y-2 divide-[#ece7da]">
                            {items.map(item => (
                                <tr key={item.id} className="hover:bg-[#fdfcf9] transition-colors group">
                                    <td className="p-10">
                                        <div className="flex items-center gap-10">
                                            <div className="w-24 h-36 bg-[#f4efdf] rounded-[20px] overflow-hidden shadow-xl flex-shrink-0 group-hover:scale-110 group-hover:-rotate-3 transition-transform duration-700">
                                                {item.content.coverUrl ? (
                                                    <img src={item.content.coverUrl} className="w-full h-full object-cover" alt="" />
                                                ) : (
                                                    <div className="w-full h-full flex items-center justify-center text-[10px] font-black opacity-10">TCD</div>
                                                )}
                                            </div>
                                            <div>
                                                <div className="text-[40px] font-black text-[#2d2a26] uppercase tracking-tighter italic mb-2 leading-none">
                                                    {item.content.title}
                                                </div>
                                                <div className="flex items-center gap-4">
                                                    <span className="text-tcd-orange font-black text-[10px] uppercase tracking-widest">
                                                        {item.content.type}
                                                    </span>
                                                    <span className="w-1.5 h-1.5 bg-[#8c8471]/30 rounded-full"></span>
                                                    <span className="text-[10px] font-bold text-[#8c8471]/40 uppercase tracking-widest italic">
                                                        Added on {new Date().toLocaleDateString()}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="p-10">
                                        <div className="text-[11px] font-black uppercase tracking-widest text-[#8c8471]">
                                            Digital Archive
                                        </div>
                                    </td>
                                    <td className="p-10">
                                        <button 
                                            onClick={() => handleUpdateStatus(item.id, item.status)}
                                            className={`px-8 py-3 rounded-full text-[10px] font-black uppercase tracking-[0.2em] transition-all shadow-sm ${
                                                item.status === 'COMPLETED' ? 'bg-[#e7f5e9] text-[#1e7e34]' : 'bg-[#fff4e5] text-[#b8601a]'
                                            }`}
                                        >
                                            {item.status}
                                        </button>
                                    </td>
                                    <td className="p-10 text-right">
                                        <button 
                                            onClick={() => handleDelete(item.id)}
                                            className="text-[10px] font-black uppercase tracking-widest text-[#8c8471] hover:text-red-600 transition-colors opacity-30 hover:opacity-100 px-6 py-2 border border-[#ece7da] rounded-full"
                                        >
                                            Eliminar
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default ProfilePage;
