import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authApi } from '../services/api';

interface User {
    id: number;
    name: string;
    email: string;
}

interface AuthContextType {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    loading: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (name: string, email: string, password: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedToken = localStorage.getItem('token');
        const storedUser = localStorage.getItem('user');

        if (storedToken && storedUser) {
            setToken(storedToken);
            setUser(JSON.parse(storedUser));
        }
        setLoading(false);
    }, []);

    const login = async (email: string, password: string) => {
        const response = await authApi.login(email, password);
        const { data } = response.data;

        localStorage.setItem('token', data.accessToken);
        localStorage.setItem('user', JSON.stringify({
            id: data.userId,
            name: data.name,
            email: data.email,
        }));

        setToken(data.accessToken);
        setUser({
            id: data.userId,
            name: data.name,
            email: data.email,
        });
    };

    const register = async (name: string, email: string, password: string) => {
        const response = await authApi.register(name, email, password);
        const { data } = response.data;

        localStorage.setItem('token', data.accessToken);
        localStorage.setItem('user', JSON.stringify({
            id: data.userId,
            name: data.name,
            email: data.email,
        }));

        setToken(data.accessToken);
        setUser({
            id: data.userId,
            name: data.name,
            email: data.email,
        });
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{
            user,
            token,
            isAuthenticated: !!token,
            loading,
            login,
            register,
            logout,
        }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
