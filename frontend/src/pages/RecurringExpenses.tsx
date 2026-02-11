import { useState, useEffect } from 'react';
import { recurringApi, categoryApi, RecurringExpense, Category, RecurringExpenseRequest } from '../services/api';
import { Plus, RefreshCw, Edit2, Trash2, X, Calendar, Pause } from 'lucide-react';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

const FREQUENCIES = [
    { value: 'DAILY', label: 'Daily' },
    { value: 'WEEKLY', label: 'Weekly' },
    { value: 'MONTHLY', label: 'Monthly' },
    { value: 'YEARLY', label: 'Yearly' },
];

export default function RecurringExpenses() {
    const [expenses, setExpenses] = useState<RecurringExpense[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingExpense, setEditingExpense] = useState<RecurringExpense | null>(null);

    const [formData, setFormData] = useState<RecurringExpenseRequest>({
        amount: 0,
        categoryId: 0,
        description: '',
        frequency: 'MONTHLY',
        startDate: format(new Date(), 'yyyy-MM-dd'),
    });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [expensesRes, categoriesRes] = await Promise.all([
                recurringApi.getAll(),
                categoryApi.getAll(),
            ]);
            setExpenses(expensesRes.data.data);
            setCategories(categoriesRes.data.data);
        } catch (error) {
            toast.error('Failed to load recurring expenses');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            if (editingExpense) {
                await recurringApi.update(editingExpense.id, formData);
                toast.success('Recurring expense updated');
            } else {
                await recurringApi.create(formData);
                toast.success('Recurring expense added');
            }
            setShowModal(false);
            resetForm();
            fetchData();
        } catch (error) {
            toast.error('Failed to save recurring expense');
        }
    };

    const handleDeactivate = async (id: number) => {
        try {
            await recurringApi.deactivate(id);
            toast.success('Recurring expense paused');
            fetchData();
        } catch (error) {
            toast.error('Failed to pause recurring expense');
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Are you sure you want to delete this recurring expense?')) return;

        try {
            await recurringApi.delete(id);
            toast.success('Recurring expense deleted');
            fetchData();
        } catch (error) {
            toast.error('Failed to delete recurring expense');
        }
    };

    const handleEdit = (expense: RecurringExpense) => {
        setEditingExpense(expense);
        setFormData({
            amount: expense.amount,
            categoryId: expense.categoryId,
            description: expense.description,
            frequency: expense.frequency as RecurringExpenseRequest['frequency'],
            startDate: expense.nextDueDate,
        });
        setShowModal(true);
    };

    const resetForm = () => {
        setEditingExpense(null);
        setFormData({
            amount: 0,
            categoryId: categories[0]?.id || 0,
            description: '',
            frequency: 'MONTHLY',
            startDate: format(new Date(), 'yyyy-MM-dd'),
        });
    };

    const openNewModal = () => {
        resetForm();
        if (categories.length > 0) {
            setFormData(prev => ({ ...prev, categoryId: categories[0].id }));
        }
        setShowModal(true);
    };

    const getDaysUntilDue = (dateStr: string) => {
        const dueDate = new Date(dateStr);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        dueDate.setHours(0, 0, 0, 0);
        const diffTime = dueDate.getTime() - today.getTime();
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        return diffDays;
    };

    const activeExpenses = expenses.filter(e => e.isActive);
    const inactiveExpenses = expenses.filter(e => !e.isActive);

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Recurring Expenses</h1>
                    <p className="text-gray-500">Manage your subscriptions and regular payments</p>
                </div>
                <button onClick={openNewModal} className="btn-primary flex items-center gap-2">
                    <Plus size={20} />
                    Add Recurring
                </button>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="card">
                    <p className="text-sm text-gray-500">Active Subscriptions</p>
                    <p className="text-2xl font-bold text-gray-900">{activeExpenses.length}</p>
                </div>
                <div className="card">
                    <p className="text-sm text-gray-500">Monthly Total</p>
                    <p className="text-2xl font-bold text-gray-900">
                        ${activeExpenses
                            .filter(e => e.frequency === 'MONTHLY')
                            .reduce((sum, e) => sum + e.amount, 0)
                            .toFixed(2)}
                    </p>
                </div>
                <div className="card">
                    <p className="text-sm text-gray-500">Due This Week</p>
                    <p className="text-2xl font-bold text-gray-900">
                        {activeExpenses.filter(e => getDaysUntilDue(e.nextDueDate) <= 7 && getDaysUntilDue(e.nextDueDate) >= 0).length}
                    </p>
                </div>
            </div>

            {/* Active Recurring Expenses */}
            <div className="card">
                <h3 className="font-semibold text-gray-900 mb-4">Active Recurring Expenses</h3>
                {activeExpenses.length > 0 ? (
                    <div className="divide-y -mx-6">
                        {activeExpenses.map((expense) => {
                            const daysUntil = getDaysUntilDue(expense.nextDueDate);
                            return (
                                <div key={expense.id} className="px-6 py-4 hover:bg-gray-50 transition-colors">
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center gap-4">
                                            <div
                                                className="w-12 h-12 rounded-lg flex items-center justify-center text-2xl"
                                                style={{ backgroundColor: expense.categoryColor + '20' }}
                                            >
                                                {expense.categoryIcon}
                                            </div>
                                            <div>
                                                <p className="font-medium text-gray-900">{expense.description || expense.categoryName}</p>
                                                <div className="flex items-center gap-2 text-sm text-gray-500">
                                                    <RefreshCw size={14} />
                                                    <span>{expense.frequency}</span>
                                                    <span>•</span>
                                                    <Calendar size={14} />
                                                    <span>
                                                        {daysUntil === 0
                                                            ? 'Due today'
                                                            : daysUntil < 0
                                                                ? `${Math.abs(daysUntil)} days overdue`
                                                                : `Due in ${daysUntil} days`}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <div className="text-right">
                                                <span className="font-semibold text-lg text-gray-900">
                                                    ${expense.amount.toFixed(2)}
                                                </span>
                                                <p className="text-xs text-gray-500">
                                                    {format(new Date(expense.nextDueDate), 'MMM d, yyyy')}
                                                </p>
                                            </div>
                                            <div className="flex items-center gap-1">
                                                <button
                                                    onClick={() => handleEdit(expense)}
                                                    className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 hover:text-gray-700"
                                                    title="Edit"
                                                >
                                                    <Edit2 size={18} />
                                                </button>
                                                <button
                                                    onClick={() => handleDeactivate(expense.id)}
                                                    className="p-2 hover:bg-yellow-50 rounded-lg text-gray-500 hover:text-yellow-600"
                                                    title="Pause"
                                                >
                                                    <Pause size={18} />
                                                </button>
                                                <button
                                                    onClick={() => handleDelete(expense.id)}
                                                    className="p-2 hover:bg-red-50 rounded-lg text-gray-500 hover:text-red-600"
                                                    title="Delete"
                                                >
                                                    <Trash2 size={18} />
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                ) : (
                    <div className="text-center py-8">
                        <p className="text-gray-500">No active recurring expenses</p>
                        <button onClick={openNewModal} className="text-primary-600 font-medium mt-2">
                            Add your first subscription
                        </button>
                    </div>
                )}
            </div>

            {/* Inactive Recurring Expenses */}
            {inactiveExpenses.length > 0 && (
                <div className="card opacity-60">
                    <h3 className="font-semibold text-gray-900 mb-4">Paused Recurring Expenses</h3>
                    <div className="divide-y -mx-6">
                        {inactiveExpenses.map((expense) => (
                            <div key={expense.id} className="px-6 py-4">
                                <div className="flex items-center justify-between">
                                    <div className="flex items-center gap-4">
                                        <div
                                            className="w-10 h-10 rounded-lg flex items-center justify-center text-xl opacity-50"
                                            style={{ backgroundColor: expense.categoryColor + '20' }}
                                        >
                                            {expense.categoryIcon}
                                        </div>
                                        <div>
                                            <p className="font-medium text-gray-600">{expense.description || expense.categoryName}</p>
                                            <p className="text-sm text-gray-400">{expense.frequency} • Paused</p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-4">
                                        <span className="font-semibold text-gray-600">${expense.amount.toFixed(2)}</span>
                                        <button
                                            onClick={() => handleDelete(expense.id)}
                                            className="p-2 hover:bg-red-50 rounded-lg text-gray-400 hover:text-red-600"
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-2xl w-full max-w-md">
                        <div className="flex items-center justify-between p-6 border-b">
                            <h2 className="text-xl font-semibold">
                                {editingExpense ? 'Edit Recurring Expense' : 'Add Recurring Expense'}
                            </h2>
                            <button onClick={() => setShowModal(false)} className="p-2 hover:bg-gray-100 rounded-lg">
                                <X size={20} />
                            </button>
                        </div>
                        <form onSubmit={handleSubmit} className="p-6 space-y-4">
                            <div>
                                <label className="label">Amount</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    value={formData.amount || ''}
                                    onChange={(e) => setFormData({ ...formData, amount: parseFloat(e.target.value) || 0 })}
                                    className="input"
                                    placeholder="0.00"
                                    required
                                />
                            </div>
                            <div>
                                <label className="label">Category</label>
                                <select
                                    value={formData.categoryId}
                                    onChange={(e) => setFormData({ ...formData, categoryId: Number(e.target.value) })}
                                    className="input"
                                    required
                                >
                                    {categories.map((cat) => (
                                        <option key={cat.id} value={cat.id}>
                                            {cat.icon} {cat.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="label">Description</label>
                                <input
                                    type="text"
                                    value={formData.description}
                                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                    className="input"
                                    placeholder="Netflix, Spotify, etc."
                                />
                            </div>
                            <div>
                                <label className="label">Frequency</label>
                                <select
                                    value={formData.frequency}
                                    onChange={(e) => setFormData({ ...formData, frequency: e.target.value as RecurringExpenseRequest['frequency'] })}
                                    className="input"
                                >
                                    {FREQUENCIES.map((freq) => (
                                        <option key={freq.value} value={freq.value}>
                                            {freq.label}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="label">Next Due Date</label>
                                <input
                                    type="date"
                                    value={formData.startDate}
                                    onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                                    className="input"
                                    required
                                />
                            </div>
                            <div className="flex gap-3 pt-4">
                                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary flex-1">
                                    Cancel
                                </button>
                                <button type="submit" className="btn-primary flex-1">
                                    {editingExpense ? 'Update' : 'Add'} Recurring
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
