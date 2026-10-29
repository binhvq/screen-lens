# Screen Lens (Lens màn hình)

**Tiếng Việt** | [English](#english)

Khôi phục một phần tính năng **Circle to Search** (khoanh vùng để tìm bằng Google Lens) trên máy **ColorOS ROM Trung Quốc (nội địa)**, nơi tính năng này bị chặn hẳn ở cấp ROM.

## Ứng dụng này làm gì

Trên ROM nội địa của Oppo, nhấn giữ nút Home/thanh cử chỉ hoặc nút nguồn chỉ gọi trợ lý Breeno, không gọi được Gemini hay Circle to Search. Tôi đã kiểm tra trên Oppo Pad Mini (OPD2515, ColorOS 17 CN) và thấy:

- Cờ `google_assistant_supported` của SystemUI bị đặt `false` trong phân vùng chỉ đọc, và cử chỉ nhấn giữ được gắn cứng vào Breeno.
- Máy không có dịch vụ hệ thống `contextual_search`, nên Circle to Search không có backend nào để chạy.
- Các cách không cần root (đổi vai trò trợ lý, phím `KEYCODE_ASSIST`, intent `VOICE_COMMAND`, các khoá `settings` đã lưu hành) đều không có tác dụng trên bản này.

Screen Lens là cách làm gần nhất mà **không cần root**:

1. Bạn mở app (ví dụ từ thanh bên thông minh của ColorOS).
2. App chụp màn hình hiện tại bằng dịch vụ trợ năng.
3. Bạn **khoanh một nét tự do** quanh thứ cần tìm. Chạm nhanh thì tìm cả màn hình.
4. App cắt theo hộp bao quanh nét vẽ rồi gửi ảnh vào **Google Lens**.

Nó không giống hệt Circle to Search của Google: Lens mở toàn màn hình thay vì một khung kết quả nổi, và nét khoanh chỉ dùng để chọn vùng cắt. Cử chỉ nhấn giữ Home/nguồn **không** được khôi phục. Muốn khôi phục đầy đủ thì phải có root, ví dụ module LSPosed [ColorOS-Assistant-Restore](https://github.com/Andrea-lyz/ColorOS-Assistant-Restore) (do dự án đó báo là chạy trên ColorOS 17 CN; tôi chưa tự kiểm chứng).

### Quyền riêng tư

App không khai báo quyền nào, kể cả INTERNET. Ảnh chụp lưu trong bộ nhớ đệm riêng của app và chỉ được trao quyền đọc cho Google app khi gửi vào Lens. Ảnh cũ bị xoá ở lần chụp kế tiếp. Dịch vụ trợ năng chỉ dùng quyền chụp màn hình (`canTakeScreenshot`), không đọc nội dung hay sự kiện trên màn hình.

## Cài đặt

Đã thử trên: **Oppo Pad Mini OPD2515, ColorOS 17.0.0.104 (CN01), Android 17**. Yêu cầu Android 11 trở lên (`minSdk 30`). Các máy khác chưa được kiểm tra.

1. Tải `screen-lens-1.0-debug.apk` ở [Releases](../../releases) (hoặc thư mục [`apk/`](apk)) rồi cài. ColorOS sẽ hỏi xác nhận cài từ nguồn không xác định. APK này ký bằng khoá debug.
2. Vào **Cài đặt > Hỗ trợ tiếp cận > Lens màn hình** và bật dịch vụ.
3. Cài sẵn **ứng dụng Google** (có Google Lens) và đảm bảo dịch vụ Google hoạt động. App gửi ảnh tới `com.google.android.googlequicksearchbox` và thử `com.google.ar.lens` nếu không có.
4. Thêm biểu tượng "Lens màn hình" vào thanh bên thông minh hoặc màn hình chính, rồi chạm để dùng.

Chọn ngôn ngữ (Theo hệ thống / Tiếng Việt / English): chạm bánh răng ở góc màn hình khoanh vùng.

## Xử lý sự cố

**Chạm vào app lại ra cài đặt trợ năng.** ColorOS đã force-stop app và Android gỡ dịch vụ khỏi danh sách đã bật. Bật lại bằng tay, hoặc bằng adb:

```bash
adb shell settings put secure enabled_accessibility_services \
  dev.binh.lensbubble/dev.binh.lensbubble.LensBubbleService
adb shell settings put secure accessibility_enabled 1
```

Nếu ColorOS bỏ qua lần ghi đầu, hãy chạy lại lần hai. Vào Cài đặt > Ứng dụng > Lens màn hình và cho phép hoạt động nền/tự khởi chạy, đồng thời khoá app trong màn hình đa nhiệm, để giảm khả năng bị dừng.

**Lens mở nhưng báo "không có mạng".** Trên máy thử, ứng dụng Pin của ColorOS (`com.oplus.battery`) đặt chính sách `REJECT_ALL` cho Dịch vụ Google Play sau khi bước thăm dò Google lúc khởi động thất bại. Kiểm tra bằng:

```bash
adb shell service call networking_control 2 i32 <uid của com.google.android.gms>
```

Giá trị `00000004` nghĩa là đang bị chặn. Tắt rồi bật Wi-Fi sẽ làm Pin thăm dò lại và gỡ chặn. Có VPN đang chạy thì bước thăm dò được bỏ qua. Đây là quan sát trên một máy, chưa kiểm tra trên máy khác.

## Build từ mã nguồn

Cần JDK 17 trở lên và Android SDK (platform 36).

```bash
echo "sdk.dir=/đường/dẫn/Android/Sdk" > local.properties
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Mã nguồn Java thuần, không phụ thuộc thư viện ngoài.

## Giấy phép

[MIT](LICENSE). Đây là dự án không chính thức, không liên kết với Google hay Oppo. "Google", "Lens" và "Circle to Search" là nhãn hiệu của chủ sở hữu tương ứng.

---

## English

Restores a partial **Circle to Search** (circle an area to search with Google Lens) on **ColorOS China-ROM** devices, where the feature is blocked at the ROM level.

### What it does

On Oppo's China ROM, long-pressing Home/the gesture bar or the power button only starts the Breeno assistant, never Gemini or Circle to Search. On an Oppo Pad Mini (OPD2515, ColorOS 17 CN) I found:

- SystemUI's `google_assistant_supported` flag is `false` on a read-only partition, and the long-press gesture is hard-wired to Breeno.
- There is no `contextual_search` system service, so Circle to Search has no backend.
- The non-root options (assistant role, `KEYCODE_ASSIST`, the `VOICE_COMMAND` intent, circulated `settings` keys) had no effect on this build.

Screen Lens is the closest thing that works **without root**:

1. Launch the app (for example from the ColorOS smart sidebar).
2. It captures the current screen through an accessibility service.
3. You **draw a freehand stroke** around what you want to search. A quick tap searches the whole screen.
4. It crops to the stroke's bounding box and hands the image to **Google Lens**.

It is not identical to Google's Circle to Search: Lens opens full screen instead of a floating result sheet, and the stroke only chooses the crop. The Home/power long-press gesture is **not** restored. A full restore needs root, for example the LSPosed module [ColorOS-Assistant-Restore](https://github.com/Andrea-lyz/ColorOS-Assistant-Restore) (reported by that project to work on ColorOS 17 CN; I have not verified it myself).

### Privacy

The app declares no permissions, including INTERNET. Screenshots live in the app's private cache and are only readable by the Google app while being sent to Lens. Old screenshots are deleted on the next capture. The accessibility service only uses the screenshot capability (`canTakeScreenshot`); it does not read screen content or events.

### Install

Tested on: **Oppo Pad Mini OPD2515, ColorOS 17.0.0.104 (CN01), Android 17**. Requires Android 11+ (`minSdk 30`). Other devices are untested.

1. Download `screen-lens-1.0-debug.apk` from [Releases](../../releases) (or the [`apk/`](apk) folder) and install it. ColorOS asks you to confirm an install from an unknown source. The APK is signed with the debug key.
2. Go to **Settings > Accessibility > Screen Lens** and turn the service on.
3. Have the **Google app** (with Google Lens) installed and Google services working. The app sends the image to `com.google.android.googlequicksearchbox` and falls back to `com.google.ar.lens`.
4. Add the "Screen Lens" icon to the smart sidebar or home screen and tap it to use.

Language (Follow system / Tiếng Việt / English): tap the gear in the corner of the selection screen.

### Troubleshooting

**Tapping the app opens Accessibility settings.** ColorOS force-stopped the app and Android dropped the service from the enabled list. Turn it on again by hand, or with adb:

```bash
adb shell settings put secure enabled_accessibility_services \
  dev.binh.lensbubble/dev.binh.lensbubble.LensBubbleService
adb shell settings put secure accessibility_enabled 1
```

If ColorOS ignores the first write, run it a second time. In Settings > Apps > Screen Lens allow background activity/auto-start and lock the app in the recents screen to make it less likely to be stopped.

**Lens opens but says "no network".** On the test device, ColorOS's Battery app (`com.oplus.battery`) set a `REJECT_ALL` policy on Google Play services after its boot-time Google probe failed. Check with:

```bash
adb shell service call networking_control 2 i32 <uid of com.google.android.gms>
```

A value of `00000004` means it is blocked. Toggling Wi-Fi off and on makes Battery probe again and lifts the block. A running VPN makes the probe skip. This is an observation from one device and has not been checked on others.

### Build from source

Needs JDK 17+ and the Android SDK (platform 36).

```bash
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Plain Java, no third-party dependencies.

### License

[MIT](LICENSE). This is an unofficial project, not affiliated with Google or Oppo. "Google", "Lens" and "Circle to Search" are trademarks of their respective owners.
