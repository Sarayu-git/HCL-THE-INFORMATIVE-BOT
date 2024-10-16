import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {

    private static final String API_KEY = "AIzaSyC7JzxdQK--A3cHDjVp69ZSO80RwPxcFbc"; // Replace with your actual API key
    private static final String KNOWLEDGE_GRAPH_URL = "https://kgsearch.googleapis.com/v1/entities:search";

    public static void main(String[] args) {
        try {
            String query = getUserInput();
            if (query.isEmpty()) {
                System.out.println("Invalid input. Please enter a valid query.");
                return;
            }
            String encodedQuery = encodeQuery(query);
            String apiUrl = buildApiUrl(encodedQuery);
            String jsonResponse = sendGetRequest(apiUrl);
            String htmlContent = parseAndGenerateHtml(jsonResponse);
            writeToFile("output.html", htmlContent);
            System.out.println("Results written to output.html. Please open the file in a web browser.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private static String getUserInput() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter your search query: ");
            return reader.readLine().trim();
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
            return "";
        }
    }

    private static String encodeQuery(String query) throws Exception {
        return URLEncoder.encode(query, "UTF-8");
    }

    private static String buildApiUrl(String encodedQuery) {
        return KNOWLEDGE_GRAPH_URL + "?query=" + encodedQuery + "&key=" + API_KEY + "&limit=5&indent=True";
    }

    private static String sendGetRequest(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    private static String parseAndGenerateHtml(String jsonResponse) {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray itemListElement = jsonObject.getAsJsonArray("itemListElement");
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
                   .append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                   .append("<title>Knowledge Graph Search Results</title>\n")
                   .append("<style>\nbody { font-family: Arial, sans-serif; margin: 20px; }\n")
                   .append(".entity { border-bottom: 1px solid #ccc; padding-bottom: 10px; margin-bottom: 10px; }\n")
                   .append(".entity-name { font-size: 1.5em; font-weight: bold; }\n")
                   .append(".entity-description { color: #555; }\n")
                   .append(".entity-url { color: #0066cc; text-decoration: none; }\n")
                   .append("</style>\n</head>\n<body>\n")
                   .append("<h1>Knowledge Graph Search Results</h1>\n<div id=\"results\">\n");

        if (itemListElement.size() == 0) {
            htmlBuilder.append("<p>No results found for the specified query.</p>\n");
        } else {
            for (JsonElement element : itemListElement) {
                JsonObject result = element.getAsJsonObject().getAsJsonObject("result");
                String name = getEntityName(result);
                String description = getCompleteDescription(result);
                String url = getUrl(result);
                htmlBuilder.append("<div class=\"entity\">\n")
                           .append("<div class=\"entity-name\">" + name + "</div>\n")
                           .append("<div class=\"entity-description\">" + description + "</div>\n")
                           .append("<div><a href=\"" + url + "\" class=\"entity-url\">" + url + "</a></div>\n")
                           .append("</div>\n");
            }
        }

        htmlBuilder.append("</div>\n</body>\n</html>");
        return htmlBuilder.toString();
    }

    private static String getCompleteDescription(JsonObject result) {
        String description = null;

        // Check for description field
        if (result.has("description")) {
            description = result.getAsJsonPrimitive("description").getAsString();
        }

        // Check for detailedDescription field if available
        if (result.has("detailedDescription")) {
            JsonObject detailedDescription = result.getAsJsonObject("detailedDescription");
            if (detailedDescription.has("articleBody")) {
                description = detailedDescription.getAsJsonPrimitive("articleBody").getAsString();
            }
        }

        // Return "No description available" if the description is null or empty
        return (description == null || description.isEmpty()) ? "No description available" : description;
    }

    private static String getEntityName(JsonObject result) {
        return result.has("name") ? result.getAsJsonPrimitive("name").getAsString() : "No name available";
    }

    private static String getUrl(JsonObject result) {
        return result.has("url") ? result.getAsJsonPrimitive("url").getAsString() : "No URL available";
    }

    private static void writeToFile(String fileName, String content) {
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(content);
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    private static void handleError(Exception e) {
        System.err.println("An error occurred: " + e.getMessage());
        e.printStackTrace();
    }
}
