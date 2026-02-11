import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add token to requests
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Handle token expiration
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// Auth APIs
export const authApi = {
    login: (email: string, password: string) =>
        api.post('/auth/login', { email, password }),
    register: (name: string, email: string, password: string) =>
        api.post('/auth/register', { name, email, password }),
    getMe: () => api.get('/auth/me'),
};

// Expense APIs
export const expenseApi = {
    getAll: () => api.get('/expenses'),
    getById: (id: number) => api.get(`/expenses/${id}`),
    getByDateRange: (startDate: string, endDate: string) =>
        api.get(`/expenses/range?startDate=${startDate}&endDate=${endDate}`),
    getByCategory: (categoryId: number) =>
        api.get(`/expenses/category/${categoryId}`),
    getAnomalies: () => api.get('/expenses/anomalies'),
    create: (data: ExpenseRequest) => api.post('/expenses', data),
    update: (id: number, data: ExpenseRequest) => api.put(`/expenses/${id}`, data),
    delete: (id: number) => api.delete(`/expenses/${id}`),
};

// Recurring Expense APIs
export const recurringApi = {
    getAll: () => api.get('/recurring-expenses'),
    getActive: () => api.get('/recurring-expenses/active'),
    getUpcoming: (days: number = 7) =>
        api.get(`/recurring-expenses/upcoming?days=${days}`),
    create: (data: RecurringExpenseRequest) => api.post('/recurring-expenses', data),
    update: (id: number, data: RecurringExpenseRequest) =>
        api.put(`/recurring-expenses/${id}`, data),
    deactivate: (id: number) => api.patch(`/recurring-expenses/${id}/deactivate`),
    delete: (id: number) => api.delete(`/recurring-expenses/${id}`),
};

// Category APIs
export const categoryApi = {
    getAll: () => api.get('/categories'),
};

// Dashboard APIs
export const dashboardApi = {
    getSummary: () => api.get('/dashboard/summary'),
    getComparison: () => api.get('/dashboard/comparison'),
    getStats: () => api.get('/dashboard/stats'),
};

// Types
export interface ExpenseRequest {
    amount: number;
    categoryId: number;
    description?: string;
    expenseDate: string;
    paymentMode?: string;
    isRecurring?: boolean;
}

export interface RecurringExpenseRequest {
    amount: number;
    categoryId: number;
    description?: string;
    frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
    startDate: string;
}

export interface Category {
    id: number;
    name: string;
    icon: string;
    color: string;
}

export interface Expense {
    id: number;
    amount: number;
    categoryId: number;
    categoryName: string;
    categoryIcon: string;
    categoryColor: string;
    description: string;
    expenseDate: string;
    paymentMode: string;
    isRecurring: boolean;
    isAnomaly: boolean;
    anomalyScore: number | null;
    anomalyExplanation: string | null;
    createdAt: string;
}

export interface RecurringExpense {
    id: number;
    amount: number;
    categoryId: number;
    categoryName: string;
    categoryIcon: string;
    categoryColor: string;
    description: string;
    frequency: string;
    nextDueDate: string;
    isActive: boolean;
    createdAt: string;
}

export default api;
