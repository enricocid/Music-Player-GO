---
name: Bug report
about: Create a report to help us improve
title: "[BUG] A clear and concise title describing the bug"
labels: bug
assignees: enricocid

---

1) **Describe the bug**
A clear and concise description of what the bug is.

2) **To Reproduce the behaviour**
Describe the steps to reproduce the issue:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

3) **Expected behavior**
A clear and concise description of what you expected to happen.

4) **Provided log**
Note: no log no party. All issues not providing a log will be deleted.

- Download [platform-tools](https://developer.android.com/studio/releases/platform-tools) for Your OS 
- Enable [developer options](https://developer.android.com/studio/debug/dev-options) and adb debug
- Open a terminal inside platform-tools folder
- Type the following and start recording

`adb logcat -v long > name_of_problem.txt`

- Reproduce the issue/crash. Stop the recording using ctrl + c

**Alternatively** Capture and share a [bug report](https://developer.android.com/studio/debug/bug-report)

5) **Smartphone (please complete the following information):**
 - Device: [e.g. Xiaomi Mi A2 lite]
 - OS: [e.g. Android 9.0]
 - ROM or mods: [please specify any ROM or mods applied that could affect the normal functioning of the app]

6) **Screenshots**
If applicable, add screenshots to help explain your problem.


**Additional context**
Add any other context about the problem here.
