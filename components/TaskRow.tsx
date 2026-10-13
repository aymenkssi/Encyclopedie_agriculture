import React from 'react';
import { Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { FONT, SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { useUserData } from '../contexts/UserDataContext';
import { useLabels } from '../hooks/useLabels';
import { today } from '../lib/dates';
import { Task } from '../lib/userData';
import { Card, useRow } from './ui';

export const TaskRow: React.FC<{ task: Task; onPress?: () => void }> = ({ task, onPress }) => {
  const { colors } = useTheme();
  const { t, tn, align } = useLanguage();
  const { date, label } = useLabels();
  const { setTaskDone } = useUserData();
  const row = useRow();
  const overdue = !task.done && task.dueDate < today();
  const sub = [
    overdue ? t('overdue_since', { a: date(task.dueDate) }) : date(task.dueDate),
    task.origin === 'planned' ? t('estimated') : null,
    task.repeatDays ? tn('every_n_days', task.repeatDays) : null,
  ].filter(Boolean).join(' · ');
  return (
    <Card onPress={onPress} testID={`task-${task.id}`}>
      <View style={[row, { alignItems: 'center', gap: SPACING.md }]}>
        <TouchableOpacity onPress={() => setTaskDone(task.id, !task.done, t('notif_title'))} accessibilityRole="checkbox"
          accessibilityState={{ checked: task.done }} accessibilityLabel={task.title} hitSlop={8}>
          <Ionicons name={task.done ? 'checkbox' : 'square-outline'} size={26} color={colors.primary} />
        </TouchableOpacity>
        <View style={{ flex: 1 }}>
          <Text style={{ color: colors.text, fontSize: FONT.md, fontWeight: '600', textAlign: align, textDecorationLine: task.done ? 'line-through' : 'none' }}
            numberOfLines={2}>{task.title || label('act', task.activity)}</Text>
          <Text style={{ color: overdue ? colors.error : colors.textSecondary, fontSize: FONT.sm, textAlign: align }}>{sub}</Text>
        </View>
        {task.reminder && !task.done && <Ionicons name="notifications-outline" size={18} color={colors.textSecondary} />}
      </View>
    </Card>
  );
};
