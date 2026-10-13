import React, { useState } from 'react';
import { router } from 'expo-router';
import { Fab } from '../components/Fab';
import { NoteEditor } from '../components/NoteEditor';
import { Screen } from '../components/Screen';
import { Body, Button, Card, EmptyState } from '../components/ui';
import { useLanguage } from '../contexts/LanguageContext';
import { useUserData } from '../contexts/UserDataContext';
import { displayName } from '../lib/catalog';
import { catalog } from '../lib/knowledge';
import { alive, Note } from '../lib/userData';

export default function NotesScreen() {
  const { t, language } = useLanguage();
  const { data } = useUserData();
  const [editor, setEditor] = useState<{ note: Note | null } | null>(null);
  const notes = alive(data.notes).sort((a, b) => b.updatedAt - a.updatedAt);
  return (
    <Screen title={t('my_notes')} back fab={<Fab label={t('new_note')} onPress={() => setEditor({ note: null })} testID="add-note" />}>
      {notes.length === 0 ? <EmptyState icon="document-text-outline" title={t('no_notes_title')} message={t('no_notes_text')} /> : notes.map((n) => {
        const plant = n.plantId ? catalog.plantsById[n.plantId] : null;
        return (
          <Card key={n.id} onPress={() => setEditor({ note: n })}>
            <Body bold>{n.title || t('untitled')}</Body>
            <Body small numberOfLines={4}>{n.body}</Body>
            {plant && (
              <Button variant="text" icon="leaf-outline" label={displayName(plant, language)}
                onPress={() => router.push(n.cropId ? { pathname: '/crop/[id]', params: { id: n.cropId } } : { pathname: '/plant/[id]', params: { id: plant.id } })} />
            )}
          </Card>
        );
      })}
      <NoteEditor visible={!!editor} note={editor?.note ?? null} onClose={() => setEditor(null)} />
    </Screen>
  );
}
