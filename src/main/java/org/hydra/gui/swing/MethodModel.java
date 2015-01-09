package org.hydra.gui.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hydra.renamer.MethodInfo;
import org.hydra.util.Lists;
import org.hydra.util.Lists.Pair;

class MethodModel extends ChangableTableModel {
    private static final long serialVersionUID = -5575263948964070610L;
    private static String[]   title            = new String[] { "Flags", "Return Type", "Name",
            "Parameters", "Exceptions"        };
    private List<MethodInfo>  methods;

    public MethodModel(Set<MethodInfo> info) {
        super(title, info.size());
        this.methods = new ArrayList<MethodInfo>(info);
        Collections.sort(this.methods);
        this.fireTableStructureChanged();
        this.fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        Object v = getValueAt(row, column);
        return column == 2 && !"<init>".equals(v) && !"<clinit>".equals(v);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (super.getChangedValue(rowIndex, columnIndex) != null) {
            return super.getChangedValue(rowIndex, columnIndex);
        }
        String value = "";
        MethodInfo method = this.methods.get(rowIndex);
        switch (columnIndex) {
            case 0:
                value = method.getFlagString();
                break;
            case 1:
                value = method.getReturnType();
                break;
            case 2:
                value = method.getName();
                break;
            case 3:
                value = "(" + Lists.mkString(method.getParameterTypes(), ";") + ")";
                break;
            case 4:
                value = Lists.mkString(method.getExceptions());
                break;
            default:
                value = "";
        }
        return value;
    }

    static class Point {
        private int row, col;

        Point(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getRow() {
            return row;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                Point other = (Point) obj;
                return other.row == row && other.col == col;
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + row + "," + col + ")";
        }

    }

    @Override
    public String getChangeScript() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Point, Lists.Pair<String, String>> entry : changeSet.entrySet()) {
            MethodInfo field = this.methods.get(entry.getKey().getRow());
            Pair<String, String> value = entry.getValue();
            sb.append(String.format("method: %s %s to %s\n", value.getLeft(), field.getDesc(),
                value.getRight()));
        }
        return sb.toString();
    }
}