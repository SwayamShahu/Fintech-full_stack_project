import { useState, useEffect } from 'react';
import { dashboardApi, expenseApi, Expense } from '../services/api';
import {
    TrendingUp,
    TrendingDown,
    DollarSign,
    Receipt,
    AlertTriangle,
    ArrowUpRight,
    ArrowDownRight
} from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from 'recharts';
import toast from 'react-hot-toast';

interface DashboardStats {
    summary: {
        totalSpent: number;
        transactionCount: number;
        averageDaily: number;
        categoryBreakdown: Array<{ name: string; amount: number; percentage: number }>;
        topCategory: string;
        topCategoryAmount: number;
    };
    comparison: {
        currentMonthTotal: number;
        previousMonthTotal: number;
        difference: number;
        percentageChange: number;
        trend: string;
        message: string;
    };
    recentAnomalies: number;
    upcomingRecurring: number;
}

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6', '#F97316'];

export default function Dashboard() {
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [recentExpenses, setRecentExpenses] = useState<Expense[]>([]);
    const [anomalies, setAnomalies] = useState<Expense[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            const [statsRes, expensesRes, anomaliesRes] = await Promise.all([
                dashboardApi.getStats(),
                expenseApi.getAll(),
                expenseApi.getAnomalies(),
            ]);

            setStats(statsRes.data.data);
            setRecentExpenses(expensesRes.data.data.slice(0, 5));
            setAnomalies(anomaliesRes.data.data);
        } catch (error) {
            toast.error('Failed to load dashboard data');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    const summary = stats?.summary;
    const comparison = stats?.comparison;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
                <p className="text-gray-500">Overview of your financial activity</p>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="card">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-500">Total Spent</p>
                            <p className="text-2xl font-bold text-gray-900">
                                ${summary?.totalSpent?.toFixed(2) || '0.00'}
                            </p>
                        </div>
                        <div className="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center">
                            <DollarSign className="text-primary-600" size={24} />
                        </div>
                    </div>
                    <div className="mt-3 flex items-center gap-2">
                        {comparison && comparison.trend === 'UP' ? (
                            <span className="flex items-center text-red-500 text-sm">
                                <ArrowUpRight size={16} />
                                {Math.abs(comparison.percentageChange)}%
                            </span>
                        ) : comparison && comparison.trend === 'DOWN' ? (
                            <span className="flex items-center text-green-500 text-sm">
                                <ArrowDownRight size={16} />
                                {Math.abs(comparison.percentageChange)}%
                            </span>
                        ) : (
                            <span className="text-gray-500 text-sm">No change</span>
                        )}
                        <span className="text-gray-400 text-sm">vs last month</span>
                    </div>
                </div>

                <div className="card">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-500">Transactions</p>
                            <p className="text-2xl font-bold text-gray-900">{summary?.transactionCount || 0}</p>
                        </div>
                        <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                            <Receipt className="text-green-600" size={24} />
                        </div>
                    </div>
                    <p className="mt-3 text-gray-500 text-sm">
                        Avg ${summary?.averageDaily?.toFixed(2) || '0'}/day
                    </p>
                </div>

                <div className="card">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-500">Top Category</p>
                            <p className="text-2xl font-bold text-gray-900">{summary?.topCategory || '-'}</p>
                        </div>
                        <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
                            <TrendingUp className="text-purple-600" size={24} />
                        </div>
                    </div>
                    <p className="mt-3 text-gray-500 text-sm">
                        ${summary?.topCategoryAmount?.toFixed(2) || '0'}
                    </p>
                </div>

                <div className="card">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-500">Anomalies</p>
                            <p className="text-2xl font-bold text-gray-900">{stats?.recentAnomalies || 0}</p>
                        </div>
                        <div className="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center">
                            <AlertTriangle className="text-orange-600" size={24} />
                        </div>
                    </div>
                    <p className="mt-3 text-gray-500 text-sm">Unusual spending detected</p>
                </div>
            </div>

            {/* Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Category Breakdown */}
                <div className="card">
                    <h3 className="font-semibold text-gray-900 mb-4">Spending by Category</h3>
                    {summary?.categoryBreakdown && summary.categoryBreakdown.length > 0 ? (
                        <div className="flex items-center gap-8">
                            <div className="w-48 h-48">
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie
                                            data={summary.categoryBreakdown}
                                            dataKey="amount"
                                            nameKey="name"
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={40}
                                            outerRadius={70}
                                        >
                                            {summary.categoryBreakdown.map((_, index) => (
                                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                            ))}
                                        </Pie>
                                    </PieChart>
                                </ResponsiveContainer>
                            </div>
                            <div className="flex-1 space-y-2">
                                {summary.categoryBreakdown.slice(0, 5).map((cat, index) => (
                                    <div key={cat.name} className="flex items-center justify-between">
                                        <div className="flex items-center gap-2">
                                            <div
                                                className="w-3 h-3 rounded-full"
                                                style={{ backgroundColor: COLORS[index % COLORS.length] }}
                                            />
                                            <span className="text-sm text-gray-600">{cat.name}</span>
                                        </div>
                                        <span className="text-sm font-medium">${cat.amount.toFixed(2)}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <p className="text-gray-500 text-center py-8">No spending data yet</p>
                    )}
                </div>

                {/* Month Comparison */}
                <div className="card">
                    <h3 className="font-semibold text-gray-900 mb-4">Month Comparison</h3>
                    {comparison ? (
                        <div>
                            <ResponsiveContainer width="100%" height={180}>
                                <BarChart
                                    data={[
                                        { name: 'Last Month', amount: comparison.previousMonthTotal },
                                        { name: 'This Month', amount: comparison.currentMonthTotal },
                                    ]}
                                >
                                    <XAxis dataKey="name" axisLine={false} tickLine={false} />
                                    <YAxis axisLine={false} tickLine={false} />
                                    <Tooltip formatter={(value) => `$${Number(value).toFixed(2)}`} />
                                    <Bar dataKey="amount" fill="#3B82F6" radius={[8, 8, 0, 0]} />
                                </BarChart>
                            </ResponsiveContainer>
                            <div className={`mt-4 p-3 rounded-lg ${comparison.trend === 'DOWN' ? 'bg-green-50' : 'bg-red-50'
                                }`}>
                                <div className="flex items-center gap-2">
                                    {comparison.trend === 'DOWN' ? (
                                        <TrendingDown className="text-green-600" size={20} />
                                    ) : (
                                        <TrendingUp className="text-red-600" size={20} />
                                    )}
                                    <span className={comparison.trend === 'DOWN' ? 'text-green-700' : 'text-red-700'}>
                                        {comparison.message}
                                    </span>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-gray-500 text-center py-8">No comparison data</p>
                    )}
                </div>
            </div>

            {/* Recent Expenses & Anomalies */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="card">
                    <h3 className="font-semibold text-gray-900 mb-4">Recent Expenses</h3>
                    {recentExpenses.length > 0 ? (
                        <div className="space-y-3">
                            {recentExpenses.map((expense) => (
                                <div key={expense.id} className="flex items-center justify-between py-2 border-b last:border-0">
                                    <div className="flex items-center gap-3">
                                        <span className="text-2xl">{expense.categoryIcon}</span>
                                        <div>
                                            <p className="font-medium text-gray-900">{expense.categoryName}</p>
                                            <p className="text-sm text-gray-500">{expense.description || expense.expenseDate}</p>
                                        </div>
                                    </div>
                                    <span className="font-semibold text-gray-900">-${expense.amount.toFixed(2)}</span>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="text-gray-500 text-center py-8">No expenses yet. Start tracking!</p>
                    )}
                </div>

                <div className="card">
                    <h3 className="font-semibold text-gray-900 mb-4">Anomaly Alerts</h3>
                    {anomalies.length > 0 ? (
                        <div className="space-y-3">
                            {anomalies.slice(0, 5).map((expense) => (
                                <div key={expense.id} className="flex items-start gap-3 p-3 bg-orange-50 rounded-lg">
                                    <AlertTriangle className="text-orange-500 flex-shrink-0 mt-0.5" size={20} />
                                    <div className="flex-1">
                                        <div className="flex items-center justify-between">
                                            <p className="font-medium text-gray-900">{expense.categoryName}</p>
                                            <span className="font-semibold text-orange-600">${expense.amount.toFixed(2)}</span>
                                        </div>
                                        <p className="text-sm text-gray-600 mt-1">{expense.anomalyExplanation}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-8">
                            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
                                <TrendingDown className="text-green-600" size={32} />
                            </div>
                            <p className="text-gray-600">No unusual spending detected</p>
                            <p className="text-sm text-gray-400">Your spending patterns look normal</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
