import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

const LoginPage = () => {
    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        
        try {
            const res = await fetch('http://localhost:8083/api/login', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });
            
            if (res.ok) {
                // For simplicity we just navigate, the backend handles session cookie
                navigate('/');
            } else {
                setError('Usuario o contraseña incorrectos');
            }
        } catch (err) {
            setError('Error de conexión');
        }
    };

    return (
        <div className="min-h-[70vh] flex items-center justify-center">
            <div className="bg-white p-16 rounded-[40px] shadow-2xl border border-[#ece7da] w-full max-w-md">
                <div className="text-center mb-12">
                    <h2 className="text-5xl font-black text-[#2d2a26] uppercase tracking-tighter mb-4 italic">
                        Inicia <span className="text-tcd-orange">sesión</span>.
                    </h2>
                    <p className="text-[#8c8471] text-xs font-black uppercase tracking-widest">Bienvenido de nuevo</p>
                </div>

                {error && (
                    <div className="bg-red-50 text-red-800 p-6 rounded-2xl mb-8 text-[11px] font-black uppercase tracking-widest border border-red-100 flex items-center gap-4">
                        <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-8">
                    <div>
                        <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-3 ml-2">Usuario</label>
                        <input 
                            type="text" 
                            required
                            className="w-full bg-[#fdfaf5] border border-[#ece7da] p-6 rounded-3xl focus:outline-none focus:border-tcd-orange transition-all font-bold"
                            placeholder="tu_usuario"
                            value={formData.username}
                            onChange={e => setFormData({...formData, username: e.target.value})}
                        />
                    </div>
                    <div>
                        <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-3 ml-2">Contraseña</label>
                        <input 
                            type="password" 
                            required
                            className="w-full bg-[#fdfaf5] border border-[#ece7da] p-6 rounded-3xl focus:outline-none focus:border-tcd-orange transition-all font-bold"
                            placeholder="••••••••"
                            value={formData.password}
                            onChange={e => setFormData({...formData, password: e.target.value})}
                        />
                    </div>

                    <button className="w-full bg-[#b8601a] text-white p-6 rounded-3xl font-black uppercase tracking-[0.3em] text-xs shadow-xl hover:bg-[#a05015] hover:scale-[1.02] transition-all duration-300">
                        ENTRAR
                    </button>

                    <div className="text-center mt-10 p-4 border-t border-dashed border-[#ece7da]">
                        <p className="text-[10px] font-bold text-[#8c8471] uppercase tracking-widest">
                            ¿No tienes cuenta? <Link to="/register" className="text-tcd-orange font-black hover:underline ml-2">REGÍSTRATE</Link>
                        </p>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default LoginPage;
