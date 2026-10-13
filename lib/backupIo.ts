import * as DocumentPicker from 'expo-document-picker';
import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';

/** Writes the backup to a file and opens the share sheet (save to Drive, send by e-mail…). */
export async function shareBackup(json: string, fileName: string): Promise<void> {
  const file = new File(Paths.cache, fileName);
  if (file.exists) file.delete();
  file.create();
  file.write(json);
  await Sharing.shareAsync(file.uri, { mimeType: 'application/json', dialogTitle: fileName, UTI: 'public.json' });
}

/** Lets the user pick a backup file; null when cancelled. */
export async function pickBackup(): Promise<string | null> {
  const result = await DocumentPicker.getDocumentAsync({ type: ['application/json', 'text/plain', '*/*'], copyToCacheDirectory: true });
  if (result.canceled || !result.assets?.length) return null;
  return new File(result.assets[0].uri).text();
}
