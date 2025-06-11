package csvwrangler;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Kontroler aplikacji CSV Data Wrangler - pośredniczy między widokiem a modelem
 * @author Mateusz Jakoczyk
 * @version 1.0
 */
public class CSVController {
    private CSVWranglerApp view;
    private CSVTableModel tableModel;
    private CSVTableModel prev_tableModel;
    private File currentFile;

    public CSVController(CSVWranglerApp view, CSVTableModel tableModel) {
        this.view = view;
        this.tableModel = tableModel;
        updatePreviousTableModel();
    }

    /**
     * Sprawdza czy plik został zapisany CSV
     * @return bool czy aktualnie jest wczytany plik
     */
    public boolean checkFileSaved(){
        if (currentFile != null) {
            int choice = view.showConfirmDialog("Czy chcesz zapisać aktualny plik CSV?");

            if (choice == JOptionPane.YES_OPTION) {
                saveFile();
                currentFile = null;
                tableModel = null;
                return true;
            }
            else if (choice == JOptionPane.NO_OPTION) {
                tableModel = null;
                currentFile = null;
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return true;
        }
    }

    /**
     * Tworzy nowy plik CSV
     */
    public void newFile() {
        if(checkFileSaved()) {
            tableModel = new CSVTableModel();
            String value = "";
            var columnsName = new ArrayList<String>();
            do {
                value = (String) view.showInputDialog(
                        "Wprowadź tytuł dla kolumny ",
                        "Tytuł dla kolumny " + (columnsName.size()+1),
                        "Kolumna "+ (columnsName.size()+1));
                columnsName.add(value != null ? value : "");
            } while (value != null);

            columnsName.removeLast();
            tableModel.setColumnIdentifiers(columnsName.toArray());

            if (tableModel.getRowCount() <= 0) {
                this.addNewRow();
            }
            updatePreviousTableModel();
            refreshData();
            view.setStatusMessage(" Utworzono nową tabelę | Rekordów: " + tableModel.getRowCount());
        }
    }

    /**
     * Otwiera dialog wyboru pliku i ładuje dane CSV
     */
    public void openFile() {
        File file = view.showFileOpenDialog();
        if (file != null) {
            currentFile = file;
            loadCSV(file);
        }
    }

    /**
     * Ładuje dane z pliku CSV do modelu tabeli
     * @param file plik CSV do wczytania
     */
    private void loadCSV(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String[]> data = new ArrayList<>();
            String line;
            char separator = detectSeparator(file);

            // Odczytaj wszystkie wiersze
            while ((line = br.readLine()) != null) {
                String[] row = line.split(String.valueOf(separator), -1);
                data.add(row);
            }

            if (!data.isEmpty()) {
                tableModel.setSeparator(separator);

                // Jeśli nagłówki są włączone, użyj pierwszego wiersza jako nagłówków
                if (tableModel.hasHeaders()) {
                    tableModel.setColumnIdentifiers(data.getFirst());
                    data.removeFirst();
                } else {
                    // Generuj domyślne nagłówki (Kol1, Kol2, ...)
                    String[] headers = new String[data.get(0).length];
                    for (int i = 0; i < headers.length; i++) {
                        headers[i] = "Kol " + (i + 1);
                    }
                    tableModel.setColumnIdentifiers(headers);
                }

                // Wstaw dane do modelu
                tableModel.setRowCount(0);
                for (String[] row : data) {
                    tableModel.addRow(row);
                }

                updatePreviousTableModel();

                view.setStatusMessage(" Wczytano: " + file.getName() + " | Rekordów: " + tableModel.getRowCount());
                view.updateColumnsList(getColumnNames());
            }

        } catch (IOException e) {
            view.showErrorMessage("Błąd podczas wczytywania pliku: " + e.getMessage());
        }
    }

