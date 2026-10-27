# Phone Guard signing identity

## INTERNAL / upload lane

Application ID: `com.riccardopinato.phoneguard`

Expected INTERNAL certificate SHA-256:

`0E:CA:F9:20:E1:14:03:7C:DC:09:0E:30:95:0A:13:04:39:A3:F2:E4:FF:E6:6F:48:6E:E4:17:39:F2:BB:A6:54`

The private keystore and passwords are never stored in this repository. The GitHub INTERNAL workflow reconstructs the keystore only from repository secrets and verifies the APK and AAB certificate before uploading artifacts.

## Google Play decision

For the first Play enrollment, use **Play App Signing with a Google-generated app-signing key unique to Phone Guard**. The certificate above may remain the controlled upload/internal identity; it is not the intended Play distribution app-signing identity.

This keeps Phone Guard cryptographically independent as an installed Play product even if the same developer upload credential is reused elsewhere.

Official guidance:
- https://support.google.com/googleplay/android-developer/answer/9842756
- https://developer.android.com/guide/app-bundle/faq
