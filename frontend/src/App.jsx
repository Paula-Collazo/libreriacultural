import React from 'react';
import { motion } from 'framer-motion';
import { Play, Book, Music, Tv, Search, User, ShieldCheck } from 'lucide-react';

function App() {
  return (
    <div className="min-h-screen relative">
      <div className="hero-gradient" />
      
      {/* Topbar */}
      <nav className="flex items-center justify-between px-8 py-6 max-w-7xl mx-auto">
        <div className="text-2xl font-bold tracking-tight" style={{ color: 'var(--accent)' }}>
          TCD<span className="text-white">.</span>
        </div>
        <div className="hidden md:flex items-center gap-8 text-sm font-medium opacity-70">
          <a href="#" className="hover:opacity-100 transition-opacity">PELÍCULAS</a>
          <a href="#" className="hover:opacity-100 transition-opacity">SERIES</a>
          <a href="#" className="hover:opacity-100 transition-opacity">LIBROS</a>
          <a href="#" className="hover:opacity-100 transition-opacity">MÚSICA</a>
        </div>
        <div className="flex items-center gap-4">
          <button className="text-sm font-semibold hover:opacity-70">ENTRAR</button>
          <button className="btn-primary text-sm">EMPEZAR GRATIS</button>
        </div>
      </nav>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-8 pt-20 pb-32">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <motion.div 
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, ease: "easeOut" }}
          >
            <span className="text-xs font-bold tracking-[0.2em] text-[#c8773a] uppercase mb-4 block">
              Tu archivo cultural definitivo
            </span>
            <h1 className="text-6xl md:text-7xl font-bold leading-[1.1] mb-8">
              Rastrea todo lo que <span style={{ color: 'var(--accent)' }}>disfrutas.</span>
            </h1>
            <p className="text-lg text-muted mb-10 leading-relaxed max-w-lg" style={{ color: 'var(--text-muted)' }}>
              Organiza tus películas, libros, series y música en un solo lugar. 
              Sin distracciones, solo tú y tu cultura.
            </p>
            <div className="flex flex-wrap gap-4">
              <button className="btn-primary px-10 py-4 text-lg">Crear mi cuenta</button>
              <button className="glass-card px-10 py-4 text-lg font-semibold hover:bg-white/10 transition-colors flex items-center gap-2">
                <Play size={18} fill="currentColor" /> Ver demo
              </button>
            </div>
          </motion.div>

          {/* Cards Preview */}
          <motion.div 
            className="grid grid-cols-2 gap-4"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            <div className="space-y-4 pt-12">
              <div className="glass-card p-6 transform hover:-translate-y-2 transition-transform">
                <Tv className="text-[#c8773a] mb-4" />
                <h3 className="font-bold mb-2">Series</h3>
                <p className="text-xs text-muted">Controla tus temporadas y episodios vistos.</p>
              </div>
              <div className="glass-card p-6 transform hover:-translate-y-2 transition-transform">
                <Music className="text-[#c8773a] mb-4" />
                <h3 className="font-bold mb-2">Música</h3>
                <p className="text-xs text-muted">Guarda tus álbumes y canciones favoritas.</p>
              </div>
            </div>
            <div className="space-y-4">
              <div className="glass-card p-6 transform hover:-translate-y-2 transition-transform">
                <Play className="text-[#c8773a] mb-4" />
                <h3 className="font-bold mb-2">Películas</h3>
                <p className="text-xs text-muted">Tu historial de cine personal y organizado.</p>
              </div>
              <div className="glass-card p-6 transform hover:-translate-y-2 transition-transform">
                <Book className="text-[#c8773a] mb-4" />
                <h3 className="font-bold mb-2">Libros</h3>
                <p className="text-xs text-muted">Rastrea tu progreso de lectura página a página.</p>
              </div>
            </div>
          </motion.div>
        </div>
      </main>

      {/* Trust Section */}
      <section className="border-t border-[#262626] bg-[#0a0a0a]">
        <div className="max-w-7xl mx-auto px-8 py-12 flex flex-wrap justify-between items-center gap-8 opacity-50">
          <div className="flex items-center gap-2 font-bold"><ShieldCheck /> SEGURO</div>
          <div className="flex items-center gap-2 font-bold"><Search /> BÚSQUEDA API</div>
          <div className="flex items-center gap-2 font-bold"><User /> COMUNIDAD</div>
          <div className="font-bold">MODO OSCURO</div>
        </div>
      </section>
    </div>
  );
}

export default App;
