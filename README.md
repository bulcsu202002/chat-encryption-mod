# 🛡️ SecureChat - AES-128 Chat Encryption

**SecureChat** is a lightweight, client-side Fabric mod designed for Minecraft 1.21.x (tested up to 1.21.11). It allows players to communicate securely using high-level **AES-128 encryption**. 

By using a shared secret key, only users with the same password can read the messages. To anyone else (including server admins and console logs), the messages appear as unreadable encrypted strings.

## ✨ Features
- **AES-128 Symmetric Encryption:** Secure your conversations with industry-standard encryption.
- **Full Privacy:** The mod intercepts and blocks original messages before they reach the server. Only the encrypted version is sent.
- **Private Message Support:** Works with `/msg`, `/tell`, and `/w` commands.
- **Client-Side Only:** No server-side installation required. Works on any server as long as both parties have the mod.
- **On-the-fly Key Change:** Update your encryption password at any time without restarting the game.

## 🚀 Installation
1. Ensure you have the **Fabric Loader** installed for Minecraft 1.21.x.
2. Download the latest `SecureChat.jar` from the Releases page.
3. Place the `.jar` file into your `.minecraft/mods` folder.

## 🛠️ How to Use
Both you and the person you want to talk to must have the mod installed and use the **exact same password**.

### 1. Set your Password
First, define your secret key. Only players with this key can decrypt your messages.
`/enc-key <your_password>`

### 2. Enable Encryption
By default, the mod is disabled. Turn it on to start encrypting outgoing messages.
`/enc-on`

### 3. Send Messages
Simply type in chat or use private commands.
- `Hello everyone!` -> Sent as `[AES] Gx7zR9...`
- `/msg PlayerName Secret info` -> Sent as `/msg PlayerName [AES] u8B2p...`

### 4. Disable Encryption
If you want to go back to normal, unencrypted chat:
`/enc-off`

## 🔒 Security Note
This mod uses **SHA-256** to derive a 128-bit key from your password and **AES/ECB/PKCS5Padding** for message encryption. Never use your real-life sensitive passwords (bank, email, etc.) as an encryption key.
