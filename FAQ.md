# Music-Player-GO - Frequently Asked Questions (FAQ)

For a list of **supported media formats** and related issues, please refer to [supported formats page](https://github.com/enricocid/Music-Player-GO/blob/master/FORMATS.md).

This FAQ will be continually updated to accommodate new information with respect to new updates and changes on the app, and will be used to help users find answers to their questions.


### What is Music Player GO?

A lightweight, original, privacy-friendly, **ad-free** player to play music from your local library, in a colorful and simple UI.


### Where can I download Music Player GO?

- [F-Droid](https://f-droid.org/packages/com.iven.musicplayergo/).
- [Google Play Store](https://play.google.com/store/apps/details?id=com.iven.musicplayergo).

### How can I contribute to translations?

Join us on [Hosted Weblate](https://hosted.weblate.org/engage/music-player-go/).

PS: pull requests are welcome too :)


### Can I customise the app's theme to my taste?

1. Yes, 3 themes are available:

 - Light
 - Dark/Pure Black
 - Automatic: still follows system settings in Android Q and battery levels in pre-Q as suggested in the [guidelines](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme).

2. 19 accents.

Note: MPGO will strictly follow [material design color guidelines](https://material.io/design/color/the-color-system.html). No requests to modify the color system will be taken into account.

As per [guidelines](https://material.io/design/color/dark-theme.html) **desaturated primary colors** are to be used in night themes as a more **accessible**/legible alternative to saturared colors used in light theme. In fact, the latter visually vibrate against dark surfaces, which can induce eye strain.


### Does Music Player GO have or use an equalizer?

Yes!
The equalizer can be found in the "Now Playing" dialog.

Note however that by default MPGO tries to open the system equalizer (**if it exists**)!
You can force the use of the built-in equalizer from the app settings! 

Note: in case of built-in equalizer faults, MPGO tries to fall back to the the system equalizer (**if it exists**) found on the device.

PS: don't blame the devs if equalizer is not working properly, blame the device manufacturer instead!

:)


### What is audio focus?

It is an Android feature to moderate playback in media apps that prevents multiple media apps from playing at the same time.
Every media app should deal with audio focus. This option is ON by default so that MPGO can:
1. **Pause** if another app requests audio focus.
2. **Lower** its volume when a new message is received.


### Does Music Player GO play music tracks from online sources?

No, local library only.

### Does Music Player GO access the Internet?

No. Internet permission is not required.


### How does Music Player GO categorized music tracks?

By artists, folders and albums.
Artists' music are in turn categorized by albums.
Folders can be considered albums.
Each album consists of tracks of the same album.


### Can I exclude folders from my library?

Filter them once the app has loaded.


### How does MPGO handle shuffled lists?

To avoid performance issues queue built from shuffled lists is limited to 250 items.


### Sometimes MediaButton does not work

Make sure headphones are properly connected.


### Android 12 and battery optimization: media player stops working

"Apps that target Android 12 (API level 31) or higher can't start foreground services while running in the background, except for a few special cases. If an app tries to start a foreground service while the app is running in the background, and the foreground service doesn't satisfy one of the exceptional cases, the system throws a [ForegroundServiceStartNotAllowedException](https://developer.android.com/guide/components/foreground-services#background-start-restrictions)."

As mentioned in the [Android documentation](https://developer.android.com/guide/components/foreground-services#background-start-restriction-exemptions) please consider disabling battery optimization for MPGO in system settings to solve the issue:
Apps > All apps > Music Player GO > Battery > Unrestricted

### What about features requests/crash reports?

1. Unfortunately, not all requests can be taken into consideration for a variety of reasons:

   - Job/real life.
   - Complex feature clashing with the KISS philosophy behind the app: Keep it snimple snupid :)
   - The rather poor developer is rocking a ~~Xiaomi Mi A1~~ Xiaomi Mi A2 lite as the only test device. A good computer could ensure emulator usage.
   - Keep in mind that the app is built around the **developer's usage needs** and is shared for the love of libre software.
   - Sometimes time is just short…
   - Cyclic lack of motivation/interest in Android development.


2. A crash report is useful only when a logcat and/or a detailed explanation on how to reproduce the bug are attached. If you don't know how to proceed, contact the developer to receive guidance.


### Does this app keep any info or data about me or my device?

No. Neither the device's nor owner info.

Pose other questions by creating issues, and we will answer them.
