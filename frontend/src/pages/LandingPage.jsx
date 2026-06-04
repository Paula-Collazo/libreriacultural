import React from 'react';
import { Link } from 'react-router-dom';

const LandingPage = () => {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center -mt-20">
            <div className="text-center max-w-4xl px-6">
                <span className="text-tcd-orange text-[10px] font-black uppercase tracking-[0.6em] mb-8 block animate-fade-in">
                    TU ARCHIVO CULTURAL PERSONAL
                </span>
                
                <h1 className="text-[50px] md:text-[70px] font-black tracking-tighter text-[#2d2a26] leading-[0.85] uppercase mb-10 italic">
                    Sigue todo lo que <span className="text-tcd-orange">ves</span>, lees y escuchas<span className="text-tcd-orange">.</span>
                </h1>
                
                <p className="text-lg text-[#8c8471] font-medium tracking-tight mb-10 max-w-2xl mx-auto leading-relaxed">
                    Películas, series, libros, música. Sin ruido. <br/>
                    Solo tú y tu cultura en un entorno <span className="text-[#2d2a26] font-black italic">premium</span>.
                </p>

                <div className="flex flex-col sm:flex-row gap-6 justify-center items-center mb-20">
                    <Link to="/register" className="bg-[#b8601a] text-white px-10 py-5 rounded-full text-[11px] font-black uppercase tracking-[0.2em] shadow-2xl hover:bg-[#a05015] hover:scale-105 transition-all duration-500">
                        REGÍSTRATE GRATIS
                    </Link>
                    <Link to="/login" className="bg-white text-[#2d2a26] border border-[#ece7da] px-10 py-5 rounded-full text-[11px] font-black uppercase tracking-[0.2em] shadow-xl hover:bg-[#faf9f6] hover:scale-105 transition-all duration-500">
                        YA TENGO CUENTA
                    </Link>
                </div>

                {/* Categories Icons (Mimicking backend) */}
                <div className="flex justify-center gap-16 pb-20 border-b border-dashed border-[#ece7da]">
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-[10px] font-black uppercase text-[#8c8471]">PEL</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Películas</span>
                    </div>
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-[10px] font-black uppercase text-[#8c8471]">SER</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Series</span>
                    </div>
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-[10px] font-black uppercase text-[#8c8471]">LIB</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Libros</span>
                    </div>
                    <div className="flex flex-col items-center gap-4 opacity-40 hover:opacity-100 transition-opacity">
                        <div className="w-16 h-16 bg-[#f4efdf] rounded-2xl flex items-center justify-center text-[10px] font-black uppercase text-[#8c8471]">DIS</div>
                        <span className="text-[10px] font-black uppercase tracking-widest">Discos</span>
                    </div>
                </div>
            </div>

            {/* Preview Section */}
            <div className="mt-32 w-full grid grid-cols-2 md:grid-cols-6 gap-6 opacity-30 grayscale hover:grayscale-0 hover:opacity-85 transition-all duration-[1.2s] ease-out">
                {[
                    { title: "Interstellar", url: "https://image.tmdb.org/t/p/w500/gEU2QvJWzIF7efg2t4t7f4j2zsf.jpg" },
                    { title: "OK Computer", url: "https://i.scdn.co/image/ab67616d0000b273934d4007d4b245084a441113" },
                    { title: "Dune", url: "https://image.tmdb.org/t/p/w500/d57ubvVpC7pS79uKq9Y96C4v0pS.jpg" },
                    { title: "Breaking Bad", url: "https://image.tmdb.org/t/p/w500/ztkUQvHnd79fv6rnB69xz9vU046.jpg" },
                    { title: "AM", url: "https://i.scdn.co/image/ab67616d0000b2737bc0f57dfc201243a860773c" },
                    { title: "The Godfather", url: "https://image.tmdb.org/t/p/w500/3bhkrjOiERvSTqX6DHAs679Pzmb.jpg" }
                ].map((item, idx) => (
                    <div key={idx} className="aspect-[2/3] bg-white rounded-[28px] overflow-hidden shadow-2xl border border-[#ece7da] hover:-translate-y-6 hover:rotate-3 transition-all duration-700 cursor-pointer">
                        <img src={item.url} alt={item.title} className="w-full h-full object-cover" />
                    </div>
                ))}
            </div>
        </div>
    );
};

export default LandingPage;
