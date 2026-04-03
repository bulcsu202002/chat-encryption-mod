package net.mcreator.chatencryption;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class ChatDecrypter {
    private static String encryptionKey = "MzIxem9tYmllMTIzNDU=";
    private static boolean isEnabled = false;
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("enc-on").executes(context -> {
                isEnabled = true;
                sendLocalMessage("§6[✔] Titkosítás BEKAPCSOLVA!");
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("enc-off").executes(context -> {
                isEnabled = false;
                sendLocalMessage("§c[✘] Titkosítás KIKAPCSOLVA!");
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("enc-key")
                .then(ClientCommandManager.argument("password", StringArgumentType.string())
                .executes(context -> {
                    setKey(StringArgumentType.getString(context, "password"));
                    return 1;
                })));
        });
        registerEvents();
        isInitialized = true;
    }

    private static void registerEvents() {
        // --- KÜLDÉS: Sima Chat ---
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (!isEnabled || message.contains("[AES]")) return true;
            try {
                String encrypted = aesEncrypt(message, encryptionKey);
                if (Minecraft.getInstance().getConnection() != null)
                    Minecraft.getInstance().getConnection().sendChat("[AES] " + encrypted);
            } catch (Exception e) {}
            return false;
        });

        // --- KÜLDÉS: Parancsok (/msg, /tell) ---
        ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
            if (!isEnabled || command.contains("[AES]")) return true;
            String lower = command.toLowerCase();
            if (lower.startsWith("msg ") || lower.startsWith("tell ") || lower.startsWith("w ")) {
                String[] parts = command.split(" ", 3);
                if (parts.length < 3) return true;
                try {
                    String encrypted = aesEncrypt(parts[2], encryptionKey);
                    if (Minecraft.getInstance().getConnection() != null)
                        Minecraft.getInstance().getConnection().sendCommand(parts[0] + " " + parts[1] + " [AES] " + encrypted);
                    return false;
                } catch (Exception e) {}
            }
            return true;
        });

        // --- FOGADÁS: Chat ÉS Rendszer üzenetek (Privát üzenethez) ---
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            processIncoming(message.getString());
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            processIncoming(message.getString());
        });
    }

    private static void processIncoming(String content) {
        // Megtisztítjuk a szöveget a Minecraft színkódoktól a kereséshez
        String cleanContent = content.replaceAll("§[0-9a-fk-orx]", "");
        
        if (cleanContent.contains("[AES]")) {
            try {
                // Megkeressük hol kezdődik a kód az [AES] után
                String encoded = cleanContent.substring(cleanContent.indexOf("[AES]") + 5).trim();
                // Csak az első szót vesszük (a Base64 kód nem tartalmaz szóközt)
                if (encoded.contains(" ")) encoded = encoded.split(" ")[0];
                
                String decoded = aesDecrypt(encoded, encryptionKey);
                sendLocalMessage("§b[PRIVÁT ÜZENET]: §f" + decoded);
            } catch (Exception e) {}
        }
    }

    public static void setKey(String password) {
        try {
            byte[] key = password.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            encryptionKey = Base64.getEncoder().encodeToString(Arrays.copyOf(key, 16));
            sendLocalMessage("§6[✔] Új jelszó aktív!");
        } catch (Exception e) {}
    }

    private static String aesEncrypt(String str, String sec) throws Exception {
        SecretKeySpec sk = new SecretKeySpec(Base64.getDecoder().decode(sec), "AES");
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, sk);
        return Base64.getEncoder().encodeToString(c.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }

    private static String aesDecrypt(String str, String sec) throws Exception {
        SecretKeySpec sk = new SecretKeySpec(Base64.getDecoder().decode(sec), "AES");
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, sk);
        return new String(c.doFinal(Base64.getDecoder().decode(str)));
    }

    private static void sendLocalMessage(String text) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(text), false);
        }
    }
}