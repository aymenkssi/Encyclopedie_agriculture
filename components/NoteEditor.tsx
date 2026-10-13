import React, { useState } from 'react';
import { View } from 'react-native';
import { SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { useUserData } from '../contexts/UserDataContext';
import { Note } from '../lib/userData';
import { Body, Button, Field, Sheet, useRow } from './ui';

type Props = { visible: boolean; note: Note | null; plantId?: string | null; cropId?: string | null; onClose: () => void };

export const NoteEditor: React.FC<Props> = (props) => (props.visible ? <NoteForm key={props.note?.id ?? 'new'} {...props} /> : null);

const NoteForm: React.FC<Props> = ({ visible, note, plantId = null, cropId = null, onClose }) => {
  const { t } = useLanguage();
  const { colors } = useTheme();
  const { saveNote, deleteNote } = useUserData();
  const row = useRow();
  const [title, setTitle] = useState(note?.title ?? '');
  const [body, setBody] = useState(note?.body ?? '');
  const [confirm, setConfirm] = useState(false);

  const save = () => {
    if (!body.trim() && !title.trim()) return onClose();
    saveNote(note?.id ?? null, { title, body, plantId: note?.plantId ?? plantId, cropId: note?.cropId ?? cropId });
    onClose();
  };

  return (
    <Sheet visible={visible} onClose={onClose} title={t(note ? 'edit_note' : 'new_note')}>
      {confirm ? (
        <>
          <Body>{t('delete_note_text')}</Body>
          <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.md }]}>
            <Button variant="text" label={t('cancel')} onPress={() => setConfirm(false)} />
            <Button label={t('delete')} color={colors.error} onPress={() => { if (note) deleteNote(note.id); onClose(); }} testID="confirm" />
          </View>
        </>
      ) : (
        <>
          <Field label={t('title')} value={title} onChangeText={setTitle} testID="note-title" />
          <Field label={t('note')} value={body} onChangeText={setBody} multiline style={{ minHeight: 120, textAlignVertical: 'top' }} testID="note-body" />
          <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.sm }]}>
            {note && <Button variant="text" label={t('delete')} color={colors.error} onPress={() => setConfirm(true)} />}
            <Button variant="text" label={t('cancel')} onPress={onClose} />
            <Button label={t('save')} onPress={save} testID="note-save" />
          </View>
        </>
      )}
    </Sheet>
  );
};
