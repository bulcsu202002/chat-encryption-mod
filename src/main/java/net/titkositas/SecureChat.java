package net.titkositas;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class SecureChat implements ClientModInitializer {
    private static String keyStr = "MzIxem9tYmllMTIzNDU=";
    private static boolean enabled = false;

    @Override
    public void onInitializeClient() {
        // Mod saját parancsai
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("enc-on").executes(c -> {
                enabled = true;
                msg("§6[✔] Titkosítás BEKAPCSOLVA!");
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("enc-off").executes(c -> {
                enabled = false;
                msg("§c[✘] Titkosítás KIKAPCSOLVA!");
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("enc-key")
                .then(ClientCommandManager.argument("jelszo", StringArgumentType.string())
                .executes(c -> {
                    updateKey(StringArgumentType.getString(c, "jelszo"));
                    return 1;
                })));
        });

        // Chat titkosítás
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!enabled || message.contains("[AES]")) return true;
            try {
                String encrypted = encrypt(message);
                final String finalMsg = "[AES] " + encrypted;
                // Biztonságos küldés soron kívül
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().getNetworkHandler() != null)
                        MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(finalMsg);
                });
                return false; 
            } catch (Exception e) { return true; }
        });

        // Privát üzenet (/msg, /tell, /w) titkosítás
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (!enabled || command.contains("[AES]")) return true;
            
            String[] parts = command.split("\\s+", 3);
            if (parts.length < 3) return true;

            String cmd = parts[0].toLowerCase();
            if (cmd.equals("msg") || cmd.equals("tell") || cmd.equals("w")) {
                try {
                    String target = parts[1];
                    String message = parts[2];
                    String encrypted = encrypt(message);
                    final String finalCmd = cmd + " " + target + " [AES] " + encrypted;

                    // Itt a trükk: a fő szálon, soron kívül küldjük el a parancsot
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().getNetworkHandler() != null)
                            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand(finalCmd);
                    });
                    return false; // Az eredeti (nyers) parancsot elvetjük
                } catch (Exception e) { return true; }
            }
            return true;
        });

        // Bejövő üzenetek dekódolása
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            processIncoming(message.getString());
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            processIncoming(message.getString());
        });
    }

    private void processIncoming(String txt) {
        if (txt != null && txt.contains("[AES]")) {
            try {
                String cleanTxt = txt.replaceAll("§[0-9a-fk-orx]", "");
                int index = cleanTxt.indexOf("[AES]");
                String encodedPart = cleanTxt.substring(index + 5).trim().split(" ")[0];
                
                String decoded = decrypt(encodedPart);
                msg("§b[TITKOS ÜZENET]: §f" + decoded);
            } catch (Exception ignored) {}
        }
    }

    private void updateKey(String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(password.getBytes(StandardCharsets.UTF_8));
            keyStr = Base64.getEncoder().encodeToString(Arrays.copyOf(key, 16));
            msg("§6[✔] Kulcs frissítve!");
        } catch (Exception e) { msg("§cHiba a kulcs generálásakor."); }
    }

    private String encrypt(String s) throws Exception {
        SecretKeySpec sk = new SecretKeySpec(Base64.getDecoder().decode(keyStr), "AES");
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, sk);
        return Base64.getEncoder().encodeToString(c.doFinal(s.getBytes(StandardCharsets.UTF_8)));
    }

    private String decrypt(String s) throws Exception {
        SecretKeySpec sk = new SecretKeySpec(Base64.getDecoder().decode(keyStr), "AES");
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, sk);
        return new String(c.doFinal(Base64.getDecoder().decode(s)));
    }

    private void msg(String t) {
        if (MinecraftClient.getInstance().player != null)
            MinecraftClient.getInstance().player.sendMessage(Text.literal(t), false);
    }
}