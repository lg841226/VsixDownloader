import org.fusesource.jansi.Ansi;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

public class VsixDownLoader {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            String userUrl = getUserInputUrl(scanner);
            if (userUrl == null) {
                return;
            }

            String[] extractedInfo = extractPublisherAndExtension(userUrl);
            if (extractedInfo == null) {
                return;
            }
            String publisher = extractedInfo[0];
            String extensionName = extractedInfo[1];

            if (validateUrl(userUrl)) {
                String userVersion = getExtensionVersion(scanner);
                if (userVersion != null) {
                    generateDownloadLink(publisher, extensionName, userVersion);
                }
            }
        }
    }

    private static String getUserInputUrl(Scanner scanner) {
        System.out.print(Ansi.ansi().fgBlue().bold().a("\n输入 Vsix URL → ").reset());
        String userUrl = scanner.nextLine();
        if (userUrl == null || userUrl.isEmpty()) {
            System.out.println(Ansi.ansi().fgRed().bold().a("\n<输入 URL>").reset());
            return null;
        }
        return userUrl;
    }

    private static String[] extractPublisherAndExtension(String userUrl) {
        try {
            URI uri = new URI(userUrl);
            String query = uri.getQuery();
            if (query == null || !query.contains("itemName=")) {
                return null;
            }
            String itemName = query.split("itemName=")[1];
            String[] parts = itemName.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            return new String[]{parts[0], parts[1]};
        } catch (Exception e) {
            System.out.println(Ansi.ansi().fgRed().bold().a("\n<验证 URL 失败>").reset());
            return null;
        }
    }

    private static boolean validateUrl(String userUrl) {
        try {
            URI uri = new URI(userUrl);
            System.out.println(Ansi.ansi().fgCyan().bold().a("\n<验证并请求 URL>").reset());
            if (uri.getHost() == null || !uri.getHost().endsWith("marketplace.visualstudio.com")) {
                System.out.println(Ansi.ansi().fgRed().bold().a("<无效的 Vsix URL>").reset());
                return false;
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .setHeader("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200) {
                System.out.println(Ansi.ansi().fgGreen().bold().a("\n<URL 请求成功>").reset());
                return true;
            } else {
                System.out.println(Ansi.ansi().fgRed().bold().a("\n<请求失败 ( " + response.statusCode() + ")>").reset());
                return false;
            }

        } catch (Exception e) {
            System.out.println(Ansi.ansi().fgRed().bold().a("\n<验证失败，请查看错误详细信息>").reset());
        }
        return false;
    }

    private static String getExtensionVersion(Scanner scanner) {
        System.out.print(Ansi.ansi().fgBlue().bold().a("\nVsix 版本 → ").reset());
        String userVersion = scanner.nextLine();
        if (userVersion == null || userVersion.isEmpty()) {
            System.out.println(Ansi.ansi().fgRed().bold().a("\n<输入版本号>").reset());
            return null;
        }
        return userVersion;
    }

    private static void generateDownloadLink(String publisher, String extensionName, String userVersion) {
        System.out.println(Ansi.ansi().fgGreen().bold().a("\n<已构建下载链接>").reset());
        String downloadUrl = String.format(
                "https://marketplace.visualstudio.com/_apis/public/gallery/publishers/%s/vsextensions/%s/%s/vspackage",
                publisher, extensionName, userVersion);
        System.out.println(downloadUrl + "\n");
        System.out.println(Ansi.ansi().fgBlue().bold().a("下载 " + extensionName + " 吗? [是 Y][否 N]").reset());

        try (Scanner scanner = new Scanner(System.in)) {
            String choice = scanner.nextLine().toLowerCase();
            if (choice.equals("y")) {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.browse(new URI(downloadUrl));
                    System.out.println(Ansi.ansi().fgGreen().bold().a("\n<操作成功完成>").reset());
                } catch (Exception e) {
                    System.out.println(Ansi.ansi().fgRed().bold().a("\n<操作失败>").reset());
                }
            } else {
                System.exit(0);
            }
        }
    }
}
