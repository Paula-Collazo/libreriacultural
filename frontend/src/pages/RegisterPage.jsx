import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

const RegisterPage = () => {
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        passwordConfirm: ''
    });
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        
        try {
            const res = await fetch('http://localhost:8083/api/register', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });
            
            if (res.ok) {
                navigate('/');
            } else {
                const msg = await res.text();
                setError(msg);
            }
        } catch (err) {
            setError('Error de conexión con el servidor');
        }
    };

    return (
        <div className="min-h-[80vh] flex items-center justify-center">
            <div className="bg-white p-16 rounded-[40px] shadow-2xl border border-[#ece7da] w-full max-w-xl">
                <div className="text-center mb-12">
                    <h2 className="text-5xl font-black text-[#2d2a26] uppercase tracking-tighter mb-4 italic">
                        Crea tu <span className="text-tcd-orange">cuenta</span>.
                    </h2>
                    <p className="text-[#8c8471] text-xs font-black uppercase tracking-widest">Únete al departamento cultural</p>
                </div>

                {error && (
                    <div className="bg-red-50 text-red-800 p-6 rounded-2xl mb-8 text-[11px] font-black uppercase tracking-widest border border-red-100 flex items-center gap-4">
                        <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-3 ml-2">Username</label>
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
                        <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-3 ml-2">Email</label>
                        <input 
                            type="email" 
                            required
                            className="w-full bg-[#fdfaf5] border border-[#ece7da] p-6 rounded-3xl focus:outline-none focus:border-tcd-orange transition-all font-bold"
                            placeholder="nombre@ejemplo.com"
                            value={formData.email}
                            onChange={e => setFormData({...formData, email: e.target.value})}
                        />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
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
                        <div>
                            <label className="block text-[10px] font-black uppercase tracking-[0.2em] text-[#8c8471] mb-3 ml-2">Confirmar</label>
                            <input 
                                type="password" 
                                required
                                className="w-full bg-[#fdfaf5] border border-[#ece7da] p-6 rounded-3xl focus:outline-none focus:border-tcd-orange transition-all font-bold"
                                placeholder="••••••••"
                                value={formData.passwordConfirm}
                                onChange={e => setFormData({...formData, passwordConfirm: e.target.value})}
                            />
                        </div>
                    </div>

                    <button className="w-full bg-[#2d2a26] text-white p-6 rounded-3xl font-black uppercase tracking-[0.3em] text-xs shadow-xl hover:bg-black hover:scale-[1.02] transition-all duration-300 mt-6">
                        REGISTRARSE
                    </button>

                    <div className="text-center mt-10">
                        <p className="text-[10px] font-bold text-[#8c8471] uppercase tracking-widest">
                            ¿Ya tienes cuenta? <Link to="/login" className="text-tcd-orange font-black hover:underline ml-2">IDENTIFÍCATE</Link>
                        </p>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default RegisterPage;
