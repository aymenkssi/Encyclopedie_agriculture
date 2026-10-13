import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { parseIso } from './dates';

/**
 * Local reminders. They only concern tasks of the user's own calendar (created by the user or
 * derived from the dates of their crops): no universal agronomic reminder is ever sent.
 */
Notifications.setNotificationHandler({
  handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: false, shouldSetBadge: false }),
});

const CHANNEL = 'tasks';
let channelReady = false;

async function ensureChannel() {
  if (channelReady || Platform.OS !== 'android') return;
  await Notifications.setNotificationChannelAsync(CHANNEL, { name: 'Tasks', importance: Notifications.AndroidImportance.DEFAULT });
  channelReady = true;
}

export async function ensureNotificationPermission(): Promise<boolean> {
  const current = await Notifications.getPermissionsAsync();
  if (current.granted) return true;
  if (!current.canAskAgain) return false;
  return (await Notifications.requestPermissionsAsync()).granted;
}

/** Schedules a reminder at 8:00 on the due date (or in one minute if that time has passed today). */
export async function scheduleTaskReminder(title: string, body: string, dueDate: string): Promise<string | null> {
  try {
    if (!(await ensureNotificationPermission())) return null;
    await ensureChannel();
    const when = parseIso(dueDate);
    when.setHours(8, 0, 0, 0);
    const now = Date.now();
    if (when.getTime() < now) {
      if (new Date(when).toDateString() !== new Date(now).toDateString()) return null; // past day: no reminder
      when.setTime(now + 60_000);
    }
    return await Notifications.scheduleNotificationAsync({
      content: { title, body },
      trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: when, channelId: CHANNEL },
    });
  } catch (error) {
    console.warn('Could not schedule the reminder:', error);
    return null;
  }
}

export async function cancelReminder(id: string | null | undefined): Promise<void> {
  if (!id) return;
  try {
    await Notifications.cancelScheduledNotificationAsync(id);
  } catch (error) {
    console.warn('Could not cancel the reminder:', error);
  }
}
