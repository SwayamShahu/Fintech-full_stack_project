import { useState, useEffect } from 'react';
import { notificationApi, Notification, NotificationStatus, recurringApi } from '../services/api';
import { Bell, CheckCircle, XCircle, Clock, Trash2, Settings } from 'lucide-react';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

export default function Notifications() {
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [loading, setLoading] = useState(true);
    const [showSettings, setShowSettings] = useState(false);
    const [notificationTime, setNotificationTime] = useState({ hour: 9, minute: 0 });

    useEffect(() => {
        fetchNotifications();
        const interval = setInterval(fetchNotifications, 30000); // Refresh every 30 seconds
        return () => clearInterval(interval);
    }, []);

    const fetchNotifications = async () => {
        try {
            const res = await notificationApi.getAll();
            setNotifications(res.data.data);
        } catch (error) {
            toast.error('Failed to load notifications');
        } finally {
            setLoading(false);
        }
    };

    const handleDone = async (notificationId: number, recurringExpenseId: number) => {
        try {
            await notificationApi.takeAction(notificationId, 'DONE');
            await recurringApi.approve(recurringExpenseId, true);
            toast.success('Payment marked as done and added to transactions');
            fetchNotifications();
        } catch (error) {
            toast.error('Failed to process action');
        }
    };

    const handleLeft = async (notificationId: number, recurringExpenseId: number) => {
        try {
            await notificationApi.takeAction(notificationId, 'LEFT');
            await recurringApi.approve(recurringExpenseId, false);
            toast.success('Payment marked as left/skipped for this cycle');
            fetchNotifications();
        } catch (error) {
            toast.error('Failed to process action');
        }
    };

    const handleMarkAsRead = async (notificationId: number) => {
        try {
            await notificationApi.markAsRead(notificationId);
            fetchNotifications();
        } catch (error) {
            toast.error('Failed to mark as read');
        }
    };

    const handleClear = async () => {
        if (!confirm('Clear all old notifications?')) return;
        try {
            await notificationApi.clearAll();
            toast.success('Notifications cleared');
            fetchNotifications();
        } catch (error) {
            toast.error('Failed to clear notifications');
        }
    };

    const handleTriggerCheck = async () => {
        try {
            await notificationApi.triggerCheck();
            toast.success('Notification check triggered (demo mode)');
            setTimeout(fetchNotifications, 500);
        } catch (error) {
            toast.error('Failed to trigger check');
        }
    };

    const handleSaveSettings = async () => {
        try {
            await notificationApi.updateSettings({
                notificationHourOfDay: notificationTime.hour,
                notificationMinute: notificationTime.minute,
            });
            toast.success('Notification settings updated');
            setShowSettings(false);
        } catch (error) {
            toast.error('Failed to save settings');
        }
    };

    const unreadCount = notifications.filter(n => n.status === 'UNREAD').length;
    const activeNotifications = notifications.filter(n => n.status === 'UNREAD' || n.status === 'READ');
    const processedNotifications = notifications.filter(n => n.status !== 'UNREAD' && n.status !== 'READ');

    const getStatusBadge = (status: NotificationStatus) => {
        const badges: Record<NotificationStatus, { bg: string; text: string; icon: React.ReactNode }> = {
            UNREAD: { bg: 'bg-blue-100', text: 'text-blue-700', icon: <Bell size={16} /> },
            READ: { bg: 'bg-gray-100', text: 'text-gray-700', icon: <Bell size={16} /> },
            DONE: { bg: 'bg-green-100', text: 'text-green-700', icon: <CheckCircle size={16} /> },
            LEFT: { bg: 'bg-orange-100', text: 'text-orange-700', icon: <Clock size={16} /> },
            SKIPPED: { bg: 'bg-yellow-100', text: 'text-yellow-700', icon: <XCircle size={16} /> },
            EXPIRED: { bg: 'bg-gray-100', text: 'text-gray-500', icon: <XCircle size={16} /> },
        };
        const badge = badges[status] || badges.READ;
        return (
            <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium ${badge.bg} ${badge.text}`}>
                {badge.icon}
                {status}
            </span>
        );
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Notifications Inbox</h1>
                    <p className="text-gray-500">
                        {unreadCount > 0 ? `${unreadCount} unread notification${unreadCount !== 1 ? 's' : ''}` : 'All caught up!'}
                    </p>
                </div>
                <div className="flex gap-2">
                    <button onClick={handleTriggerCheck} className="btn-secondary flex items-center gap-2 whitespace-nowrap">
                        <Bell size={20} />
                        Trigger Check (Demo)
                    </button>
                    <button onClick={() => setShowSettings(true)} className="btn-secondary flex items-center gap-2">
                        <Settings size={20} />
                    </button>
                </div>
            </div>

            {/* Settings Modal */}
            {showSettings && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-2xl w-full max-w-md p-6">
                        <h2 className="text-xl font-semibold mb-4">Notification Settings</h2>
                        <div className="space-y-4">
                            <div className="flex gap-4">
                                <div className="flex-1">
                                    <label className="label">Hour (0-23)</label>
                                    <input
                                        type="number"
                                        min="0"
                                        max="23"
                                        value={notificationTime.hour}
                                        onChange={(e) => setNotificationTime({ ...notificationTime, hour: parseInt(e.target.value) })}
                                        className="input"
                                    />
                                </div>
                                <div className="flex-1">
                                    <label className="label">Minute (0-59)</label>
                                    <input
                                        type="number"
                                        min="0"
                                        max="59"
                                        value={notificationTime.minute}
                                        onChange={(e) => setNotificationTime({ ...notificationTime, minute: parseInt(e.target.value) })}
                                        className="input"
                                    />
                                </div>
                            </div>
                            <p className="text-sm text-gray-600">
                                Notifications will be triggered at <strong>{String(notificationTime.hour).padStart(2, '0')}:{String(notificationTime.minute).padStart(2, '0')}</strong>
                            </p>
                            <div className="flex gap-3 pt-4">
                                <button onClick={() => setShowSettings(false)} className="btn-secondary flex-1">
                                    Cancel
                                </button>
                                <button onClick={handleSaveSettings} className="btn-primary flex-1">
                                    Save Settings
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Active Notifications */}
            {activeNotifications.length > 0 && (
                <div className="card">
                    <h3 className="font-semibold text-gray-900 mb-4">Pending Actions ({activeNotifications.length})</h3>
                    <div className="space-y-3">
                        {activeNotifications.map((notification) => (
                            <div
                                key={notification.id}
                                className="border rounded-lg p-4 hover:shadow-md transition-shadow"
                            >
                                <div className="flex items-start gap-4">
                                    <div
                                        className="w-12 h-12 rounded-lg flex items-center justify-center text-2xl flex-shrink-0"
                                        style={{ backgroundColor: notification.categoryColor + '20' }}
                                    >
                                        {notification.categoryIcon}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-start justify-between gap-2 mb-2">
                                            <div>
                                                <p className="font-semibold text-gray-900">{notification.title}</p>
                                                <p className="text-sm text-gray-600">{notification.message}</p>
                                            </div>
                                            {getStatusBadge(notification.status)}
                                        </div>
                                        <div className="flex items-center gap-4 text-sm text-gray-500 mb-3">
                                            <span>Due: {format(new Date(notification.dueDate), 'MMM d, yyyy')}</span>
                                            <span>•</span>
                                            <span>${notification.amount.toFixed(2)}</span>
                                            <span>•</span>
                                            <span>{notification.frequency}</span>
                                        </div>
                                        <div className="flex gap-2">
                                            <button
                                                onClick={() => handleDone(notification.id, notification.recurringExpenseId)}
                                                className="flex-1 px-3 py-2 bg-green-100 text-green-700 rounded-lg hover:bg-green-200 font-medium text-sm flex items-center justify-center gap-2"
                                            >
                                                <CheckCircle size={16} />
                                                Payment Done
                                            </button>
                                            <button
                                                onClick={() => handleLeft(notification.id, notification.recurringExpenseId)}
                                                className="flex-1 px-3 py-2 bg-orange-100 text-orange-700 rounded-lg hover:bg-orange-200 font-medium text-sm flex items-center justify-center gap-2"
                                            >
                                                <Clock size={16} />
                                                Left for Later
                                            </button>
                                            {notification.status === 'UNREAD' && (
                                                <button
                                                    onClick={() => handleMarkAsRead(notification.id)}
                                                    className="px-3 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 font-medium text-sm"
                                                    title="Mark as read"
                                                >
                                                    ✓
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Empty State */}
            {activeNotifications.length === 0 && processedNotifications.length === 0 && (
                <div className="card text-center py-12">
                    <Bell size={48} className="mx-auto text-gray-300 mb-4" />
                    <p className="text-gray-500 text-lg">No notifications yet</p>
                    <p className="text-gray-400 text-sm mt-2">Create recurring expenses to receive notifications</p>
                </div>
            )}

            {/* Processed Notifications History */}
            {processedNotifications.length > 0 && (
                <div className="card">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="font-semibold text-gray-900">History ({processedNotifications.length})</h3>
                        <button onClick={handleClear} className="text-red-600 hover:text-red-700 text-sm font-medium flex items-center gap-1">
                            <Trash2 size={16} />
                            Clear
                        </button>
                    </div>
                    <div className="space-y-2 max-h-96 overflow-y-auto">
                        {processedNotifications.map((notification) => (
                            <div key={notification.id} className="flex items-center justify-between py-2 px-3 bg-gray-50 rounded-lg">
                                <div className="flex items-center gap-3 flex-1 min-w-0">
                                    <div className="text-lg flex-shrink-0">{notification.categoryIcon}</div>
                                    <div className="min-w-0">
                                        <p className="text-sm font-medium text-gray-900 truncate">{notification.recurringExpenseDescription}</p>
                                        <p className="text-xs text-gray-500">
                                            {format(new Date(notification.dueDate), 'MMM d')} • ${notification.amount.toFixed(2)}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex-shrink-0">
                                    {getStatusBadge(notification.status)}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
