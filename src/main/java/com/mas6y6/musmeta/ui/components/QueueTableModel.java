package com.mas6y6.musmeta.ui.components;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class QueueTableModel<T> extends AbstractTableModel {

    private final List<T> items = new ArrayList<>();

    private final String[] columnNames;
    private final Function<T, ?>[] getters;
    private final BiConsumer<T, Object>[] setters;

    @SafeVarargs
    public QueueTableModel(
            String[] columnNames,
            Function<T, ?>... getters
    ) {
        this(columnNames, getters, null);
    }

    public QueueTableModel(
            String[] columnNames,
            Function<T, ?>[] getters,
            BiConsumer<T, Object>[] setters
    ) {
        if (columnNames.length != getters.length) {
            throw new IllegalArgumentException(
                    "columnNames and getters must have the same length"
            );
        }

        this.columnNames = columnNames;
        this.getters = getters;
        this.setters = setters;
    }

    public List<T> getItems() {
        return items;
    }

    public void add(T item) {
        items.add(item);

        int row = items.size() - 1;
        fireTableRowsInserted(row, row);
    }

    public void addAll(List<T> newItems) {
        if (newItems.isEmpty()) {
            return;
        }

        int start = items.size();

        items.addAll(newItems);

        fireTableRowsInserted(
                start,
                items.size() - 1
        );
    }

    public T getItem(int row) {
        return items.get(row);
    }

    public void remove(int row) {
        items.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public void clear() {
        items.clear();
        fireTableDataChanged();
    }

    public void moveRow(int from, int to) {
        if (from < 0 || from >= items.size()) {
            return;
        }

        if (to < 0 || to >= items.size()) {
            return;
        }

        if (from == to) {
            return;
        }

        T item = items.remove(from);
        items.add(to, item);

        fireTableRowsUpdated(
                Math.min(from, to),
                Math.max(from, to)
        );
    }

    public void moveRowTo(int from, int queuePosition) {
        if (queuePosition < 1 || queuePosition > items.size()) {
            return;
        }

        moveRow(from, queuePosition - 1);
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length + 1;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "#";
        }

        return columnNames[column - 1];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == 0) {
            return Integer.class;
        }

        if (items.isEmpty()) {
            return Object.class;
        }

        Object value = getters[column - 1].apply(items.get(0));

        return value != null
                ? value.getClass()
                : Object.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        T item = items.get(row);

        if (column == 0) {
            return row + 1;
        }

        return getters[column - 1].apply(item);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == 0) {
            return true;
        }

        return setters != null
                && setters[column - 1] != null;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (row < 0 || row >= items.size()) {
            return;
        }

        if (column == 0) {
            if (value == null) {
                return;
            }

            try {
                int newPosition = Integer.parseInt(
                        value.toString().trim()
                );

                moveRowTo(row, newPosition);

            } catch (NumberFormatException ignored) {
            }

            return;
        }

        if (setters != null && setters[column - 1] != null) {
            setters[column - 1].accept(
                    items.get(row),
                    value
            );

            fireTableCellUpdated(row, column);
        }
    }
}