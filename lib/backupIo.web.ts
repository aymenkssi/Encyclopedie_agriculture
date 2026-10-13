import * as DocumentPicker from 'expo-document-picker';

/** Web: downloads the backup file. */
export async function shareBackup(json: string, fileName: string): Promise<void> {
  const url = URL.createObjectURL(new Blob([json], { type: 'application/json' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}

export async function pickBackup(): Promise<string | null> {
  const result = await DocumentPicker.getDocumentAsync({ type: 'application/json' });
  if (result.canceled || !result.assets?.length) return null;
  const asset = result.assets[0];
  return asset.file ? asset.file.text() : (await fetch(asset.uri)).text();
}
