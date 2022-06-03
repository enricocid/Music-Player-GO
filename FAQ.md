# Music-Player-GO - Frequently Asked Questions (FAQ)

For a list of **supported media formats** and related issues, please refer to [supported formats page](https://github.com/enricocid/Music-Player-GO/blob/master/FORMATS.md).

This FAQ will be continually updated to accommodate new information with respect to new updates and changes on the app, and will be used to help users find answers to their questions.


### What is Music Player GO?

A lightweight, original, privacy-friendly, **ad-free** music player playing music from your local music library, in acolorful and simple UI.


### Where can I download Music Player GO?

[Here](https://f-droid.org/packages/com.iven.musicplayergo/) on F-Droid.
[Here](https://play.google.com/store/apps/details?id=com.iven.musicplayergo) on the Google Play Store.

### How can I contribute to translations?

Join us on [Hosted Weblate](https://hosted.weblate.org/engage/music-player-go/). :)


### Can I customise the app's theme to my taste?

1. Yes, choose between 3 themes:

 - Light
 - Dark
 - Automatic: still follows system settings in Android Q and battery levels in pre-Q as suggested in the [guidelines](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme).

2. 36 accent colours.


### Why are accents 'lighter' in the dark theme?

As per [guidelines](https://material.io/design/color/dark-theme.html), primary colors are desaturated for accessibility: saturated colors visually vibrate against dark surfaces, which can induce eye strain.
Instead, desaturated colors can be used as a more legible alternative.


### Does Music Player GO have or use an equaliser?

Music Player GO uses the system (default) equalizer (if it exists) found on the device.
The equalizer can be found in the "Now Playing" dialog.
There is also a built-in equalizer. You can use by turning off the one in your system.

If the Android system is missing (as found in some Samsung, HTC, Pixel, etc. devices) the android.media.audiofx.Equalizer class then there is nothing for MPGO's EQ to link to.
All you can do is to use an equalizer that relies on an internal audio engine and interacts with Android media session, like [Wavelet](https://pittvandewitt.github.io/Wavelet/) does.


### The built-in equalizer is not being turned on

Just toggle the switch button again.


### What is audio focus?

It is an Android feature to moderate playback in media apps and prevents multiple media apps from playing at the same time.
Every media app should request and receive the audio focus. This option is on by default so that MPGO can:
1. **pause** if another app requests audio focus.
2. **lower** its volume when a new message is received.


### How to clear queued and favorited songs?

Long-click on the buttons from the bottom panel.
To clear the queue You can also start playback from "Artist" details.

### Does Music Player GO play music tracks from online sources?

No, it only plays from the local library.

### Does Music Player GO access the Internet?

No.
Internet permission is not requested.


### How does Music Player GO categorized music tracks?

By artists, folders, and albums.
Artists' music are in turn categorized by albums.
Folders can be considered albums.
Each album consists of tracks of the same album.


### Can I exclude folders from my library?

Filter them once the app has loaded.


### How does MPGO handle shuffled lists?

To avoid performance issues queue built from shuffled lists are limited to 250 items.


### Sometimes MediaButton does not work

Sometimes it seems to not register properly. Try to reconnect the headphones.


### Android 12 and battery optimization: media player stops working

"Apps that target Android 12 (API level 31) or higher can't start foreground services while running in the background, except for a few special cases. If an app tries to start a foreground service while the app is running in the background, and the foreground service doesn't satisfy one of the exceptional cases, the system throws a [ForegroundServiceStartNotAllowedException](https://developer.android.com/guide/components/foreground-services#background-start-restrictions)."

As mentioned in the [Android documentation](https://developer.android.com/guide/components/foreground-services#background-start-restriction-exemptions) please consider disabling battery optimization for MPGO in system settings to solve the issue:
Apps > All apps > Music Player GO > Battery > Unrestricted

### What about features requests/crash reports?

1. Unfortunately, not all requests can be taken into consideration, for a variety of reasons:

   - The feature is too complex, clashing with the philosophy behind the app: KISS. Keep it snimple snupid :)
   - The rather poor developer is rocking a dying Nexus 5 as the only test device for now. A good computer could ensure emulator usage. This is the case of [#51](https://github.com/enricocid/Music-Player-GO/issues/51).
   - The feature just does not match the developer's needs. Keep in mind that this app is built around the developer's usage needs and is shared for the love of libre software.
   - Sometimes time is just short…
   - Lack of motivation

2. A crash report is useful only when a logcat and detailed explanation on how to reproduce the bug is provided. If you don't know how to proceed, send an e-mail to the developer to receive guidance.


### Does this app keep any info or data about me or my device?

No. Neither the device's nor owner info.

Pose other questions by creating issues, and we will answer them.
