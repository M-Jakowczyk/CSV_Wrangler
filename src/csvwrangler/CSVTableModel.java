package csvwrangler;

import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.Vector;

/**
 * Rozszerzony model tabeli dostosowany do obsługi danych CSV.
 * Klasa dziedziczy po DefaultTableModel i dodaje funkcjonalność specyficzną dla CSV,
 * w tym obsługę różnych separatorów, zarządzanie oryginalnymi danymi i automatyczne
 * rozpoznawanie typów danych w kolumnach.
 *
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
     * Konstruktor domyślny - inicjalizuje pusty model z domyślnymi wartościami:
     * - separator: przecinek (',')
     * - pierwszy wiersz jako nagłówki: true
     */
    public CSVTableModel() {
        super();
        this.hasHeaders = true;
        this.separator = ',';
        this.originalData = new Vector<>();
        this.originalColumnNames = new Vector<>();
    }

    /**
     * Konstruktor z danymi i nagłówkami.
     *
     * @param data dane tabeli jako wektor wektorów obiektów
     * @param columnNames nazwy kolumn jako wektor stringów
     */
    public CSVTableModel(Vector<Vector<Object>> data, Vector<String> columnNames) {
        super(data, columnNames);
        this.hasHeaders = true;
        this.separator = ',';
        this.originalData = new Vector<>(data);
        this.originalColumnNames = new Vector<>(columnNames);
    }

    /**
     * Zwraca klasę danych dla określonej kolumny.
     *
     * @param columnIndex indeks kolumny
     * @return klasa danych w kolumnie (String.class domyślnie)
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnTypes != null && columnTypes.length > columnIndex) {
            return columnTypes[columnIndex];
        }
        return String.class; // Domyślnie traktuj wszystkie kolumny jako String
    }

    /**
     * Ustawia nowy wektor danych i identyfikatorów kolumn.
     * Zachowuje oryginalne dane przy pierwszym ładowaniu.
     *
     * @param dataVector wektor danych (wiersze)
     * @param columnIdentifiers wektor nazw kolumn
     */
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
     * Tworzy głęboką kopię wektora danych.
     *
     * @param source źródłowy wektor danych do skopiowania
     * @return nowa, niezależna kopia wektora danych
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

    /**
     * Analizuje typy danych w kolumnach i aktualizuje tablicę columnTypes.
     * Wykrywa kolumny zawierające wyłącznie wartości liczbowe.
     */
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

    /**
     * Sprawdza czy komórka jest edytowalna.
     *
     * @param row indeks wiersza
     * @param column indeks kolumny
     * @return zawsze true (wszystkie komórki są edytowalne)
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return true; // Wszystkie komórki edytowalne
    }

    /**
     * Ustawia separator używany w plikach CSV.
     *
     * @param separator znak separatora (np. ',', ';', '\t')
     */
    public void setSeparator(char separator) {
        this.separator = separator;
    }

    /**
     * Pobiera aktualny separator.
     *
     * @return znak separatora
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * Sprawdza czy model używa pierwszego wiersza jako nagłówków.
     *
     * @return true jeśli pierwszy wiersz traktowany jest jako nagłówki
     */
    public boolean hasHeaders() {
        return hasHeaders;
    }

    /**
     * Tworzy i zwraca kopię tego obiektu.
     *
     * @return sklonowany obiekt CSVTableModel
     * @throws CloneNotSupportedException jeśli klonowanie nie jest wspierane
     */
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