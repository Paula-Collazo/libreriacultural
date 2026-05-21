import React from 'react';
import { Link } from 'react-router-dom';

const LandingPage = () => {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center -mt-20">
            <div className="text-center max-w-4xl px-6">
                <span className="text-tcd-orange text-[10px] font-black uppercase tracking-[0.6em] mb-8 block animate-fade-in">
                    TU ARCHIVO CULTURAL PERSONAL
                </span>
                
                <h1 className="text-[120px] font-black tracking-tighter text-[#2d2a26] leading-[0.85] uppercase mb-12 italic">
                    Sigue todo lo que <span className="text-tcd-orange">ves</span>, lees y escuchas<span className="text-tcd-orange">.</span>
                </h1>
                
                <p className="text-2xl text-[#8c8471] font-medium tracking-tight mb-16 max-w-2xl mx-auto leading-relaxed">
                    Películas, series, libros, música. Sin ruido. <br/>
                    Solo tú y tu cultura en un entorno <span className="text-[#2d2a26] font-black italic">premium</span>.
                </p>

                <div className="flex flex-col sm:flex-row gap-6 justify-center items-center mb-24">
                    <Link to="/register" className="bg-[#b8601a] text-white px-12 py-6 rounded-full text-xs font-black uppercase tracking-[0.2em] shadow-2xl hover:bg-[#a05015] hover:scale-105 transition-all duration-500">
                        REGÍSTRATE GRATIS
                    </Link>
                    <Link to="/login" className="bg-white text-[#2d2a26] border border-[#ece7da] px-12 py-6 rounded-full text-xs font-black uppercase tracking-[0.2em] shadow-xl hover:bg-[#faf9f6] hover:scale-105 transition-all duration-500">
                        YA TENGO CUENTA
                    </Link>
                </div>

                {/* Categories Icons (Mimicking backend) */}
                <div className="flex justify-center gap-16 pb-20 border-b border-dashed border-[#ece7da]">
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-2xl">🎬</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Películas</span>
                    </div>
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-2xl">📺</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Series</span>
                    </div>
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-2xl">📖</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Libros</span>
                    </div>
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-2xl">🎵</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Discos</span>
                    </div>
                </div>
            </div>

            {/* Preview Section */}
            <div className="mt-32 w-full grid grid-cols-6 gap-4 opacity-10 grayscale hover:grayscale-0 transition-all duration-1000">
                {[1, 2, 3, 4, 5, 6].map(i => (
                    <div key={i} className="aspect-[2/3] bg-white rounded-2xl shadow-sm border border-[#ece7da]"></div>
                ))}
            </div>
        </div>
    );
};

export default LandingPage;
