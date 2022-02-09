package cn.enaium.humblemc.installer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Enaium
 */
public class MainGUI extends JFrame {

    public MainGUI() throws HeadlessException {
        super("Humble Installer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(getOwner());
        setSize(500, 200);
        setLayout(new FlowLayout());
        JPanel gameVersionPanel = new JPanel();
        gameVersionPanel.add(new JLabel("Game version:"));
        JComboBox<String> gameVersionCombo = new JComboBox<>();
        new Thread(() -> {
            JsonArray versions = new Gson().fromJson(readString("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"), JsonObject.class).get("versions").getAsJsonArray();
            for (JsonElement jsonElement : versions) {
                gameVersionCombo.addItem(jsonElement.getAsJsonObject().get("id").getAsString());
            }
        }).start();
        gameVersionPanel.add(gameVersionCombo);
        add(gameVersionPanel, BorderLayout.NORTH);
        JPanel loaderVersionPanel = new JPanel();
        loaderVersionPanel.add(new JLabel("Loader version:"));
        JComboBox<String> loaderVersionCombo = new JComboBox<>();
        new Thread(() -> {
            String xml = readString("https://maven.enaium.cn/cn/enaium/humblemc/HumbleLoader/maven-metadata.xml");
            String allLoaderVersion = xml.substring(xml.indexOf("<versions>") + "<versions>".length(), xml.lastIndexOf("</versions>")).replaceAll("\\s", "");
            String[] split = allLoaderVersion.split("(<version>|</version>)");
            for (int i = split.length - 1; i >= 0; i--) {
                if (!split[i].equals("")) {
                    loaderVersionCombo.addItem(split[i]);
                }
            }
        }).start();
        loaderVersionPanel.add(loaderVersionCombo);
        add(loaderVersionPanel);
        JPanel path = new JPanel();
        path.add(new JLabel("Path"));
        path.add(new JTextField(getMinecraftDir().getAbsolutePath()));
        add(path, BorderLayout.CENTER);
        JButton install = new JButton("Install");
        install.addActionListener(e -> {
            Object gameVersion = gameVersionCombo.getSelectedItem();
            Object loaderVersion = loaderVersionCombo.getSelectedItem();
            if (gameVersion == null || loaderVersion == null) {
                JOptionPane.showMessageDialog(null, "please wait");
            } else {
                try {

                    String name = String.format("Humble-%s-%s", gameVersion, loaderVersion);
                    File dir = new File(getMinecraftDir(), "versions" + "/" + name);
                    if (!dir.exists()) {
                        Files.createDirectories(dir.toPath());
                    }

                    String string = getString(this.getClass().getResourceAsStream("/config.json"));
                    string = string.replace("${gameVersion}", gameVersion.toString());
                    string = string.replace("${loaderVersion}", loaderVersion.toString());
                    string = string.replace("${formattedTime}", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
                    Files.write(new File(dir, name + ".json").toPath(), string.getBytes(StandardCharsets.UTF_8));
                    JOptionPane.showMessageDialog(null, "Success!");
                } catch (IOException exception) {
                    exception.printStackTrace();
                    JOptionPane.showMessageDialog(null, exception);
                }
            }
        });
        add(install);
    }

    private File getMinecraftDir() {
        File minecraftFolder;
        if (getOsName().contains("win")) {
            minecraftFolder = new File(System.getenv("APPDATA"), File.separator + ".minecraft");
        } else if (getOsName().contains("mac")) {
            minecraftFolder = new File(System.getProperty("user.home"), File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft");
        } else {
            minecraftFolder = new File(System.getProperty("user.home"), File.separator + ".minecraft");
        }
        return minecraftFolder;
    }

    private String getOsName() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT);
    }

    private String readString(String link) {
        try {
            URL url = new URL(link);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection connection = null;
            if (urlConnection instanceof HttpURLConnection) {
                connection = (HttpURLConnection) urlConnection;
            }

            if (connection == null) {
                throw new NullPointerException(String.format("Link: '%s' fail", link));
            }

            return getString(connection.getInputStream());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return null;
    }

    private String getString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder.toString();
    }
}
