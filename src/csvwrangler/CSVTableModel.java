package csvwrangler;

import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.Vector;

/**
 * Rozszerzony model tabeli dostosowany do obsługi danych CSV
 * @author Mateusz Jakoczyk
 * @version 1.0
 */
public class CSVTableModel extends DefaultTableModel implements Cloneable {
    private boolean hasHeaders;
    private char separator;
    private Vector<Vector<Object>> originalData;
    private Vector<String> originalColumnNames;
    private Vector<Vector<Object>> prevData;
    private Vector<String> prevColumnNames;
    private boolean isInitialLoad = true;


    private Class<?>[] columnTypes;

    /**
     * Konstruktor domyślny - inicjalizuje pusty model
     */
    public CSVTableModel() {
        super();
        this.hasHeaders = true;
        this.separator = ',';
        this.originalData = new Vector<>();
        this.originalColumnNames = new Vector<>();
    }

    /**
     * Konstruktor z danymi i nagłówkami
     * @param data dane tabeli
     * @param columnNames nazwy kolumn
     */
    public CSVTableModel(Vector<Vector<Object>> data, Vector<String> columnNames) {
        super(data, columnNames);
        this.hasHeaders = true;
        this.separator = ',';
        this.originalData = new Vector<>(data);
        this.originalColumnNames = new Vector<>(columnNames);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
//        // Próbuj określić typ danych w kolumnie
//        for (int row = 0; row < getRowCount(); row++) {
//            Object value = getValueAt(row, columnIndex);
//            if (value != null && !value.toString().isEmpty()) {
//                try {
//                    Double.parseDouble(value.toString());
//                    return Double.class;
//                } catch (NumberFormatException e) {
//                    // Nie jest liczbą
//                }
//                return String.class;
//            }
//        }
//        return String.class; // Domyślnie traktuj jako string
        if (columnTypes != null && columnTypes.length > columnIndex) {
            return columnTypes[columnIndex];
        }
        return String.class; // Domyślnie traktuj wszystkie kolumny jako String
    }

    @Override
    public void setDataVector(Vector<? extends Vector> dataVector, Vector<?> columnIdentifiers) {

        // Zachowaj kopię oryginalnych danych TYLKO przy pierwszym ładowaniu
        if (isInitialLoad) {
            this.originalData = deepCopyVector(dataVector);
            this.originalColumnNames = new Vector<>();
            for (Object colName : columnIdentifiers) {
                this.originalColumnNames.add(colName.toString());
            }
            isInitialLoad = false;
        }
        super.setDataVector(dataVector, columnIdentifiers);

        analyzeColumnTypes();
    }

    /**
     * Tworzy głęboką kopię wektora danych
     */
    private Vector<Vector<Object>> deepCopyVector(Vector<? extends Vector> source) {
        Vector<Vector<Object>> copy = new Vector<>();
        for (Vector<?> row : source) {
            Vector<Object> rowCopy = new Vector<>();
            for (Object cell : row) {
                rowCopy.add(cell);
            }
            copy.add(rowCopy);
        }
        return copy;
    }