    /**
     * Zapisuje dane do bieżącego pliku lub wyświetla dialog zapisu
     */
    public void saveFile() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            saveFileAs();
        }
    }

    /**
     * Wyświetla dialog zapisu pliku i zapisuje dane
     */
    public void saveFileAs() {
        String separator = (String) view.showInputDialog("Podaj separator danych: ", "Podaj separator danych", tableModel.getSeparator());
        if (separator != null) {
            tableModel.setSeparator(separator.charAt(0));
        }
        File file = view.showFileSaveDialog();
        if (file != null) {
            currentFile = file;
            saveToFile(file);
        }
    }

    /**
     * Zapisuje dane do określonego pliku
     * @param file plik docelowy
     */
    private void saveToFile(File file) {
        try (PrintWriter writer = new PrintWriter(file)) {
            // Zapisz nagłówki jeśli są widoczne
            if (tableModel.hasHeaders()) {
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.print(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.print(tableModel.getSeparator());
                    }
                }
                writer.println();
            }

            // Zapisz dane
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object value = tableModel.getValueAt(row, col);
                    writer.print(value != null ? value.toString() : "");
                    if (col < tableModel.getColumnCount() - 1) {
                        writer.print(tableModel.getSeparator());
                    }
                }
                writer.println();
            }

            view.setStatusMessage(" Zapisano: " + file.getName());
        } catch (IOException e) {
            view.showErrorMessage("Błąd podczas zapisywania pliku: " + e.getMessage());
        }
    }

    /**
     * Dodaje nowy wiersz do tabeli
     */
    public void addNewRow() {
        if (tableModel.getColumnCount() == 0) {
            view.showErrorMessage("Najpierw wczytaj plik CSV");
            return;
        }

        Object[] rowData = new Object[tableModel.getColumnCount()];

        tableModel.addRow(rowData);
        updatePreviousTableModel();
        view.setStatusMessage(" Dodano nowy wiersz | Rekordów: " + tableModel.getRowCount());
    }

    /**
     * Usuwa zaznaczony wiersz z tabeli
     */
    public void deleteSelectedRow() {
        int[] selectedRow = view.getSelectedRow();
        if (selectedRow.length > 0) {
            Stack<Integer> selectedRowStack = new Stack<>();

            // Dodawanie wszystkich elementów z selectedRow do stosu
            for (int row : selectedRow) {
                selectedRowStack.push(row);
            }

            // Usuwanie elementów z wierzchu stosu
            while (!selectedRowStack.isEmpty()) {
                int row = selectedRowStack.pop();
                tableModel.removeRow(row);
            }
            if (tableModel.getRowCount() <= 0) {
                this.addNewRow();
            }
            view.updateTableModel(tableModel);
            updatePreviousTableModel();
            view.setStatusMessage(" Usunięto wiersze: " + Arrays.toString(Arrays.stream(selectedRow).toArray()) + " | Rekordów: " + tableModel.getRowCount());
        } else {
            view.showErrorMessage("Nie wybrano wiersza do usunięcia");
        }
    }

    /**
     * Filtruje dane w tabeli na podstawie kryteriów
     * @param column kolumna do filtrowania
     * @param operator operator porównania (zawiera, równa się, zaczyna się)
     * @param value wartość do porównania
     */
    public void filterData(String column, String operator, String value) {
        if (column == null || value == null || value.isEmpty()) {
            view.showErrorMessage("Wprowadź wartość do filtrowania");
            return;
        }

        int columnIndex = tableModel.findColumn(column);
        if (columnIndex == -1) {
            view.showErrorMessage("Nie znaleziono kolumny: " + column);
            return;
        }

        List<Integer> rowsToKeep = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object cellValue = tableModel.getValueAt(i, columnIndex);
            String cellStr = cellValue != null ? cellValue.toString() : "";

            boolean matches = switch (operator) {
                case "zawiera" -> cellStr.contains(value);
                case "równa się" -> cellStr.equals(value);
                case "zaczyna się" -> cellStr.startsWith(value);
                default -> false;
            };

            if (matches) {
                rowsToKeep.add(i);
            }
        }

        // Zachowaj tylko pasujące wiersze
        Vector<String> columnNames = new Vector<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            columnNames.add(tableModel.getColumnName(i));
        }

        Vector<Vector<Object>> newData = new Vector<>();
        for (int row : rowsToKeep) {
            Vector<Object> rowData = new Vector<>();
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                rowData.add(tableModel.getValueAt(row, col));
            }
            newData.add(rowData);
        }

        tableModel.setDataVector(newData, columnNames);
        view.setStatusMessage(" Przefiltrowano dane | Pasujących rekordów: " + tableModel.getRowCount());
    }

    /**
     * Uaktualnia stan poprzedni modelu tabeli
     */
    private void updatePreviousTableModel() {
        try {
            prev_tableModel = (CSVTableModel) tableModel.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Czyści wszystkie zastosowane filtry i przywraca oryginalne dane
     */
    public void clearFilters() {
        try {
            tableModel = (CSVTableModel) prev_tableModel.clone();
            view.updateTableModel(tableModel);
            view.updateColumnsList(getColumnNames());
            view.setStatusMessage(" Filtry wyczyszczone | Rekordów: " + tableModel.getRowCount());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ukrywa wybrane kolumny w tabeli
     * @param columnIndices indeksy kolumn do ukrycia
     */
    public void hideColumns(int[] columnIndices) {
        if (columnIndices == null || columnIndices.length == 0) {
            view.showErrorMessage("Nie wybrano kolumn do ukrycia");
            return;
        }

        for (int index : columnIndices) {
            view.getTable().getColumnModel().getColumn(index).setMinWidth(0);
            view.getTable().getColumnModel().getColumn(index).setMaxWidth(0);
            view.getTable().getColumnModel().getColumn(index).setWidth(0);
        }

        view.setStatusMessage(" Ukryto " + columnIndices.length + " kolumn");
    }

    /**
     * Pokazuje wszystkie ukryte kolumny
     */
    public void showAllColumns() {
        JTable table = view.getTable();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setMinWidth(50);
            table.getColumnModel().getColumn(i).setMaxWidth(Integer.MAX_VALUE);
            table.getColumnModel().getColumn(i).setPreferredWidth(100);
        }
        view.setStatusMessage(" Pokazano wszystkie kolumny");
    }

    /**
     * Wykrywa separator w pliku CSV
     * @param file plik do analizy
     * @return wykryty separator
     */
    private char detectSeparator(File file) {
        char[] possibleSeparators = {',', ';', '\t', '|'};
        int[] counts = new int[possibleSeparators.length];

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null && line.length() < 1000) {
                for (int i = 0; i < possibleSeparators.length; i++) {
                    int finalI = i;
                    counts[i] += line.chars().filter(ch -> ch == possibleSeparators[finalI]).count();
                }
            }
        } catch (IOException e) {
            return ','; // Domyślny separator jeśli nie uda się odczytać pliku
        }

        // Znajdź separator z największą liczbą wystąpień
        int maxIndex = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[maxIndex]) {
                maxIndex = i;
            }
        }

        return possibleSeparators[maxIndex];
    }

    /**
     * Pobiera nazwy kolumn z modelu
     * @return tablica nazw kolumn
     */
    private String[] getColumnNames() {
        String[] names = new String[tableModel.getColumnCount()];
        for (int i = 0; i < names.length; i++) {
            names[i] = tableModel.getColumnName(i);
        }
        return names;
    }

    /**
     * Odświeża dane w widoku
     */
    public void refreshData() {
        view.updateTableModel(tableModel);
        view.updateColumnsList(getColumnNames());
    }
}