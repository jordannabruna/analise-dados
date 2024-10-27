import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AnalyseData1000 {

    public static void main(String[] args) throws IOException {
        String filePath = "src/main/resources/thefuck-sample-1000.json";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
        String json = new String(Files.readAllBytes(Paths.get(filePath)));
        JsonElement jsonElement = JsonParser.parseString(json);
    
        if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
    
            Map<String, MonthlyData> countByYearMonth = filterByMonth(jsonArray, formatter);
    
            String results = formatResults(countByYearMonth);
    
            System.out.println(results);
    
        } else {
            throw new IllegalArgumentException("O JSON raiz não é um array.");
        }
    }
    
    private static class MonthlyData {
        final long totalUsers;
        final long hireableUsers;
        final long emailUsers;
        final long twitterUsers;

        MonthlyData(long totalUsers, long hireableUsers, long emailUsers, long twitterUsers) {
            this.totalUsers = totalUsers;
            this.hireableUsers = hireableUsers;
            this.emailUsers = emailUsers;
            this.twitterUsers = twitterUsers;
        }

        static MonthlyData increment(MonthlyData data, boolean isHireable, boolean hasEmail, boolean hasTwitter) {
            return new MonthlyData(
                data.totalUsers + 1,
                data.hireableUsers + (isHireable ? 1 : 0),
                data.emailUsers + (hasEmail ? 1 : 0),
                data.twitterUsers + (hasTwitter ? 1 : 0)
            );
        }

        static MonthlyData initial() {
            return new MonthlyData(0, 0, 0, 0);
        }
    }

    private static Map<String, MonthlyData> filterByMonth(JsonArray jsonArray, DateTimeFormatter formatter) {
        return StreamSupport.stream(jsonArray.spliterator(), false)
            .filter(JsonElement::isJsonObject)
            .map(JsonElement::getAsJsonObject)
            .filter(obj -> obj.has("starred_at") && obj.has("user"))
            .collect(Collectors.groupingBy(
                obj -> {
                    String starredAt = obj.get("starred_at").getAsString();
                    LocalDate date = LocalDate.parse(starredAt, formatter);
                    return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                },
                () -> new TreeMap<>(),
                Collectors.reducing(
                    MonthlyData.initial(),
                    obj -> {
                        JsonObject user = obj.getAsJsonObject("user");
                        boolean isHireable = user.has("hireable") && user.get("hireable").isJsonPrimitive() && user.get("hireable").getAsBoolean();
                        boolean hasEmail = user.has("email") && user.get("email").isJsonPrimitive() && !user.get("email").getAsString().isEmpty();
                        boolean hasTwitter = user.has("twitter_username") && user.get("twitter_username").isJsonPrimitive() && !user.get("twitter_username").getAsString().isEmpty();
                        return MonthlyData.increment(MonthlyData.initial(), isHireable, hasEmail, hasTwitter);
                    },
                    (data1, data2) -> new MonthlyData(
                        data1.totalUsers + data2.totalUsers,
                        data1.hireableUsers + data2.hireableUsers,
                        data1.emailUsers + data2.emailUsers,
                        data1.twitterUsers + data2.twitterUsers
                    )
                )
            ));
    }

    private static String formatResults(Map<String, MonthlyData> countByYearMonth) {
        StringBuilder sb = new StringBuilder();
        countByYearMonth.forEach((key, data) -> {
            sb.append(key)
              .append(", ")
              .append(data.totalUsers)
              .append(", ")
              .append(data.hireableUsers)
              .append(", ")
              .append(data.emailUsers)
              .append(", ")
              .append(data.twitterUsers)
              .append("\n");
        });
        return sb.toString();
    }
}