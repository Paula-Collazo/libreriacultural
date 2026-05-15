import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, NavLink } from 'react-router-dom';
import { motion } from 'framer-motion';

function ProfilePage() {
  return (
    <main className="profile-wrap mx-auto px-4 pt-10" style={{ maxWidth: '1100px' }}>
      {/* NAV DE PESTAÑAS */}
      <nav className="type-nav mb-6 flex justify-center sm:justify-start">
        <NavLink to="/" className={({ isActive }) => isActive ? "active" : ""}>Mi perfil</NavLink>
        <NavLink to="/peliculas">Películas</NavLink>
        <NavLink to="/series">Series</NavLink>
        <NavLink to="/libros">Libros</NavLink>
        <NavLink to="/discos">Discos</NavLink>
      </nav>

      {/* BLOQUE DE PERFIL */}
      <section className="glass-card mb-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <p className="kicker font-bold opacity-50 mb-1">PERFIL ACTIVO</p>
          <h1 className="text-5xl font-bold mb-2">red4444</h1>
          <p className="muted text-sm opacity-70">paulacofi2005@gmail.com</p>
        </div>
        <div className="summary-pill bg-[#fdfaf5] border border-[#ddd6c8] p-6 rounded-2xl text-right min-w-[180px]">
           <span className="text-xs uppercase font-bold opacity-50 block mb-1">Total en biblioteca</span>
           <strong className="text-4xl" style={{fontFamily: 'Space Grotesk'}}>9</strong>
        </div>
      </section>

      {/* SECCIÓN COMUNIDAD */}
      <div className="mb-8">
         <button className="btn-primary inline-block w-auto px-6 py-2 text-sm mb-4">Comunidad / Amigos</button>
         <div className="space-y-1 text-sm opacity-80">
            <p><strong>39</strong> Páginas leídas</p>
            <p><strong>1</strong> Episodios vistos</p>
            <p><strong>9</strong> Novedades (30d)</p>
         </div>
      </div>

      {/* MENSAJE DE ESTADO */}
      <div className="bg-[#f0f9f1] border border-[#d1e7dd] text-[#0f5132] p-4 rounded-xl mb-8 text-center text-sm font-medium">
         Estado de película actualizado
      </div>

      {/* BIBLIOTECA POR TIPO */}
      <section className="mb-10">
        <h2 className="text-xl font-bold mb-4">Biblioteca separada por tipo</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="stat-item border-2 border-dashed border-[#ddd6c8] p-6 rounded-2xl bg-white/40">
            <span className="text-xs font-bold opacity-50 uppercase">Películas</span>
            <strong className="text-2xl block mt-1">4</strong>
          </div>
          <div className="stat-item border-2 border-dashed border-[#ddd6c8] p-6 rounded-2xl bg-white/40">
            <span className="text-xs font-bold opacity-50 uppercase">Series</span>
            <strong className="text-2xl block mt-1">2</strong>
          </div>
          <div className="stat-item border-2 border-dashed border-[#ddd6c8] p-6 rounded-2xl bg-white/40">
            <span className="text-xs font-bold opacity-50 uppercase">Libros</span>
            <strong className="text-2xl block mt-1">2</strong>
          </div>
          <div className="stat-item border-2 border-dashed border-[#ddd6c8] p-6 rounded-2xl bg-white/40">
            <span className="text-xs font-bold opacity-50 uppercase">Discos</span>
            <strong className="text-2xl block mt-1">1</strong>
          </div>
        </div>
      </section>
    </main>
  );
}

function App() {
  return (
    <Router>
      <div className="min-h-screen relative pb-20">
        <div className="grain"></div>
        
        <header className="topbar">
          <Link className="brand text-xl" to="/">TCD<span style={{color: 'var(--accent)'}}>.</span></Link>
          <div className="topbar-actions">
            <div className="theme-switch hidden sm:flex mr-4">
              <label className="text-xs uppercase font-bold opacity-60 mr-2">Tema</label>
              <select className="text-xs bg-white/50 border border-[#ddd6c8] rounded px-2 py-1">
                <option>Rosa Pastel</option>
                <option>Modo Oscuro</option>
              </select>
            </div>
            <button className="ghost-btn">Cerrar sesión</button>
          </div>
        </header>

        <Routes>
          <Route path="/" element={<ProfilePage />} />
          <Route path="/peliculas" element={<div className="p-20 text-center">Próximamente: Películas en React</div>} />
          {/* Añadiremos el resto de rutas aquí */}
        </Routes>

        <footer className="mt-20 py-8 text-center opacity-30 text-xs uppercase tracking-[0.2em]">
          The Cultured Department
        </footer>
      </div>
    </Router>
  );
}

export default App;
