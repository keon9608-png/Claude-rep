# Big Eyes 👀 — 갤럭시용 홈스크린 위젯

Nothing Playground의 **Big Eyes** 위젯을 삼성 갤럭시(및 모든 안드로이드 폰)에서
쓸 수 있도록 만든 네이티브 안드로이드 앱입니다.

- **홈스크린 위젯**: 검은 라운드 사각형 + 하얀 두 눈. 홈 화면에 올려두면 됩니다.
- **탭하면 전체 화면**: 눈동자가 **손가락을 따라오고**, 폰을 **기울이면 그 방향을 보고**,
  가끔 **깜빡**입니다. (60fps 부드러운 애니메이션)
- `minSdk 26` (Android 8.0+) — 사실상 현역 갤럭시 전 기종 지원.

> ⚠️ 참고: Nothing Playground의 원본 위젯 파일은 Nothing OS 전용이라 갤럭시에
> 그대로 설치할 수 없습니다. 이 프로젝트는 같은 모양·동작을 **직접 구현한 클론**이며,
> Nothing의 로고나 브랜딩은 사용하지 않았습니다.

---

## APK 받기 (Android Studio 없이)

이 저장소에는 **GitHub Actions 빌드 워크플로**가 들어 있어서, 코드를 푸시하면
클라우드에서 자동으로 APK를 빌드합니다.

1. GitHub 저장소 → **Actions** 탭 → **Build Big Eyes APK** 워크플로 실행 클릭
2. 초록색 체크가 뜨면 실행 상세 페이지 하단 **Artifacts** → `big-eyes-debug-apk` 다운로드
3. 압축을 풀면 `app-debug.apk`가 나옵니다 → 이 파일을 갤럭시로 보내세요
   (카톡 나에게 보내기 / 구글 드라이브 / USB 등)

## 갤럭시에 설치하기

1. `app-debug.apk`를 갤럭시에서 엽니다.
2. "출처를 알 수 없는 앱 설치" 권한을 요청하면 → 해당 앱(파일 관리자/브라우저)에 대해 **허용**.
3. 설치 완료 후:
   - **위젯 추가**: 홈 화면 빈 곳 길게 누르기 → **위젯** → **Big Eyes** 를 찾아 홈에 배치.
   - 위젯을 **탭**하면 전체 화면 Big Eyes가 열립니다.

---

## 직접 빌드하기 (Android Studio)

1. Android Studio로 이 폴더를 엽니다.
2. `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.
3. 또는 터미널에서:
   ```bash
   ./gradlew assembleDebug
   # 결과물: app/build/outputs/apk/debug/app-debug.apk
   ```

## 구조

| 파일 | 역할 |
|------|------|
| `EyesRenderer.kt` | 눈을 그리는 공용 Canvas 렌더러 (위젯·전체화면 공유) |
| `BigEyesWidgetProvider.kt` | 홈스크린 위젯 (비트맵 렌더 + 탭 시 액티비티 실행) |
| `BigEyesView.kt` | 전체 화면용 커스텀 View — 손가락/기울기 추적 + 깜빡임 |
| `BigEyesActivity.kt` | 위젯 탭 시 열리는 전체 화면 액티비티 |

### 위젯 애니메이션에 대하여
안드로이드 홈스크린 위젯은 배터리 보호를 위해 실시간 60fps 애니메이션이 불가능합니다
(런처가 가끔씩만 화면 갱신을 허용). 그래서 **위젯**은 갱신될 때마다 눈이 살짝 다른 곳을
보도록 하고, **부드러운 실시간 애니메이션(손가락 추적·깜빡임)은 탭했을 때 열리는
전체 화면**에서 보여줍니다.
