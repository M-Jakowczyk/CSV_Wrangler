package csvwrangler;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * Główna klasa aplikacji CSV Data Wrangler - widok w architekturze MVC
 * @author Mateusz Jakoczyk
 * @version 1.0
 */
public class CSVWranglerApp extends JFrame {
    private JTable dataTable;
    private JScrollPane scrollPane;
    private JLabel statusLabel;
    private CSVController controller;
    private CSVTableModel tableModel;
    private JList<String> columnsList;
    private JComboBox<String> filterColumnCombo;

    /**
     * Konstruktor głównego okna aplikacji.
     * Inicjalizuje komponenty interfejsu użytkownika i kontroler.
     */
    public CSVWranglerApp() {
        setTitle("CSV Data Wrangler");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                if (controller.checkFileSaved())
                    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                else
                    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            }
        });

        setSize(1000, 700);
        setLocationRelativeTo(null);

        tableModel = new CSVTableModel();
        controller = new CSVController(this, tableModel);

        initUI();
    }

    /**
     * Inicjalizuje interfejs użytkownika aplikacji.
     * Tworzy i konfiguruje wszystkie komponenty GUI.
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        setJMenuBar(createMenuBar());
        mainPanel.add(createToolbar(), BorderLayout.NORTH);

        dataTable = new JTable(tableModel);
        scrollPane = new JScrollPane(dataTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel(" Gotowy");
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        mainPanel.add(createSidePanel(), BorderLayout.EAST);

        add(mainPanel);
    }

    /**
     * Tworzy pasek menu aplikacji.
     *
     * @return JMenuBar z skonfigurowanymi menu i pozycjami
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Plik
        JMenu fileMenu = new JMenu("Plik");
        JMenuItem newItem = new JMenuItem("Nowy plik CSV");
        newItem.addActionListener(e -> controller.newFile());
        JMenuItem openItem = new JMenuItem("Otwórz CSV");
        openItem.addActionListener(e -> controller.openFile());
        JMenuItem saveItem = new JMenuItem("Zapisz CSV");
        saveItem.addActionListener(e -> controller.saveFile());
        JMenuItem saveAsItem = new JMenuItem("Zapisz CSV jako");
        saveAsItem.addActionListener(e -> controller.saveFileAs());
        JMenuItem exitItem = new JMenuItem("Wyjdź");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Menu Edycja
        JMenu editMenu = new JMenu("Edycja");
        JMenuItem addRowItem = new JMenuItem("Dodaj wiersz");
        addRowItem.addActionListener(e -> controller.addNewRow());
        JMenuItem deleteRowItem = new JMenuItem("Usuń wiersz");
        deleteRowItem.addActionListener(e -> controller.deleteSelectedRow());

        editMenu.add(addRowItem);
        editMenu.add(deleteRowItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        return menuBar;
    }

    /**
     * Tworzy pasek narzędziowy aplikacji.
     *
     * @return JToolBar z przyciskami szybkiego dostępu
     */
    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Przycisk Otwórz
        JButton newButton = new JButton(new ImageIcon((new ImageIcon("resources/icons/new.png")).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH)));
        newButton.setPreferredSize(new Dimension(25, 25));
        newButton.setToolTipText("Nowy plik CSV");
        newButton.addActionListener(e -> controller.newFile());

        // Przycisk Otwórz
        JButton openButton = new JButton(new ImageIcon((new ImageIcon("resources/icons/open.png")).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH)));
        openButton.setPreferredSize(new Dimension(25, 25));
        openButton.setToolTipText("Otwórz plik CSV");
        openButton.addActionListener(e -> controller.openFile());

        // Przycisk Zapisz
        JButton saveButton = new JButton(new ImageIcon((new ImageIcon("resources/icons/save.png")).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH)));
        saveButton.setToolTipText("Zapisz plik CSV");
        saveButton.addActionListener(e -> controller.saveFile());

        // Przycisk Zapisz Jako
        JButton saveAsButton = new JButton(new ImageIcon((new ImageIcon("resources/icons/save-as.png")).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH)));
        saveAsButton.setToolTipText("Zapisz plik CSV jako...");
        saveAsButton.addActionListener(e -> controller.saveFileAs());

        // Przycisk Dodaj wiersz
        JButton addButton = new JButton(new ImageIcon((new ImageIcon("resources/icons/add.png")).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH)));
        addButton.setToolTipText("Dodaj nowy wiersz");
        addButton.addActionListener(e -> controller.addNewRow());

        // Przycisk Usuń wiersz
        JButton deleteButton = new JButton(new ImageIcon((new ImageIcon("resources/icons/delete.png")).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH)));
        deleteButton.setToolTipText("Usuń zaznaczony wiersz");
        deleteButton.addActionListener(e -> controller.deleteSelectedRow());

        toolbar.add(newButton);
        toolbar.add(openButton);
        toolbar.add(saveButton);
        toolbar.add(saveAsButton);
        toolbar.addSeparator();
        toolbar.add(addButton);
        toolbar.add(deleteButton);

        return toolbar;
    }

    /**
     * Tworzy panel boczny z narzędziami do filtrowania i zarządzania kolumnami.
     *
     * @return JPanel z komponentami do filtrowania i zarządzania kolumnami
     */
    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Panel filtrowania
        JPanel filterPanel = new JPanel(new GridLayout(0, 1));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtrowanie"));

        filterColumnCombo = new JComboBox<>();
        JComboBox<String> filterOperatorCombo = new JComboBox<>(new String[]{"zawiera", "równa się", "zaczyna się"});
        JTextField filterValueField = new JTextField();
        JButton filterButton = new JButton("Filtruj");

        filterButton.addActionListener(e -> {
            String column = (String) filterColumnCombo.getSelectedItem();
            String operator = (String) filterOperatorCombo.getSelectedItem();
            String value = filterValueField.getText();
            controller.filterData(column, operator, value);
        });

        JButton clearFilterButton = new JButton("Wyczyść filtry");
        clearFilterButton.addActionListener(e -> {
            controller.clearFilters(); // Wywołanie metody czyszczącej filtry
            filterValueField.setText(""); // Wyczyszczenie pola wartości
        });

        filterPanel.add(new JLabel("Kolumna:"));
        filterPanel.add(filterColumnCombo);
        filterPanel.add(new JLabel("Operator:"));
        filterPanel.add(filterOperatorCombo);
        filterPanel.add(new JLabel("Wartość:"));
        filterPanel.add(filterValueField);
        filterPanel.add(filterButton);
        filterPanel.add(clearFilterButton);

        // Panel zarządzania kolumnami
        JPanel columnsPanel = new JPanel(new BorderLayout());
        columnsPanel.setBorder(BorderFactory.createTitledBorder("Kolumny"));

        columnsList = new JList<>();
        columnsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane columnsScroll = new JScrollPane(columnsList);

        JButton hideColumnsButton = new JButton("Ukryj zaznaczone");
        hideColumnsButton.addActionListener(e -> {
            int[] selectedIndices = columnsList.getSelectedIndices();
            controller.hideColumns(selectedIndices);
        });

        JButton showAllButton = new JButton("Pokaż wszystkie");
        showAllButton.addActionListener(e -> controller.showAllColumns());

        JPanel columnsButtonPanel = new JPanel(new GridLayout(1, 2));
        columnsButtonPanel.add(hideColumnsButton);
        columnsButtonPanel.add(showAllButton);

        columnsPanel.add(columnsScroll, BorderLayout.CENTER);
        columnsPanel.add(columnsButtonPanel, BorderLayout.SOUTH);

        sidePanel.add(filterPanel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(columnsPanel);

        return sidePanel;
    }

    /**
     * Zwraca referencję do tabeli z danymi.
     *
     * @return JTable z danymi CSV
     */
    public JTable getTable() {
        return dataTable;
    }

    /**
     * Zwraca indeksy zaznaczonych wierszy w tabeli.
     *
     * @return tablica indeksów zaznaczonych wierszy
     */
    public int[] getSelectedRow() {
        return  dataTable.getSelectedRows();
    }

    /**
     * Aktualizuje model danych tabeli.
     *
     * @param model nowy model danych
     */
    public void updateTableModel(TableModel model) {
        dataTable.setModel(model);
    }

    /**
     * Ustawia komunikat w pasku statusu.
     *
     * @param message tekst komunikatu
     */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Aktualizuje listę kolumn w panelu bocznym.
     *
     * @param columns tablica nazw kolumn
     */
    public void updateColumnsList(String[] columns) {
        columnsList.setListData(columns);
        filterColumnCombo.setModel(new DefaultComboBoxModel<>(columns));
    }

    /**
     * Wyświetla dialog otwierania pliku.
     *
     * @return wybrany plik lub null jeśli anulowano
     */
    public File showFileOpenDialog() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
    }

    /**
     * Wyświetla dialog zapisywania pliku.
     *
     * @return wybrany plik lub null jeśli anulowano
     */
    public File showFileSaveDialog() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
    }

    /**
     * Wyświetla dialog wprowadzania danych.
     *
     * @param message komunikat do wyświetlenia
     * @return wprowadzony tekst lub null jeśli anulowano
     */
    public String showInputDialog(String message) {
        return JOptionPane.showInputDialog(this, message);
    }

    /**
     * Wyświetla dialog potwierdzenia.
     *
     * @param message komunikat do wyświetlenia
     * @return wybrana opcja (YES_NO_CANCEL_OPTION)
     */
    public int showConfirmDialog(String message) {
        return JOptionPane.showConfirmDialog(this, message, this.getTitle(), JOptionPane.YES_NO_CANCEL_OPTION);
    }

    /**
     * Wyświetla dialog wprowadzania danych z dodatkowymi opcjami.
     *
     * @param message komunikat do wyświetlenia
     * @param title tytuł okna dialogowego
     * @param option domyślna opcja
     * @return wprowadzone dane lub null jeśli anulowano
     */
    public Object showInputDialog(String message, String title, Object option) {
        return JOptionPane.showInputDialog(
                this,
                message,
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,     //no custom icon
                null,  //button titles
                option //default button
        );
    }

    /**
     * Wyświetla komunikat o błędzie.
     *
     * @param message treść komunikatu błędu
     */
    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Błąd", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Punkt wejścia aplikacji.
     *
     * @param args argumenty wiersza poleceń
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CSVWranglerApp app = new CSVWranglerApp();
            app.setVisible(true);
        });
    }
}