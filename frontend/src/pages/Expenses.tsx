import { useState, useEffect } from 'react';
import { expenseApi, categoryApi, Expense, Category, ExpenseRequest } from '../services/api';
import { Plus, Search, Filter, Edit2, Trash2, X, AlertTriangle } from 'lucide-react';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

export default function Expenses() {
    const [expenses, setExpenses] = useState<Expense[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingExpense, setEditingExpense] = useState<Expense | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedCategory, setSelectedCategory] = useState<number | null>(null);

    // Form state
    const [formData, setFormData] = useState<ExpenseRequest>({
        amount: 0,
        categoryId: 0,
        description: '',
        expenseDate: format(new Date(), 'yyyy-MM-dd'),
        paymentMode: 'Cash',
    });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [expensesRes, categoriesRes] = await Promise.all([
                expenseApi.getAll(),
                categoryApi.getAll(),
            ]);
            setExpenses(expensesRes.data.data);
            setCategories(categoriesRes.data.data);
        } catch (error) {
            toast.error('Failed to load expenses');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            if (editingExpense) {
                await expenseApi.update(editingExpense.id, formData);
                toast.success('Expense updated');
            } else {
                await expenseApi.create(formData);
                toast.success('Expense added');
            }
            setShowModal(false);
            resetForm();
            fetchData();
        } catch (error) {
            toast.error('Failed to save expense');
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Are you sure you want to delete this expense?')) return;

        try {
            await expenseApi.delete(id);
            toast.success('Expense deleted');
            fetchData();
        } catch (error) {
            toast.error('Failed to delete expense');
        }
    };

    const handleEdit = (expense: Expense) => {
        setEditingExpense(expense);
        setFormData({
            amount: expense.amount,
            categoryId: expense.categoryId,
            description: expense.description,
            expenseDate: expense.expenseDate,
            paymentMode: expense.paymentMode,
        });
        setShowModal(true);
    };

    const resetForm = () => {
        setEditingExpense(null);
        setFormData({
            amount: 0,
            categoryId: categories[0]?.id || 0,
            description: '',
            expenseDate: format(new Date(), 'yyyy-MM-dd'),
            paymentMode: 'Cash',
        });
    };

    const openNewExpenseModal = () => {
        resetForm();
        if (categories.length > 0) {
            setFormData(prev => ({ ...prev, categoryId: categories[0].id }));
        }
        setShowModal(true);
    };

    const filteredExpenses = expenses.filter((expense) => {
        const matchesSearch = expense.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            expense.categoryName.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesCategory = !selectedCategory || expense.categoryId === selectedCategory;
        return matchesSearch && matchesCategory;
    });

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
                    <h1 className="text-2xl font-bold text-gray-900">Expenses</h1>
                    <p className="text-gray-500">Track and manage your spending</p>
                </div>
                <button onClick={openNewExpenseModal} className="btn-primary flex items-center gap-2">
                    <Plus size={20} />
                    Add Expense
                </button>
            </div>

            {/* Filters */}
            <div className="flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                    <input
                        type="text"
                        placeholder="Search expenses..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="input pl-10"
                    />
                </div>
                <div className="relative">
                    <Filter className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                    <select
                        value={selectedCategory || ''}
                        onChange={(e) => setSelectedCategory(e.target.value ? Number(e.target.value) : null)}
                        className="input pl-10 pr-8 appearance-none min-w-[200px]"
                    >
                        <option value="">All Categories</option>
                        {categories.map((cat) => (
                            <option key={cat.id} value={cat.id}>
                                {cat.icon} {cat.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* Expenses List */}
            <div className="card overflow-hidden p-0">
                {filteredExpenses.length > 0 ? (
                    <div className="divide-y">
                        {filteredExpenses.map((expense) => (
                            <div key={expense.id} className="p-4 hover:bg-gray-50 transition-colors">
                                <div className="flex items-center justify-between">
                                    <div className="flex items-center gap-4">
                                        <div
                                            className="w-12 h-12 rounded-lg flex items-center justify-center text-2xl"
                                            style={{ backgroundColor: expense.categoryColor + '20' }}
                                        >
                                            {expense.categoryIcon}
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-2">
                                                <p className="font-medium text-gray-900">{expense.categoryName}</p>
                                                {expense.isAnomaly && (
                                                    <span className="flex items-center gap-1 text-xs text-orange-600 bg-orange-100 px-2 py-0.5 rounded-full">
                                                        <AlertTriangle size={12} />
                                                        Anomaly
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-sm text-gray-500">
                                                {expense.description || 'No description'} • {expense.paymentMode}
                                            </p>
                                            <p className="text-xs text-gray-400 mt-1">
                                                {format(new Date(expense.expenseDate), 'MMM d, yyyy')}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-4">
                                        <span className="font-semibold text-lg text-gray-900">
                                            -${expense.amount.toFixed(2)}
                                        </span>
                                        <div className="flex items-center gap-2">
                                            <button
                                                onClick={() => handleEdit(expense)}
                                                className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 hover:text-gray-700"
                                            >
                                                <Edit2 size={18} />
                                            </button>
                                            <button
                                                onClick={() => handleDelete(expense.id)}
                                                className="p-2 hover:bg-red-50 rounded-lg text-gray-500 hover:text-red-600"
                                            >
                                                <Trash2 size={18} />
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-12">
                        <p className="text-gray-500">No expenses found</p>
                        <button onClick={openNewExpenseModal} className="text-primary-600 font-medium mt-2">
                            Add your first expense
                        </button>
                    </div>
                )}
            </div>

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-2xl w-full max-w-md">
                        <div className="flex items-center justify-between p-6 border-b">
                            <h2 className="text-xl font-semibold">
                                {editingExpense ? 'Edit Expense' : 'Add Expense'}
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
                                    placeholder="What was this expense for?"
                                />
                            </div>
                            <div>
                                <label className="label">Date</label>
                                <input
                                    type="date"
                                    value={formData.expenseDate}
                                    onChange={(e) => setFormData({ ...formData, expenseDate: e.target.value })}
                                    className="input"
                                    required
                                />
                            </div>
                            <div>
                                <label className="label">Payment Mode</label>
                                <select
                                    value={formData.paymentMode}
                                    onChange={(e) => setFormData({ ...formData, paymentMode: e.target.value })}
                                    className="input"
                                >
                                    <option value="Cash">Cash</option>
                                    <option value="Credit Card">Credit Card</option>
                                    <option value="Debit Card">Debit Card</option>
                                    <option value="UPI">UPI</option>
                                    <option value="Bank Transfer">Bank Transfer</option>
                                </select>
                            </div>
                            <div className="flex gap-3 pt-4">
                                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary flex-1">
                                    Cancel
                                </button>
                                <button type="submit" className="btn-primary flex-1">
                                    {editingExpense ? 'Update' : 'Add'} Expense
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
