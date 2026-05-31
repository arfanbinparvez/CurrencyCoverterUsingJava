import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import okhttp3.*;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CurrencyConverterApp extends Application {

    private final ComboBox<String> fromCurrency = new ComboBox<>();
    private final ComboBox<String> toCurrency = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final Label resultLabel = new Label();
    private final TextArea historyArea = new TextArea();
    private final List<String> history = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, Double> rates = new HashMap<>();

    private final String[] currencies = {"USD", "EUR", "INR", "GBP", "JPY", "AUD", "CAD"};

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        fromCurrency.getItems().addAll(currencies);
        toCurrency.getItems().addAll(currencies);
        fromCurrency.setValue("USD");
        toCurrency.setValue("INR");

        Button convertButton = new Button("Convert");
        Button swapButton = new Button("Reverse");
        Button refreshButton = new Button("Refresh");
        Button exportButton = new Button("Export History");
        Button clearButton = new Button("Clear History");


        convertButton.setOnAction(e -> convertCurrency());
        swapButton.setOnAction(e -> swapCurrencies());
        refreshButton.setOnAction(e -> fetchRates());
        exportButton.setOnAction(e -> exportHistory());
        clearButton.setOnAction(e -> clearHistory());


        historyArea.setEditable(false);
        historyArea.setPrefHeight(150);

        VBox layout = new VBox(10,
                new Label("Amount:"), amountField,
                new Label("From:"), fromCurrency,
                new Label("To:"), toCurrency,
                new HBox(10, convertButton, swapButton, refreshButton),
                new HBox(10, convertButton, swapButton, refreshButton, clearButton),
                resultLabel,
                exportButton,
                new Label("History:"), historyArea
        );
        layout.setPadding(new Insets(15));

        fetchRates();

        stage.setScene(new Scene(layout, 400, 500));
        stage.setTitle("Currency Converter");
        stage.show();
    }

    private void convertCurrency() {
        String amountText = amountField.getText().trim();
        try {
            double amount = Double.parseDouble(amountText);
            String from = fromCurrency.getValue();
            String to = toCurrency.getValue();

            if (from == null || to == null || !rates.containsKey(from) || !rates.containsKey(to)) {
                resultLabel.setText("Currency selection invalid or rates not available.");
                return;
            }

            double converted = amount * (rates.get(to) / rates.get(from));
            resultLabel.setText(String.format("%.2f %s = %.2f %s", amount, from, converted, to));

            String entry = String.format("%.2f %s -> %.2f %s", amount, from, converted, to);
            history.add(entry);
            historyArea.setText(String.join("\n", history));

        } catch (NumberFormatException e) {
            resultLabel.setText("Invalid amount. Please enter a valid number.");
            System.out.println("Invalid number entered: " + amountText);
        }
    }

    private void swapCurrencies() {
        String temp = fromCurrency.getValue();
        fromCurrency.setValue(toCurrency.getValue());
        toCurrency.setValue(temp);
    }
    private void fetchRates() {
        String url = "https://v6.exchangerate-api.com/v6/c2ec48cc36868b2eafe37f7c/latest/USD";
    
        Request request = new Request.Builder().url(url).build();
    
        client.newCall(request).enqueue(new Callback() {
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        updateResult("Failed to fetch rates.");
                        return;
                    }
    
                    String jsonString = body.string();
                    Map<?, ?> json = mapper.readValue(jsonString, Map.class);
    
                    Object ratesObj = json.get("conversion_rates");
                    if (ratesObj instanceof Map<?, ?>) {
                        Map<?, ?> genericRates = (Map<?, ?>) ratesObj;
                        Map<String, Double> updatedRates = new HashMap<>();
    
                        for (Map.Entry<?, ?> entry : genericRates.entrySet()) {
                            if (entry.getKey() instanceof String && entry.getValue() instanceof Number) {
                                updatedRates.put((String) entry.getKey(), ((Number) entry.getValue()).doubleValue());
                            }
                        }
    
                        rates = updatedRates; // ✅ Set the class variable here
                        updateResult("Exchange rates updated.");
                    } else {
                        updateResult("Invalid rate format.");
                    }
                } catch (Exception e) {
                    updateResult("Error parsing rates.");
                    e.printStackTrace();
                }
            }
    
            public void onFailure(Call call, IOException e) {
                updateResult("Failed to fetch rates.");
            }
        });
    }
    

    private void updateResult(String msg) {
        Platform.runLater(() -> resultLabel.setText(msg));
    }

    private void exportHistory() {
        try (PrintWriter writer = new PrintWriter("conversion_history.txt")) {
            for (String entry : history) {
                writer.println(entry);
            }
            resultLabel.setText("History exported.");
        } catch (Exception e) {
            resultLabel.setText("Export failed.");
        }
    }
    
    private void clearHistory() {
        history.clear();
        historyArea.clear();
        resultLabel.setText("History cleared.");
    }
    
}
