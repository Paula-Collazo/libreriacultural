import React, { useState, useEffect } from 'react';

const API_BASE = 'http://localhost:8083';

const Comunidad = () => {
    const [members, setMembers] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [selectedMember, setSelectedMember] = useState(null);
    const [memberShelf, setMemberShelf] = useState([]);
    const [loadingShelf, setLoadingShelf] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);

    const handleSendRequest = (username) => {
        setActionLoading(true);
        fetch(`${API_BASE}/api/friends/request`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username })
        })
        .then(res => {
            if (res.ok) {
                setMembers(prev => prev.map(m => m.username === username ? { ...m, relationStatus: 'PENDING_SENT' } : m));
                alert(`¡Solicitud enviada a ${username}!`);
            } else {
                res.text().then(text => alert(text || "Error al enviar la solicitud"));
            }
        })
        .catch(err => {
            console.error("Error sending request:", err);
            alert("Error de conexión al enviar solicitud");
        })
        .finally(() => setActionLoading(false));
    };

    const handleAcceptRequest = (username) => {
        setActionLoading(true);
        fetch(`${API_BASE}/api/friends/accept`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username })
        })
        .then(res => {
            if (res.ok) {
                setMembers(prev => prev.map(m => m.username === username ? { ...m, relationStatus: 'ACCEPTED' } : m));
                alert(`¡Ahora eres amigo de ${username}!`);
            } else {
                res.text().then(text => alert(text || "Error al aceptar la solicitud"));
            }
        })
        .catch(err => {
            console.error("Error accepting request:", err);
            alert("Error de conexión al aceptar solicitud");
        })
        .finally(() => setActionLoading(false));
    };

    useEffect(() => {
        fetch(`${API_BASE}/api/community/members`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => {
                console.log('[Comunidad] Miembros recibidos:', data);
                setMembers(data || []);
                setLoading(false);
            })
            .catch(err => {
                console.error("Error fetching community members:", err);
                setLoading(false);
            });
    }, []);

    const fetchMemberShelf = (memberId) => {
        setLoadingShelf(true);
        fetch(`${API_BASE}/api/library/user/${memberId}`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => {
                setMemberShelf(data || []);
                setLoadingShelf(false);
            })
            .catch(err => {
                console.error("Error fetching member library:", err);
                setLoadingShelf(false);
            });
    };

    const sendRequest = async (receiverId) => {
        try {
            const res = await fetch(`${API_BASE}/api/friends/request`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ receiverId })
            });
            if (res.ok) {
                alert("¡Solicitud enviada correctamente!");
            } else {
                const msg = await res.text();
                alert(msg);
            }
        } catch (err) {
            console.error("Error sending request:", err);
            alert("Error al enviar la solicitud");
        }
    };

    const filteredMembers = members.filter(m => 
        m.username.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="animate-fade-in max-w-[1400px] mx-auto pb-40 px-4 md:px-8">
            {/* Header Banner */}
            <div className="pt-10 pb-20 border-b border-[#ece7da] mb-16 text-center md:text-left relative overflow-hidden">
                <div className="absolute top-0 right-0 w-96 h-96 bg-tcd-beige/25 rounded-full blur-3xl -z-10 translate-x-20 -translate-y-20"></div>
                <span className="text-tcd-orange text-xs font-black uppercase tracking-[0.6em] mb-4 block">
                    EL DEPARTAMENTO CULTURAL
                </span>
                <div className="flex flex-col md:flex-row justify-between items-center gap-12">
                    <div>
                        <h2 className="text-[60px] md:text-[85px] font-black tracking-tighter text-[#2d2a26] leading-[0.8] uppercase italic">
                            Comunidad<span className="text-tcd-orange">.</span>
                        </h2>
                        <p className="text-[#8c8471] text-sm font-semibold tracking-tight mt-4 uppercase">
                            Explora y descubre lo que leen, ven y escuchan otros miembros.
                        </p>
                    </div>

                    <div className="max-w-md w-full">
                        <div className="relative group">
                            <input 
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                placeholder="BUSCAR MIEMBRO POR USUARIO..."
                                className="w-full bg-white border-2 border-[#ece7da] px-8 py-5 rounded-full text-xs font-bold tracking-wider focus:outline-none focus:border-tcd-orange shadow-lg transition-all group-hover:shadow-xl"
                            />
                        </div>
                    </div>
                </div>
            </div>

            {loading ? (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {[1, 2, 3].map(i => (
                        <div key={i} className="bg-white border border-[#ece7da] rounded-[40px] p-10 h-[380px] animate-pulse"></div>
                    ))}
                </div>
            ) : filteredMembers.length === 0 ? (
                <div className="py-32 text-center border-2 border-dashed border-[#ece7da] rounded-[40px]">
                    <span className="text-3xl font-black text-[#2d2a26]/10 uppercase tracking-[0.4em] italic mb-6 block">No hay miembros</span>
                    <p className="text-xs font-black text-[#8c8471] uppercase tracking-widest">Intenta buscar con otros términos</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {filteredMembers.map((member) => (
                        <div key={member.id} className="bg-white border border-[#ece7da] rounded-[40px] p-10 hover:shadow-2xl hover:border-tcd-orange/20 hover:-translate-y-2 transition-all duration-500 flex flex-col justify-between group h-full">
                            <div>
                                {/* Avatar and Name */}
                                <div className="flex items-center gap-6 mb-6">
                                    <div className="w-20 h-20 rounded-full overflow-hidden bg-gradient-to-br from-[#c4621a] to-[#e07a3a] flex items-center justify-center text-2xl font-black border-4 border-white flex-shrink-0 group-hover:scale-105 transition-transform duration-500 shadow-lg text-white">
                                        {member.profilePicture ? (
                                            <img src={member.profilePicture} alt={member.username} className="w-full h-full object-cover" />
                                        ) : (
                                            member.username.charAt(0).toUpperCase()
                                        )}
                                    </div>
                                    <div className="min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="tcd-badge text-[7px] px-3 py-1 shadow-sm">MIEMBRO</span>
                                            {member.favoriteGenre && member.favoriteGenre !== 'Todo' && (
                                                <span className="bg-[#f0ece3] text-[#8c8471] text-[7px] font-black uppercase tracking-wider px-2 py-0.5 rounded-md border border-[#ece7da] truncate max-w-[100px]">
                                                    {member.favoriteGenre}
                                                </span>
                                            )}
                                        </div>
                                        <h3 className="text-2xl font-black tracking-tighter text-[#2d2a26] uppercase truncate italic leading-none">
                                            {member.username}
                                        </h3>
                                    </div>
                                </div>

                                {/* Bio */}
                                <div className="h-24 mb-6">
                                    <p className="text-xs text-[#8c8471] font-medium tracking-tight line-clamp-4 leading-relaxed italic">
                                        {member.bio ? `"${member.bio}"` : '"Sin biografía aún."'}
                                    </p>
                                </div>

                                {/* Showcase items */}
                                <div className="h-32 mb-8">
                                    {member.showcase && member.showcase.length > 0 ? (
                                        <>
                                            <p className="text-[8px] font-black text-[#8c8471]/60 uppercase tracking-widest mb-3">DESTACADOS EN SU ARCHIVO</p>
                                            <div className="flex gap-3 overflow-x-auto pb-1 no-scrollbar">
                                                {member.showcase.map((item, idx) => (
                                                    <div key={idx} className="w-16 h-20 rounded-xl overflow-hidden border border-[#ece7da] flex-shrink-0 bg-[#f4efdf] relative group/showcase-item shadow-sm" title={item.title}>
                                                        {item.coverUrl ? (
                                                            <img src={item.coverUrl} alt={item.title} className="w-full h-full object-cover" />
                                                        ) : (
                                                            <div className="w-full h-full flex items-center justify-center text-[7px] font-black uppercase text-[#8c8471] text-center p-1">
                                                                {item.type === 'PELICULA' ? 'PEL' : item.type === 'LIBRO' ? 'LIB' : item.type === 'SERIE' ? 'SER' : 'DIS'}
                                                            </div>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        </>
                                    ) : (
                                        <div className="h-full flex items-center justify-center border border-dashed border-[#ece7da] rounded-2xl bg-[#fdfcf9] opacity-40 py-6">
                                            <p className="text-[7px] font-black text-[#8c8471] uppercase tracking-widest">Sin destacados</p>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Stats & Actions */}
                            <div className="border-t border-[#ece7da] pt-6 flex items-center justify-between mt-auto">
                                <div className="flex gap-4">
                                    <div className="flex flex-col">
                                        <span className="text-[7px] font-black text-[#8c8471] uppercase tracking-wider">INDEXADOS</span>
                                        <strong className="text-xl font-black text-[#2d2a26]">{member.totalCount}</strong>
                                    </div>
                                    <div className="grid grid-cols-2 gap-x-3 gap-y-0.5 text-[8px] font-black text-[#8c8471] border-l border-[#ece7da]/60 pl-4 uppercase tracking-tighter">
                                        <span>PEL: {member.stats.peliculas}</span>
                                        <span>SER: {member.stats.series}</span>
                                        <span>LIB: {member.stats.libros}</span>
                                        <span>DIS: {member.stats.discos}</span>
                                    </div>
                                </div>

                                <div className="flex gap-2">
                                    {/* Mostrar botón si NO es uno mismo ni tiene ya relación activa */}
                                    {!['SELF', 'PENDING_SENT', 'PENDING_RECEIVED', 'ACCEPTED'].includes(member.relationStatus) && (
                                        <button 
                                            onClick={() => handleSendRequest(member.username)}
                                            disabled={actionLoading}
                                            className="bg-[#c4621a] hover:bg-[#a05015] text-white text-[9px] font-black uppercase tracking-widest px-4 py-3 rounded-full transition-all shadow-md hover:scale-105 flex items-center gap-1.5"
                                            title="Enviar Solicitud de Amistad"
                                        >
                                            <span>+ SEGUIR</span>
                                        </button>
                                    )}
                                    {member.relationStatus === 'PENDING_SENT' && (
                                        <button 
                                            disabled
                                            className="bg-[#8c8471]/30 text-[#8c8471] text-[9px] font-black uppercase tracking-widest px-4 py-3 rounded-full flex items-center gap-1.5 cursor-not-allowed"
                                        >
                                            <span>PENDIENTE</span>
                                        </button>
                                    )}
                                    {member.relationStatus === 'PENDING_RECEIVED' && (
                                        <button 
                                            onClick={() => handleAcceptRequest(member.username)}
                                            disabled={actionLoading}
                                            className="bg-[#65a30d] hover:bg-[#4d7c0f] text-white text-[9px] font-black uppercase tracking-widest px-4 py-3 rounded-full transition-all shadow-md hover:scale-105 flex items-center gap-1.5"
                                            title="Aceptar Solicitud de Amistad"
                                        >
                                            <span>ACEPTAR</span>
                                        </button>
                                    )}
                                    {member.relationStatus === 'ACCEPTED' && (
                                        <div 
                                            className="bg-[#f0ece3] text-[#b8601a] border border-[#ece7da] text-[9px] font-black uppercase tracking-widest px-4 py-3 rounded-full flex items-center gap-1.5 shadow-sm"
                                        >
                                            <span className="text-pink-500 font-bold">♥</span>
                                            <span>AMIGOS</span>
                                        </div>
                                    )}
                                    <button 
                                        onClick={() => {
                                            setSelectedMember(member);
                                            fetchMemberShelf(member.id);
                                        }}
                                        className="bg-[#2d2a26] text-white hover:bg-tcd-orange text-[9px] font-black uppercase tracking-widest px-4 py-3 rounded-full transition-all shadow-md hover:scale-105"
                                    >
                                        Estantería
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Public Shelf Modal */}
            {selectedMember && (
                <div className="fixed inset-0 bg-[#2d2a26]/40 backdrop-blur-md flex items-center justify-center z-[1000] p-4 animate-fade-in">
                    <div className="bg-white rounded-[40px] border border-[#ece7da] p-10 md:p-12 w-full max-w-5xl max-h-[90vh] overflow-y-auto shadow-2xl">
                        {/* Modal Header */}
                        <div className="flex justify-between items-start mb-10 border-b border-[#ece7da] pb-8">
                            <div className="flex items-center gap-6">
                                <div className="w-16 h-16 rounded-full overflow-hidden bg-gradient-to-br from-[#c4621a] to-[#e07a3a] flex items-center justify-center text-xl font-black border-4 border-white shadow-md text-white">
                                    {selectedMember.profilePicture ? (
                                        <img src={selectedMember.profilePicture} alt={selectedMember.username} className="w-full h-full object-cover" />
                                    ) : (
                                        selectedMember.username.charAt(0).toUpperCase()
                                    )}
                                </div>
                                <div>
                                    <span className="text-tcd-orange text-[9px] font-black uppercase tracking-[0.4em] mb-1 block">Estantería Pública</span>
                                    <h2 className="text-3xl font-black text-[#2d2a26] uppercase italic tracking-tighter leading-none">
                                        {selectedMember.username}<span className="text-tcd-orange">.</span>
                                    </h2>
                                </div>
                            </div>
                            <button 
                                onClick={() => { setSelectedMember(null); setMemberShelf([]); }}
                                className="w-10 h-10 rounded-full border border-[#ece7da] flex items-center justify-center hover:bg-[#f4efdf] transition-all font-bold"
                            >
                                ✕
                            </button>
                        </div>

                        {/* Modal Body */}
                        {loadingShelf ? (
                            <div className="py-24 text-center">
                                <div className="w-12 h-12 border-4 border-tcd-orange border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                                <p className="text-xs font-black text-[#8c8471] uppercase tracking-[0.3em]">Cargando estantería de {selectedMember.username}...</p>
                            </div>
                        ) : memberShelf.length === 0 ? (
                            <div className="py-20 text-center bg-[#fdfaf5] border border-dashed border-[#ece7da] rounded-3xl">
                                <p className="text-xs font-black text-[#8c8471] uppercase tracking-widest">Este miembro no tiene ítems en su biblioteca aún.</p>
                            </div>
                        ) : (
                            <div className="space-y-12">
                                {/* Group items by type */}
                                {['PELICULA', 'SERIE', 'LIBRO', 'DISCO'].map(type => {
                                    const items = memberShelf.filter(it => it.content.type === type || it.content.type?.toUpperCase() === type);
                                    if (items.length === 0) return null;

                                    const typeLabel = 
                                        type === 'PELICULA' ? 'Películas' :
                                        type === 'SERIE' ? 'Series' :
                                        type === 'LIBRO' ? 'Libros' : 'Música';

                                    const badgeColor = 
                                        type === 'PELICULA' ? '#f97316' :
                                        type === 'SERIE' ? '#65a30d' :
                                        type === 'LIBRO' ? '#d97706' : '#db2777';

                                    return (
                                        <div key={type} className="animate-in slide-in-from-bottom-3 duration-500">
                                            <div className="flex items-center justify-between mb-6 border-b border-[#ece7da]/60 pb-3" style={{ borderLeft: `4px solid ${badgeColor}`, paddingLeft: 12 }}>
                                                <h3 className="text-xs font-black text-[#2d2a26] uppercase tracking-[0.2em]">{typeLabel}</h3>
                                                <span className="bg-[#f0ece3] text-[#8c8471] text-[9px] font-black uppercase tracking-wider px-2 py-0.5 rounded-md border border-[#ece7da]">{items.length}</span>
                                            </div>

                                            <div className="explore-grid">
                                                {items.map(item => (
                                                    <div key={item.id} className="explore-item group relative">
                                                        <div className="aspect-[2/3] bg-white rounded-2xl overflow-hidden shadow-sm border border-[#ece7da] relative">
                                                            {item.content.coverUrl ? (
                                                                <img src={item.content.coverUrl} alt={item.content.title} className="w-full h-full object-cover" />
                                                            ) : (
                                                                <div className="w-full h-full flex items-center justify-center p-4 text-center bg-[#f4efdf]">
                                                                    <span className="text-[9px] font-black opacity-30 uppercase tracking-[0.1em] text-[#2d2a26] leading-tight">{item.content.title}</span>
                                                                </div>
                                                            )}
                                                            {item.favorite && (
                                                                <div className="absolute top-2 right-2 bg-pink-500 text-white w-6 h-6 rounded-full flex items-center justify-center text-xs shadow-md">♥</div>
                                                            )}
                                                        </div>
                                                        <div className="min-h-[40px]">
                                                            <p className="text-[10px] font-black text-[#2d2a26] uppercase truncate tracking-tighter mt-3 leading-tight italic" title={item.content.title}>
                                                                {item.content.title}
                                                            </p>
                                                            <span className="bg-[#f0ece3] text-[#8c8471] text-[7px] font-black uppercase tracking-widest px-1.5 py-0.5 rounded mt-1 inline-block border border-[#ece7da]/60">
                                                                {item.status === 'PLANNING' ? 'PENDIENTE' : item.status === 'visto' || item.status === 'leido' || item.status === 'COMPLETED' ? 'COMPLETADO' : 'SIGUIENDO'}
                                                            </span>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default Comunidad;
