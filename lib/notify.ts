// Web / fallback: local notifications only exist on Android and iOS (see notify.native.ts).
export async function ensureNotificationPermission(): Promise<boolean> {
  return false;
}
export async function scheduleTaskReminder(_title: string, _body: string, _dueDate: string): Promise<string | null> {
  return null;
}
export async function cancelReminder(_id: string | null | undefined): Promise<void> {}
