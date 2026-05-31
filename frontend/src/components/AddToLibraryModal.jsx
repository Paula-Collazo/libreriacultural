import React, { useState } from 'react';

const AddToLibraryModal = ({ isOpen, onClose, onConfirm, itemTitle, itemType }) => {
    const [status, setStatus] = useState(() => {
        if (itemType === 'PELICULA') return 'no_visto';
        if (itemType === 'LIBRO') return 'no_iniciado';
        if (itemType === 'SERIE') return 'seguimiento_episodios';
        return 'seguimiento_canciones';
    });
    const [completionDate, setCompletionDate] = useState('');
    const [rating, setRating] = useState(0);
    const [isFavorite, setIsFavorite] = useState(false);

    if (!isOpen) return null;

    const isCompleted = 
        status === 'visto' || 
        status === 'leido' || 
        status === 'completado';

    const handleConfirm = () => {
        onConfirm({
            status,
            completionDate: isCompleted ? completionDate : null,
            rating: isCompleted ? rating : 0,
            favorite: isFavorite
        });
    };

    const renderStars = () => {
        return (
            <div className="flex gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                    <button
                        key={star}
                        type="button"
                        onClick={() => setRating(star)}
                        className={`text-3xl transition-all ${star <= rating ? 'text-tcd-orange scale-110' : 'text-[#ece7da] hover:text-[#d3cfc4]'}`}
                    >
                        ★
                    </button>
                ))}
            </div>
        );
    };

    return (
        <div className="fixed inset-0 z-[9999] flex items-start justify-center p-4 pt-20 animate-fade-in">
            {/* Backdrop */}
            <div 
                className="absolute inset-0 bg-[#2d2a26]/70 backdrop-blur-md" 
                onClick={onClose}
            ></div>

            {/* Modal Box */}
            <div className="relative w-full max-w-xl bg-[#faf9f5] border border-[#ece7da] rounded-[40px] shadow-2xl p-10 overflow-hidden transform scale-100 transition-transform duration-300">
                <div className="absolute top-0 left-0 right-0 h-2 bg-gradient-to-r from-tcd-orange to-[#b8601a]"></div>

                <div className="mb-8 flex justify-between items-start">
                    <div className="flex-1">
                        <span className="text-[10px] font-black text-tcd-orange uppercase tracking-[0.4em] mb-2 block">
                            AÑADIR AL ARCHIVO CULTURAL
                        </span>
                        <h3 className="text-3xl font-black text-[#2d2a26] uppercase italic tracking-tighter leading-tight">
                            {itemTitle}
                        </h3>
                    </div>
                    <button 
                        onClick={() => setIsFavorite(!isFavorite)}
                        className={`p-4 rounded-2xl border-2 transition-all ${isFavorite ? 'bg-red-50 border-red-200 text-red-500 shadow-inner' : 'bg-white border-[#ece7da] text-[#8c8471] hover:border-red-200'}`}
                        title="Añadir a favoritos (Like)"
                    >
                        <span className="text-2xl">{isFavorite ? '❤️' : '🤍'}</span>
                    </button>
                </div>

                <div className="space-y-8">
                    {/* Status Selector */}
                    <div>
                        <label className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.3em] mb-3 block">
                            ¿Cuál es su estado actual?
                        </label>
                        <div className="grid grid-cols-2 gap-4">
                            {/* ... (existing buttons remain same logic but maybe style-updated if needed) ... */}
                            {(itemType === 'PELICULA' || itemType === 'MOVIE') && (
                                <>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('no_visto')}
                                        className={`py-4 px-6 rounded-2xl text-xs font-black uppercase tracking-wider transition-all border ${status === 'no_visto' ? 'bg-[#fef3c7] text-[#92400e] border-[#fde68a] shadow-md' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-[#fde68a]'}`}
                                    >
                                        Pendiente
                                    </button>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('visto')}
                                        className={`py-4 px-6 rounded-2xl text-xs font-black uppercase tracking-wider transition-all border ${status === 'visto' ? 'bg-[#e9f9ee] text-[#166534] border-[#d1f2d9] shadow-md' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-[#d1f2d9]'}`}
                                    >
                                        Visto
                                    </button>
                                </>
                            )}

                            {(itemType === 'LIBRO' || itemType === 'BOOK') && (
                                <>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('no_iniciado')}
                                        className={`py-4 px-4 rounded-2xl text-[10px] font-black uppercase tracking-wider transition-all border ${status === 'no_iniciado' ? 'bg-[#2d2a26] text-white border-[#2d2a26] shadow-lg' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-[#2d2a26]'}`}
                                    >
                                        Pendiente
                                    </button>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('leido')}
                                        className={`py-4 px-4 rounded-2xl text-[10px] font-black uppercase tracking-wider transition-all border ${status === 'leido' ? 'bg-tcd-orange text-white border-tcd-orange shadow-lg' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-tcd-orange'}`}
                                    >
                                        Leído
                                    </button>
                                </>
                            )}

                            {(itemType === 'SERIE' || itemType === 'TV') && (
                                <>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('seguimiento_episodios')}
                                        className={`py-4 px-4 rounded-2xl text-[10px] font-black uppercase tracking-wider transition-all border ${status === 'seguimiento_episodios' ? 'bg-[#2d2a26] text-white border-[#2d2a26] shadow-lg' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-[#2d2a26]'}`}
                                    >
                                        Pendiente / Siguiendo
                                    </button>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('visto')}
                                        className={`py-4 px-4 rounded-2xl text-[10px] font-black uppercase tracking-wider transition-all border ${status === 'visto' ? 'bg-tcd-orange text-white border-tcd-orange shadow-lg' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-tcd-orange'}`}
                                    >
                                        Visto completo
                                    </button>
                                </>
                            )}

                            {(itemType === 'DISCO' || itemType === 'MUSICA' || itemType === 'ALBUM') && (
                                <>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('seguimiento_canciones')}
                                        className={`py-4 px-4 rounded-2xl text-[10px] font-black uppercase tracking-wider transition-all border ${status === 'seguimiento_canciones' ? 'bg-[#2d2a26] text-white border-[#2d2a26] shadow-lg' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-[#2d2a26]'}`}
                                    >
                                        Pendiente / Escuchando
                                    </button>
                                    <button 
                                        type="button"
                                        onClick={() => setStatus('completado')}
                                        className={`py-4 px-4 rounded-2xl text-[10px] font-black uppercase tracking-wider transition-all border ${status === 'completado' ? 'bg-tcd-orange text-white border-tcd-orange shadow-lg' : 'bg-white text-[#8c8471] border-[#ece7da] hover:border-tcd-orange'}`}
                                    >
                                        Escuchado
                                    </button>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Completion Date & Rating */}
                    {isCompleted && (
                        <div className="grid grid-cols-2 gap-8 animate-fade-in">
                            <div>
                                <label className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.3em] mb-3 block">
                                    Fecha de completado
                                </label>
                                <input 
                                    type="date"
                                    value={completionDate}
                                    onChange={(e) => setCompletionDate(e.target.value)}
                                    className="w-full bg-white border-2 border-[#ece7da] rounded-2xl px-5 py-3 text-sm font-bold text-[#2d2a26] focus:outline-none focus:border-tcd-orange shadow-inner"
                                />
                            </div>
                            <div>
                                <label className="text-[10px] font-black text-[#8c8471] uppercase tracking-[0.3em] mb-3 block">
                                    Tu puntuación
                                </label>
                                {renderStars()}
                            </div>
                        </div>
                    )}
                </div>

                <div className="flex gap-4 mt-12">
                    <button 
                        type="button"
                        onClick={onClose}
                        className="flex-1 py-5 rounded-2xl border border-[#ece7da] text-[#8c8471] text-[10px] font-black uppercase tracking-widest hover:bg-[#ece7da]/20 transition-all"
                    >
                        Cancelar
                    </button>
                    <button 
                        type="button"
                        onClick={handleConfirm}
                        className="flex-1 py-5 rounded-2xl bg-tcd-orange text-white text-[10px] font-black uppercase tracking-widest hover:bg-[#a05015] shadow-lg transition-all"
                    >
                        Confirmar e Importar
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AddToLibraryModal;