    private void analyzeColumnTypes() {
        if (getRowCount() == 0 || getColumnCount() == 0) {
            columnTypes = null;
            return;
        }

        columnTypes = new Class<?>[getColumnCount()];
        Arrays.fill(columnTypes, String.class); // Domyślnie String

        for (int col = 0; col < getColumnCount(); col++) {
            boolean allNumbers = true;
            for (int row = 0; row < getRowCount(); row++) {
                Object value = getValueAt(row, col);
                if (value != null && !value.toString().isEmpty()) {
                    try {
                        Double.parseDouble(value.toString());
                    } catch (NumberFormatException e) {
                        allNumbers = false;
                        break;
                    }
                }
            }
            if (allNumbers) {
                columnTypes[col] = String.class;
            }
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true; // Wszystkie komórki edytowalne
    }

    /**
     * Ustawia separator używany w plikach CSV
     * @param separator znak separatora
     */
    public void setSeparator(char separator) {
        this.separator = separator;
    }

    /**
     * Pobiera aktualny separator
     * @return znak separatora
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * Sprawdza czy model używa pierwszego wiersza jako nagłówków
     * @return true jeśli używa nagłówków
     */
    public boolean hasHeaders() {
        return hasHeaders;
    }

    /**
     * Ustawia czy model powinien używać pierwszego wiersza jako nagłówków
     * @param hasHeaders true aby używać nagłówków
     */
    public void setHasHeaders(boolean hasHeaders) {
        this.hasHeaders = hasHeaders;
    }

    /**
     * Przywraca oryginalne dane (przed filtrowaniem)
     */
    public void restoreOriginalData() {
        // Użyj oryginalnych danych, ale nie zmieniaj flagi isInitialLoad
        super.setDataVector(deepCopyVector(originalData), new Vector<>(originalColumnNames));
        analyzeColumnTypes();
    }

    /**
     * Ręczne ustawienie oryginalnych danych (przydatne przy ponownym ładowaniu pliku)
     */
    public void setOriginalData(Vector<Vector<Object>> data, Vector<String> columnNames) {
        this.originalData = deepCopyVector(data);
        this.originalColumnNames = new Vector<>(columnNames);
        this.isInitialLoad = false;
        restoreOriginalData();
    }

    /**
     * Dodaje nową kolumnę do modelu
     * @param columnName nazwa nowej kolumny
     */
    public void addColumn(String columnName) {
        super.addColumn(columnName);
        originalColumnNames.add(columnName);

        // Dodaj puste wartości do istniejących wierszy
        for (Vector<Object> row : originalData) {
            row.add("");
        }
    }

    /**
     * Usuwa kolumnę z modelu
     * @param columnIndex indeks kolumny do usunięcia
     */
    public void removeColumn(int columnIndex) {
        columnIdentifiers.remove(columnIndex);
        for (Vector<Object> row : dataVector) {
            row.remove(columnIndex);
        }
        for (Vector<Object> row : originalData) {
            row.remove(columnIndex);
        }
        originalColumnNames.remove(columnIndex);
        fireTableStructureChanged();
    }

    /**
     * Zwraca oryginalne dane (przed filtrowaniem)
     * @return wektor oryginalnych danych
     */
    public Vector<Vector<Object>> getOriginalData() {
        return originalData;
    }

    /**
     * Zwraca oryginalne nazwy kolumn
     * @return wektor oryginalnych nazw kolumn
     */
    public Vector<String> getOriginalColumnNames() {
        return originalColumnNames;
    }

    /**
     * Wyszukuje kolumnę po nazwie (bez uwzględniania wielkości liter)
     * @param columnName nazwa kolumny do znalezienia
     * @return indeks kolumny lub -1 jeśli nie znaleziono
     */
    public int findColumnCaseInsensitive(String columnName) {
        for (int i = 0; i < getColumnCount(); i++) {
            if (getColumnName(i).equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Zlicza unikalne wartości w kolumnie
     * @param columnIndex indeks kolumny
     * @return mapa wartości i ich liczby wystąpień
     */
    public java.util.Map<Object, Integer> countUniqueValues(int columnIndex) {
        java.util.Map<Object, Integer> counts = new java.util.HashMap<>();
        for (int row = 0; row < getRowCount(); row++) {
            Object value = getValueAt(row, columnIndex);
            counts.put(value, counts.getOrDefault(value, 0) + 1);
        }
        return counts;
    }

    /**
     * Sprawdza czy model zawiera puste komórki
     * @return true jeśli są puste komórki
     */
    public boolean hasEmptyCells() {
        for (int row = 0; row < getRowCount(); row++) {
            for (int col = 0; col < getColumnCount(); col++) {
                Object value = getValueAt(row, col);
                if (value == null || value.toString().trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Eksportuje dane do formatu CSV jako String
     * @return dane w formacie CSV
     */
    public String toCSVString() {
        StringBuilder sb = new StringBuilder();

        // Nagłówki
        if (hasHeaders) {
            for (int i = 0; i < getColumnCount(); i++) {
                sb.append(getColumnName(i));
                if (i < getColumnCount() - 1) {
                    sb.append(separator);
                }
            }
            sb.append("\n");
        }

        // Dane
        for (int row = 0; row < getRowCount(); row++) {
            for (int col = 0; col < getColumnCount(); col++) {
                Object value = getValueAt(row, col);
                sb.append(value != null ? escapeCSV(value.toString()) : "");
                if (col < getColumnCount() - 1) {
                    sb.append(separator);
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Escapuje wartości CSV zgodnie ze standardem
     * @param input wartość do escapowania
     * @return zabezpieczona wartość
     */
    private String escapeCSV(String input) {
        if (input == null) {
            return "";
        }
        if (input.contains("\"") || input.contains("\n") || input.contains(",")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }

    /**
     * Sortuje dane po wybranej kolumnie
     * @param columnIndex indeks kolumny do sortowania
     * @param ascending kierunek sortowania (rosnąco/malejąco)
     */
    public void sort(int columnIndex, boolean ascending) {
        Vector<Vector> data = getDataVector();
        data.sort((row1, row2) -> {
            Object val1 = row1.get(columnIndex);
            Object val2 = row2.get(columnIndex);

            if (val1 == null && val2 == null) return 0;
            if (val1 == null) return ascending ? -1 : 1;
            if (val2 == null) return ascending ? 1 : -1;

            if (val1 instanceof Number && val2 instanceof Number) {
                double num1 = ((Number)val1).doubleValue();
                double num2 = ((Number)val2).doubleValue();
                return ascending ? Double.compare(num1, num2) : Double.compare(num2, num1);
            }

            String str1 = val1.toString();
            String str2 = val2.toString();
            return ascending ? str1.compareTo(str2) : str2.compareTo(str1);
        });
        fireTableDataChanged();
    }

    public Object clone() throws CloneNotSupportedException {
        CSVTableModel clone = (CSVTableModel) super.clone();
        clone.originalData = (Vector<Vector<Object>>) originalData.clone();
        clone.originalColumnNames = (Vector<String>) originalColumnNames.clone();
        if(columnTypes != null) {
            clone.columnTypes = columnTypes.clone();
        }
        return clone;
    }
}