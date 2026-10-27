# Phone Guard signing identity

## INTERNAL / upload lane

Application ID: `com.riccardopinato.phoneguard`

Expected INTERNAL certificate SHA-256:

`A7:82:F8:57:EE:88:05:D3:D7:BA:B8:46:E1:0F:C5:4E:56:4C:EB:FC:8B:AD:14:DF:67:8E:A5:A2:58:CF:5E:C6`

The private PKCS#12 keystore and passwords are never stored in this repository. The GitHub INTERNAL workflow reconstructs the keystore only from repository secrets and verifies the APK and AAB certificate before uploading artifacts.

## Google Play decision

For the first Play enrollment, use **Play App Signing with a Google-generated app-signing key unique to Phone Guard**. The certificate above may remain the controlled upload/internal identity; it is not the intended Play distribution app-signing identity.

This keeps Phone Guard cryptographically independent as an installed Play product even if the same developer upload credential is reused elsewhere.

Official guidance:
- https://support.google.com/googleplay/android-developer/answer/9842756
- https://developer.android.com/guide/app-bundle/faq
