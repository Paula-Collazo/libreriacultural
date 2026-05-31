import React from 'react';
import { BrowserRouter as Router, Routes, Route, NavLink, Link, useLocation } from 'react-router-dom';
import Explorar from './pages/Explorar';
import ProfilePage from './pages/ProfilePage';
import DetailsPage from './pages/DetailsPage';
import ContentGrid from './components/ContentGrid';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import Comunidad from './pages/Comunidad';

function AppContent() {
  const location = useLocation();
  const isAuthPage = ['/landing', '/login', '/register'].includes(location.pathname);

  return (
    <>
      <div className="grain"></div>
      
      {!isAuthPage && (
        <header className="flex justify-between items-center px-10 py-6 sticky top-0 z-50 bg-[#fdfaf5]/80 backdrop-blur-xl border-b border-[#ece7da]">
          <Link to="/" className="text-2xl font-black italic tracking-tighter text-[#2d2a26] hover:scale-105 transition-transform">
            <img src="/img/logo.png" alt="TCD Logo" className="h-10 w-auto" />
          </Link>
          
          <nav className="bg-[#f0ece3] p-1.5 rounded-full flex gap-1 border border-[#e6e2d8] shadow-sm">
              <NavLink to="/" className={({ isActive }) => `px-6 py-2.5 rounded-full text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${isActive ? 'bg-[#b8601a] text-white shadow-lg scale-105' : 'text-[#8c8471] hover:text-[#2d2a26]'}`}>Mi perfil</NavLink>
              <NavLink to="/explorar" className={({ isActive }) => `px-6 py-2.5 rounded-full text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${isActive ? 'bg-[#b8601a] text-white shadow-lg scale-105' : 'text-[#8c8471] hover:text-[#2d2a26]'}`}>Explorar</NavLink>
              <NavLink to="/peliculas" className={({ isActive }) => `px-6 py-2.5 rounded-full text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${isActive ? 'bg-[#b8601a] text-white shadow-lg scale-105' : 'text-[#8c8471] hover:text-[#2d2a26]'}`}>Cine</NavLink>
              <NavLink to="/series" className={({ isActive }) => `px-6 py-2.5 rounded-full text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${isActive ? 'bg-[#b8601a] text-white shadow-lg scale-105' : 'text-[#8c8471] hover:text-[#2d2a26]'}`}>TV</NavLink>
              <NavLink to="/libros" className={({ isActive }) => `px-6 py-2.5 rounded-full text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${isActive ? 'bg-[#b8601a] text-white shadow-lg scale-105' : 'text-[#8c8471] hover:text-[#2d2a26]'}`}>Libros</NavLink>
              <NavLink to="/discos" className={({ isActive }) => `px-6 py-2.5 rounded-full text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${isActive ? 'bg-[#b8601a] text-white shadow-lg scale-105' : 'text-[#8c8471] hover:text-[#2d2a26]'}`}>Música</NavLink>
              <NavLink to="/comunidad" className={({ isActive }) => `px-6 py-2.5 rounded-full text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${isActive ? 'bg-[#b8601a] text-white shadow-lg scale-105' : 'text-[#8c8471] hover:text-[#2d2a26]'}`}>Comunidad</NavLink>
          </nav>

          <div className="flex items-center gap-6">
               <button 
                  onClick={() => {
                     if(window.confirm('¿Deseas cerrar sesión?')) {
                          window.location.href = '/landing';
                     }
                  }}
                  className="bg-white border border-[#ece7da] text-[9px] font-black uppercase tracking-widest text-[#8c8471] hover:text-[#b8601a] transition-all px-6 py-2.5 rounded-full shadow-sm">
                  Cerrar Sesión
               </button>
          </div>
        </header>
      )}

      {isAuthPage && (
          <header className="px-12 py-10 flex justify-between items-center bg-transparent">
             <NavLink to="/landing" className="text-3xl font-black italic tracking-tighter text-[#2d2a26] hover:scale-105 transition-transform">
               <img src="/img/logo.png" alt="TCD Logo" className="h-12 w-auto" />
             </NavLink>
             <div className="flex gap-4">
                <NavLink to="/login" className="text-[10px] font-black uppercase tracking-widest text-[#8c8471] py-2 px-4 hover:text-black transition-colors">Entrar</NavLink>
                <NavLink to="/register" className="bg-[#b8601a] text-white text-[10px] font-black uppercase tracking-widest py-2 px-6 rounded-full shadow-lg hover:bg-[#a05015] transition-all">Registrarse</NavLink>
             </div>
          </header>
      )}

      <main className="mx-auto px-10 pt-4 max-w-[1600px]">
        <Routes>
           <Route path="/" element={<ProfilePage />} />
           <Route path="/explorar" element={<Explorar />} />
           <Route path="/peliculas" element={<Explorar filterType="PELICULA" />} />
           <Route path="/series" element={<Explorar filterType="SERIE" />} />
           <Route path="/libros" element={<Explorar filterType="LIBRO" />} />
           <Route path="/discos" element={<Explorar filterType="DISCO" />} />
           <Route path="/comunidad" element={<Comunidad />} />
           <Route path="/details/:source/:type/:id" element={<DetailsPage />} />
           <Route path="/landing" element={<LandingPage />} />
           <Route path="/login" element={<LoginPage />} />
           <Route path="/register" element={<RegisterPage />} />
         </Routes>
      </main>

      <footer className="py-20 text-center opacity-20 text-[10px] uppercase tracking-[0.4em] font-black">
        The Cultured Department • 2026
      </footer>
    </>
  );
}

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;
